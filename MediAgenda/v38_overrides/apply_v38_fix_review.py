from pathlib import Path

file=Path("build-src/MediAgenda_Android_v1/app/src/main/java/com/mediagenda/app/ClinicalLabsActivity.kt")
s=file.read_text(encoding="utf-8")
old="        val entries=mutableListOf<List<EditText>>()"
new="        val entries=mutableListOf<Pair<LabValue,List<EditText>>>()"
assert old in s
s=s.replace(old,new,1)
old="            entries.add(listOf(a,b,c,d,e))"
new="            entries.add(v to listOf(a,b,c,d,e))"
assert old in s
s=s.replace(old,new,1)
old='''                val values=entries.mapNotNull { group->
                    val parts=group.map{it.text.toString().trim()}
                    if(parts[0].isBlank()||parts[1].isBlank())null else
                        LabValue("",parts[0],parts[1],parts[2],parts[3],parts[4],1)
                }'''
new='''                val values=entries.mapNotNull { (original,group)->
                    val parts=group.map{it.text.toString().trim()}
                    if(parts[0].isBlank()||parts[1].isBlank())null else
                        LabValue(original.section,parts[0],parts[1],parts[2],
                            parts[3],parts[4],original.page)
                }'''
assert old in s
s=s.replace(old,new,1)
file.write_text(s,encoding="utf-8")
store=Path("build-src/MediAgenda_Android_v1/app/src/main/java/com/mediagenda/app/labs/ClinicalLabStore.kt")
s=store.read_text(encoding="utf-8")
old='        val suffix=if(mime=="application/pdf")".pdf" else ".jpg"'
new='''        val suffix=when(mime){
            "application/pdf"->".pdf"
            "image/png"->".png"
            "image/webp"->".webp"
            "image/heic"->".heic"
            "image/heif"->".heif"
            else->".jpg"
        }'''
assert old in s
s=s.replace(old,new,1)
old='(pdf|jpg)'
assert old in s
s=s.replace(old,'(pdf|jpg|png|webp|heic|heif)',1)
store.write_text(s,encoding="utf-8")
print("Lab reports retain source page/section and correct original image file types.")
