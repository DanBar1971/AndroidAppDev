#!/usr/bin/env python3
"""SaúdePrática 1.9.118 — reorganiza edição de medicação, sem alterar alarmes.

O formulário conserva os controlos originais (e os seus listeners), movendo-os
para um resumo inicial e cinco secções expansíveis. A via e o subtipo injetável
são atributos explícitos do JSON de cada medicamento/perfil.
"""
from pathlib import Path
root=Path("build-src/MediAgenda_Android_v1/app")
src=root/"src/main/java/com/mediagenda/app"

def replace(path,old,new,label):
    s=path.read_text(encoding="utf-8")
    n=s.count(old)
    assert n==1,(label,str(path),n)
    path.write_text(s.replace(old,new,1),encoding="utf-8")

models=src/"model/Models.kt"
replace(models,'''    var dose: String = "",
    var indication: String = "",''','''    var dose: String = "",
    // Vias de administração indicadas na prescrição (não inferir pelas doses).
    var administrationRoute: String = "",
    var injectionType: String = "",
    var indication: String = "",''',"medication model fields")

repo=src/"data/DataRepository.kt"
replace(repo,'''                dose = o.optString("dose"),
                indication = o.optString("indication"),''','''                dose = o.optString("dose"),
                administrationRoute = o.optString("administration_route"),
                injectionType = o.optString("injection_type"),
                indication = o.optString("indication"),''',"load route fields")
replace(repo,'''                .put("dose", m.dose)
                .put("indication", m.indication)''','''                .put("dose", m.dose)
                .put("administration_route", m.administrationRoute)
                .put("injection_type", m.injectionType)
                .put("indication", m.indication)''',"persist route fields")

main=src/"MainActivity.kt"
replace(main,'''        val dose = input("Dose", m.dose)
        val indication = input("Posologia / indicação", m.indication)''',
'''        val dose = input("Dose", m.dose)
        // Registar a via real indicada, independentemente do horário/tipo toma.
        val administrationRoutes=listOf(
            "Por indicar" to "",
            "Via oral" to "oral",
            "Via sublingual" to "sublingual",
            "Via parentérica · Injeção" to "parenteral",
            "Via tópica / cutânea" to "topical",
            "Via inalatória" to "inhaled",
            "Via retal" to "rectal",
            "Via vaginal" to "vaginal",
            "Via ocular" to "ocular",
            "Via otológica" to "otic",
            "Via nasal" to "nasal",
            "Outra" to "other"
        )
        val injectionTypes=listOf(
            "Por especificar" to "",
            "Subcutânea" to "sc",
            "Intramuscular" to "im",
            "Intravenosa" to "iv",
            "Intradérmica" to "id",
            "Outra injeção" to "other"
        )
        var selectedRoute=m.administrationRoute.takeIf {
            code->administrationRoutes.any{it.second==code}
        } ?: ""
        var selectedInjection=m.injectionType.takeIf {
            code->injectionTypes.any{it.second==code}
        } ?: ""
        lateinit var routeButton:MaterialButton
        lateinit var injectionButton:MaterialButton
        routeButton=secondaryButton("Via de administração: "+
            administrationRoutes.first{it.second==selectedRoute}.first){
            MaterialAlertDialogBuilder(this)
                .setTitle("Via de administração prescrita")
                .setItems(administrationRoutes.map{it.first}.toTypedArray()){_,index->
                    selectedRoute=administrationRoutes[index].second
                    routeButton.text="Via de administração: "+administrationRoutes[index].first
                    injectionButton.visibility=if(selectedRoute=="parenteral")
                        View.VISIBLE else View.GONE
                }.setNegativeButton("Cancelar",null).show()
        }
        injectionButton=secondaryButton("Tipo de injeção: "+
            injectionTypes.first{it.second==selectedInjection}.first){
            MaterialAlertDialogBuilder(this)
                .setTitle("Tipo de injeção prescrito")
                .setItems(injectionTypes.map{it.first}.toTypedArray()){_,index->
                    selectedInjection=injectionTypes[index].second
                    injectionButton.text="Tipo de injeção: "+injectionTypes[index].first
                }.setNegativeButton("Cancelar",null).show()
        }
        injectionButton.visibility=if(selectedRoute=="parenteral")
            View.VISIBLE else View.GONE
        val indication = input("Posologia / indicação", m.indication)''',"route controls")

replace(main,'''                m.indication=indication.edit.text.toString().trim()
                m.reminderTime=reminder.edit.text.toString().trim()''',
'''                m.indication=indication.edit.text.toString().trim()
                m.administrationRoute=selectedRoute
                m.injectionType=if(selectedRoute=="parenteral")selectedInjection else ""
                m.reminderTime=reminder.edit.text.toString().trim()''',"save route without touching alarm logic")

# Reparent exactly the existing views; moving a View preserves its listeners
# (photos, prescription scans, suspension, repetition, SOS, and PDF actions).
# The existing dialog already expands to the full screen in v1.9.116.
replace(main,'''        activeMedicationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(if(fromScan) "Confirmar medicação" else if(existing==null) "Nova medicação" else "Editar medicação")''',
'''        // Ficha simplificada: resumo principal + cinco grupos expansíveis.
        val oldViews=(0 until box.childCount).map{box.getChildAt(it)}
        fun itemText(view:View):String=when(view){
            is android.widget.TextView -> view.text?.toString().orEmpty()
            is ViewGroup -> (0 until view.childCount).joinToString(" ") {
                itemText(view.getChildAt(it))
            }
            else -> ""
        }
        val nameIndex=oldViews.indexOf(name.container)
        val datesIndex=oldViews.indexOf(dates.container)
        val searchIndex=oldViews.indexOfFirst{
            itemText(it).contains("Procurar +info do medicamento")
        }
        val leafletIndex=oldViews.indexOfFirst{
            itemText(it).contains("Bula / Folheto informativo")
        }.let{if(it<0)oldViews.size else it}
        val photoBottomIndex=oldViews.indexOfFirst{
            itemText(it).contains("Anexar foto existente da embalagem") ||
            itemText(it).contains("Adicionar foto da caixa a esta ficha")
        }.let{if(it<0)oldViews.size else it}
        require(nameIndex>=0 && datesIndex>nameIndex &&
            searchIndex>datesIndex && leafletIndex>=searchIndex &&
            photoBottomIndex>=leafletIndex){
            "Estrutura da ficha alterada; não reordenar campos sem revisão."
        }

        val photoGroup=mutableListOf<View>()
        val leafletGroup=mutableListOf<View>()
        val infoGroup=mutableListOf<View>()
        val suspensionGroup=mutableListOf<View>()
        val scanPrelude=if(fromScan && nameIndex>0)oldViews.first() else null
        oldViews.subList(if(scanPrelude!=null)1 else 0,nameIndex).forEach{
            if(itemText(it).contains("Bula do medicamento"))leafletGroup.add(it)
            else photoGroup.add(it)
        }
        val prescriptionGroup=listOf(prescription.container,prescriptionDate.container,
            dispense.container,option.container,validUntil.container)
        val repetitionGroup=oldViews.subList(datesIndex,searchIndex).filterNot{
            itemText(it).contains("Selecione a repetição")
        }
        infoGroup.add(oldViews[searchIndex])
        oldViews.subList(searchIndex+1,leafletIndex).forEach {
            if(itemText(it).contains("Registar toma SOS agora"))infoGroup.add(it)
            else suspensionGroup.add(it)
        }
        leafletGroup.addAll(oldViews.subList(leafletIndex,photoBottomIndex))
        photoGroup.addAll(oldViews.subList(photoBottomIndex,oldViews.size))
        box.removeAllViews()

        scanPrelude?.let{box.addView(it)}
        // Cabeçalho compacto; mostra apenas a fotografia VERDADEIRA já guardada.
        val heading=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
        }
        val nameAndDose=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            addView(name.container)
            addView(dose.container)
        }
        heading.addView(nameAndDose,LinearLayout.LayoutParams(0,-2,1f))
        if(existing!=null && existing.images.isNotEmpty()){
            MedicationPhotoLoader.load(this,existing.images,180)?.let{image->
                heading.addView(android.widget.ImageView(this).apply{
                    setImageBitmap(image)
                    scaleType=android.widget.ImageView.ScaleType.FIT_CENTER
                    contentDescription="Fotografia guardada da embalagem"
                },LinearLayout.LayoutParams(dp(92),dp(112)).apply{
                    marginStart=dp(8)
                })
            }
        }
        box.addView(heading)
        box.addView(routeButton)
        box.addView(injectionButton)
        box.addView(dosePatternButton)
        box.addView(indication.container)
        box.addView(reminder.container)
        box.addView(doseTimes.container)
        box.addView(disableMedicationAlarm)
        suspensionGroup.forEach{box.addView(it)}
        box.addView(medicationNotes.container)

        fun expandableMedicationGroup(title:String,items:List<View>){
            if(items.isEmpty())return
            val content=LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                visibility=View.GONE
            }
            items.forEach{content.addView(it)}
            lateinit var toggle:MaterialButton
            toggle=secondaryButton("▸ $title"){
                val expanding=content.visibility!=View.VISIBLE
                content.visibility=if(expanding)View.VISIBLE else View.GONE
                toggle.text=(if(expanding)"▾ " else "▸ ")+title
                toggle.contentDescription=title+
                    if(expanding)" · Fechar grupo" else " · Abrir grupo"
            }
            toggle.contentDescription=title+" · Abrir grupo"
            box.addView(toggle)
            box.addView(content)
        }
        expandableMedicationGroup("Prescrição",prescriptionGroup)
        expandableMedicationGroup("Repetições",repetitionGroup)
        expandableMedicationGroup("Informação",infoGroup)
        expandableMedicationGroup("Bula / Folheto informativo",leafletGroup)
        expandableMedicationGroup("Foto da embalagem",photoGroup)
        activeMedicationDialog = MaterialAlertDialogBuilder(this)
            .setTitle(if(fromScan) "Confirmar medicação" else if(existing==null) "Nova medicação" else "Editar medicação")''',"grouped full-screen medication editor")

gradle=root/"build.gradle.kts"
g=gradle.read_text(encoding="utf-8")
assert g.count("versionCode = 127")==1 and g.count('versionName = "1.9.117"')==1
g=g.replace("versionCode = 127","versionCode = 128",1)
g=g.replace('versionName = "1.9.117"','versionName = "1.9.118"',1)
gradle.write_text(g,encoding="utf-8")
print("SaúdePrática v1.9.118: grouped medication editor and persisted route fields, alarms untouched.")
