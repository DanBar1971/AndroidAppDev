package com.mediagenda.app.sync

import android.content.Context
import android.util.AtomicFile
import com.mediagenda.app.data.ProfileStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class NearbyMergeResult(val newRecords:Int,val newFiles:Int,val conflicts:Int)

/** Non-destructive merging of two independently maintained HealthPratica devices.
 * Received data is staged and verified before it is allowed to enter the live vault.
 */
class NearbySyncStore(private val context:Context) {
    private val root=context.filesDir
    private val profiles=ProfileStore(context)
    private val own=context.getSharedPreferences("saudepratica_nearby_v1",Context.MODE_PRIVATE)
    private val idPattern=Regex("(legacy|[0-9a-fA-F-]{36})")
    private val namePattern=Regex("[A-Za-z0-9_.-]{1,150}")
    private val folders=setOf("saudepratica_profiles","saudepratica_health",
        "saudepratica_documents","saudepratica_labs","saudepratica_workplace",
        "saudepratica_identity","medication_photos")
    private val indexes=setOf("medicacao.json","agenda.json","records.json",
        "index.json","lab_reports.json")
    private val maxBytes=300L*1024*1024
    private val maxFiles=3000
    private var added=0
    private var copies=0
    private var conflicts=0

    fun deviceId():String {
        val previous=own.getString("device_id","").orEmpty()
        if(previous.matches(Regex("[0-9a-fA-F-]{36}")))return previous
        val next=UUID.randomUUID().toString()
        check(own.edit().putString("device_id",next).commit())
        return next
    }
    private fun valid(path:String):Boolean{
        if(path.length>300||path.startsWith("/")||path.contains("..")||
            path.contains("\\")||path.contains(":"))return false
        if(path=="medicacao.json"||path=="agenda.json")return true
        val bits=path.split("/")
        if(bits.size==2&&bits[0]=="medication_photos")
            return namePattern.matches(bits[1])&&bits[1].endsWith(".jpg",true)
        if(bits.size!=3||bits[0] !in folders||!idPattern.matches(bits[1])||
            !namePattern.matches(bits[2]))return false
        if(bits[0]=="saudepratica_profiles")
            return bits[2]=="medicacao.json"||bits[2]=="agenda.json"
        return true
    }
    private fun files():List<Pair<String,File>>{
        val all=mutableListOf<Pair<String,File>>()
        fun scan(f:File,relative:String){
            if(!valid(relative))return
            if(f.isFile&&f.length()<=65L*1024*1024&&
                f.canonicalFile.parentFile==f.parentFile.canonicalFile)
                all.add(relative to f)
        }
        scan(File(root,"medicacao.json"),"medicacao.json")
        scan(File(root,"agenda.json"),"agenda.json")
        val people=profiles.all()
        for(person in people){
            val id=person.id
            if(id!="legacy")for(part in listOf("medicacao.json","agenda.json"))
                scan(File(root,"saudepratica_profiles/$id/$part"),
                    "saudepratica_profiles/$id/$part")
            for(folder in folders-setOf("saudepratica_profiles","medication_photos")){
                File(root,folder+"/"+id).listFiles()?.forEach {
                    scan(it,folder+"/"+id+"/"+it.name)
                }
            }
        }
        File(root,"medication_photos").listFiles()?.forEach{
            scan(it,"medication_photos/"+it.name)
        }
        return all
    }
    private fun sha(file:File):String{
        val d=MessageDigest.getInstance("SHA-256")
        file.inputStream().use{input->
            val buf=ByteArray(32768)
            while(true){val n=input.read(buf);if(n<0)break;d.update(buf,0,n)}
        }
        return d.digest().joinToString(""){b->"%02x".format(b)}
    }
    private fun publicProfilePrefs(id:String):JSONObject {
        val preferences=context.getSharedPreferences(
            profiles.preferencesFor(id),Context.MODE_PRIVATE)
        val result=JSONObject()
        for((key,v) in preferences.all){
            if(!key.matches(Regex("[a-zA-Z0-9_-]{1,90}")) ||
               Regex("pin|pass|secret|token|auth|key",
                   RegexOption.IGNORE_CASE).containsMatchIn(key))continue
            val item=when(v){
                is String -> JSONObject().put("t","s").put("v",v.take(5000))
                is Boolean -> JSONObject().put("t","b").put("v",v)
                is Int -> JSONObject().put("t","i").put("v",v)
                is Long -> JSONObject().put("t","l").put("v",v)
                is Float -> JSONObject().put("t","f").put("v",v.toDouble())
                else -> null
            }
            if(item!=null)result.put(key,item)
        }
        return result
    }
    fun export():File{
        val documents=files()
        require(documents.size<=maxFiles && documents.sumOf{it.second.length()}<=maxBytes){
            "Excesso de documentos para sincronizar (300 MB)."
        }
        val people=JSONArray()
        for(p in profiles.all())people.put(JSONObject()
            .put("id",p.id).put("name",p.name)
            .put("prefs",publicProfilePrefs(p.id)))
        require(people.length()>0){"Não há perfis para sincronizar."}
        val manifest=JSONObject().put("format","saudepratica-nearby-v1")
            .put("device",deviceId()).put("profiles",people).put("files",JSONArray())
        val cache=File(context.cacheDir,"nearby_sync").apply{mkdirs()}
        val destination=File(cache,UUID.randomUUID().toString()+".zip")
        try{
            ZipOutputStream(destination.outputStream().buffered()).use{zip->
                for((path,file) in documents){
                    zip.putNextEntry(ZipEntry("data/"+path))
                    file.inputStream().use{it.copyTo(zip)}
                    zip.closeEntry()
                    manifest.getJSONArray("files").put(JSONObject()
                        .put("path",path).put("sha",sha(file))
                        .put("size",file.length()))
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            return destination
        }catch(error:Exception){destination.delete();throw error}
    }

    fun merge(archive:File):NearbyMergeResult{
        require(archive.isFile && archive.length()<=maxBytes+12_000_000L)
        val temp=File(context.cacheDir,"nearby_stage_"+UUID.randomUUID())
        check(temp.mkdirs())
        added=0;copies=0;conflicts=0
        try{
            val paths=linkedMapOf<String,File>()
            var manifestFile:File?=null
            var total=0L
            ZipInputStream(archive.inputStream().buffered()).use{zip->
                while(true){
                    val entry=zip.nextEntry?:break
                    require(!entry.isDirectory && paths.size<=maxFiles)
                    val relative=entry.name
                    val out=if(relative=="manifest.json"){
                        check(manifestFile==null){"Manifesto duplicado."}
                        File(temp,"manifest.json").also{manifestFile=it}
                    }else{
                        require(relative.startsWith("data/"))
                        val path=relative.removePrefix("data/")
                        require(valid(path) && !paths.containsKey(path))
                        File(temp,path).also{
                            require(it.canonicalPath.startsWith(temp.canonicalPath+"/"))
                            check(it.parentFile!!.isDirectory || it.parentFile!!.mkdirs())
                            paths[path]=it
                        }
                    }
                    var count=0L
                    out.outputStream().use{output->
                        val b=ByteArray(32768)
                        while(true){
                            val n=zip.read(b);if(n<0)break
                            count+=n;total+=n
                            require(count<=65L*1024*1024 && total<=maxBytes)
                            output.write(b,0,n)
                        }
                    }
                    zip.closeEntry()
                }
            }
            val manifest=JSONObject((manifestFile?:error("Manifesto ausente"))
                .readText(Charsets.UTF_8))
            require(manifest.optString("format")=="saudepratica-nearby-v1" &&
                manifest.optString("device")!=deviceId())
            val arr=manifest.getJSONArray("files")
            require(arr.length()==paths.size)
            val verified=mutableSetOf<String>()
            for(i in 0 until arr.length()){
                val d=arr.getJSONObject(i)
                val path=d.getString("path")
                require(verified.add(path))
                val file=paths[path]?:error("Documento em falta.")
                require(file.length()==d.getLong("size")&&sha(file)==d.getString("sha")){
                    "Transferência com erros."
                }
            }
            // Map by explicitly displayed profile name, NEVER by local UUID:
            // the legacy profile on two phones may represent different people.
            val alias=mutableMapOf<String,String>()
            val people=manifest.getJSONArray("profiles")
            require(people.length() in 1..20)
            for(i in 0 until people.length()){
                val item=people.getJSONObject(i)
                val id=item.getString("id")
                val name=item.getString("name").trim()
                require(idPattern.matches(id)&&name.length in 1..100 && !alias.containsKey(id))
                val local=profiles.all().firstOrNull{it.name.equals(name,true)}
                    ?:profiles.create(name)
                alias[id]=local.id
                applyPrefs(local.id,item.optJSONObject("prefs")?:JSONObject())
            }
            require(paths.keys.all { name ->
                if(name.startsWith("medication_photos/"))true
                else if(name=="medicacao.json"||name=="agenda.json")alias.containsKey("legacy")
                else alias.containsKey(name.split("/")[1])
            })
            // Never overwrite files. Merge indexed records after attachments.
            val ordered=paths.entries.sortedBy{ if(isIndex(it.key))1 else 0 }
            for(entry in ordered){
                val target=destination(entry.key,alias)
                if(isIndex(entry.key))mergeIndex(entry.key,entry.value,target,
                    manifest.getString("device"))
                else copyNew(entry.key,entry.value,target,manifest.getString("device"))
            }
            runCatching{
                com.mediagenda.app.notifications.NotificationScheduler(context)
                    .scheduleAllProfiles()
            }
            return NearbyMergeResult(added,copies,conflicts)
        }finally{temp.deleteRecursively()}
    }
    private fun destination(path:String,map:Map<String,String>):File{
        if(path=="medicacao.json"||path=="agenda.json"){
            val mapped=map["legacy"]?:error("Perfil desconhecido.")
            return if(mapped=="legacy")File(root,path)
                else File(root,"saudepratica_profiles/"+mapped+"/"+path)
        }
        val bits=path.split("/")
        if(bits[0]=="medication_photos")return File(root,path)
        val id=map[bits[1]]?:error("Perfil de destino inválido.")
        if(bits[0]=="saudepratica_profiles"&&id=="legacy")return File(root,bits[2])
        return File(root,bits[0]+"/"+id+"/"+bits[2])
    }
    private fun isIndex(path:String)= when(path.substringAfterLast("/")){
        "medicacao.json","agenda.json","records.json",
        "index.json","lab_reports.json" -> true
        else->false
    }
    private fun atomic(dest:File,content:String){
        check(dest.parentFile!!.isDirectory || dest.parentFile!!.mkdirs())
        val f=AtomicFile(dest)
        val out=f.startWrite()
        try{out.write(content.toByteArray(Charsets.UTF_8));f.finishWrite(out)}
        catch(e:Exception){f.failWrite(out);throw e}
    }
    private fun conflict(path:String,incoming:File,origin:String){
        conflicts++
        val dir=File(root,"saudepratica_sync_conflicts").apply{mkdirs()}
        val marker=MessageDigest.getInstance("SHA-256")
            .digest((origin+"|"+path).toByteArray()).take(10)
            .joinToString(""){b->"%02x".format(b)}
        val output=File(dir,marker+"_"+UUID.randomUUID().toString()+".conflict")
        incoming.copyTo(output)
    }
    private fun copyNew(path:String,incoming:File,target:File,origin:String){
        if(target.exists()){
            if(target.length()!=incoming.length()||sha(target)!=sha(incoming))
                conflict(path,incoming,origin)
            return
        }
        check(target.parentFile!!.isDirectory||target.parentFile!!.mkdirs())
        incoming.copyTo(target)
        copies++
    }
    private fun applyPrefs(id:String,values:JSONObject){
        val pref=context.getSharedPreferences(profiles.preferencesFor(id),Context.MODE_PRIVATE)
        val editor=pref.edit()
        val keys=values.keys()
        while(keys.hasNext()){
            val key=keys.next()
            if(!key.matches(Regex("[a-zA-Z0-9_-]{1,90}"))||
                Regex("pin|pass|secret|token|auth|key",
                    RegexOption.IGNORE_CASE).containsMatchIn(key))continue
            val item=values.optJSONObject(key)?:continue
            val v=item.opt("v")?:continue
            if(pref.contains(key)){
                if(pref.all[key]?.toString()!=v.toString())conflicts++
                continue
            }
            when(item.optString("t")){
                "s"->editor.putString(key,v.toString().take(5000))
                "b"->if(v is Boolean)editor.putBoolean(key,v)
                "i"->if(v is Number)editor.putInt(key,v.toInt())
                "l"->if(v is Number)editor.putLong(key,v.toLong())
                "f"->if(v is Number)editor.putFloat(key,v.toFloat())
            }
        }
        check(editor.commit())
    }
    private fun fixImages(record:JSONObject){
        val pictures=record.optJSONArray("images")?:return
        val fixed=JSONArray()
        for(i in 0 until pictures.length()){
            val old=pictures.optString(i)
            val name=android.net.Uri.parse(old).lastPathSegment.orEmpty()
                .substringAfterLast("/")
            if(!namePattern.matches(name))continue
            val f=File(root,"medication_photos/"+name)
            if(f.isFile)fixed.put(f.absolutePath)
        }
        record.put("images",fixed)
    }
    private fun mergeIndex(path:String,incoming:File,target:File,origin:String){
        // Other per-profile JSON files contain specialized schemas. Preserve
        // them as immutable files, or as conflicts, without guessing fields.
        val filename=path.substringAfterLast("/")
        val field=when(filename){
            "medicacao.json"->"medications"
            "agenda.json"->"appointments"
            else->null
        }
        val remoteRoot=if(field!=null)JSONObject(incoming.readText()) else null
        val remote=if(field!=null)remoteRoot!!.getJSONArray(field)
            else JSONArray(incoming.readText())
        val localRoot=if(field!=null)
            if(target.isFile)JSONObject(target.readText())
            else JSONObject().put(field,JSONArray())
            else null
        val local=if(field!=null)localRoot!!.getJSONArray(field)
            else if(target.isFile)JSONArray(target.readText())else JSONArray()
        val ids=mutableMapOf<String,JSONObject>()
        for(i in 0 until local.length()){
            val o=local.optJSONObject(i)?:continue
            ids[o.optString("id")]=o
        }
        var changed=false
        for(i in 0 until remote.length()){
            val item=remote.optJSONObject(i)?:continue
            val id=item.optString("id")
            require(id.length in 1..150)
            if(filename=="medicacao.json")fixImages(item)
            val already=ids[id]
            if(already==null){
                // A confirmed dose is authoritative over the pending reminder.
                if(filename=="records.json"&&item.optString("kind")=="pending_intakes"){
                    val fields=item.optJSONObject("fields")
                    val hasConfirmation=(0 until local.length()).any{j->
                        val x=local.optJSONObject(j)
                        x?.optString("kind")=="intakes" &&
                        x.optJSONObject("fields")?.optString("medication_id")==fields?.optString("medication_id") &&
                        x.optJSONObject("fields")?.optString("scheduled_at")==fields?.optString("scheduled_at")
                    }
                    if(hasConfirmation)continue
                }
                local.put(item);ids[id]=item;added++;changed=true
            }else if(filename=="medicacao.json"){
                val pics=already.optJSONArray("images")?:JSONArray()
                val other=item.optJSONArray("images")?:JSONArray()
                for(j in 0 until other.length()){
                    val s=other.optString(j)
                    if((0 until pics.length()).none{k->pics.optString(k)==s}){
                        pics.put(s);changed=true
                    }
                }
                already.put("images",pics)
                val left=JSONObject(already.toString()).apply{remove("images")}
                val right=JSONObject(item.toString()).apply{remove("images")}
                if(left.toString()!=right.toString())conflict(path,incoming,origin)
            }else if(already.toString()!=item.toString()){
                conflict(path,incoming,origin)
            }
        }
        if(filename=="records.json"){
            val confirmed=mutableSetOf<String>()
            for(i in 0 until local.length()){
                val o=local.optJSONObject(i)?:continue
                if(o.optString("kind")=="intakes"){
                    val fields=o.optJSONObject("fields")?:continue
                    if(fields.optString("scheduled_at").isNotBlank())
                        confirmed.add(fields.optString("medication_id")+"|"+
                            fields.optString("scheduled_at"))
                }
            }
            val cleaned=JSONArray()
            for(i in 0 until local.length()){
                val o=local.optJSONObject(i)?:continue
                val fields=o.optJSONObject("fields")
                if(o.optString("kind")=="pending_intakes" &&
                    fields!=null && (fields.optString("medication_id")+"|"+
                        fields.optString("scheduled_at")) in confirmed){
                    changed=true;continue
                }
                cleaned.put(o)
            }
            if(changed)atomic(target,cleaned.toString())
        }else if(changed){
            if(field!=null)atomic(target,localRoot!!.put(field,local).toString())
            else atomic(target,local.toString())
        }
    }
}
