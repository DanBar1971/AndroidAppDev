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
print("Review keeps category and source PDF page for every confirmed laboratory value.")
