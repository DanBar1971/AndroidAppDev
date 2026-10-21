package com.mediagenda.app.ocr

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** OCR proposals, never committed without the user's explicit confirmation. */
object ProfileCitizenCardOcr {
    data class Proposed(val key:String,val label:String,val value:String)
    private val dateRe=Regex("""(?<!\d)(\d{2})[./\- ](\d{2})[./\- ](\d{4})(?!\d)""")
    private val isoRe=Regex("""(?<!\d)(\d{4})[./\-](\d{2})[./\-](\d{2})(?!\d)""")
    private val numeric=Regex("""(?<!\d)\d[\d .-]{7,20}\d(?!\d)""")
    private val numberCC=Regex("""\b\d{8}[A-Z0-9]{1,4}\b""")

    private fun canonical(t:String):String =
        Normalizer.normalize(t.uppercase(Locale.ROOT),Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""),"").replace(Regex("""\s+""")," ").trim()
    private fun rows(t:String)=t.lines().map{it.trim()}.filter{it.isNotBlank()}

    private fun nextValue(lines:List<String>,tags:List<String>):String {
        for(i in lines.indices){
            if(tags.none{canonical(lines[i]).contains(it)})continue
            val split=lines[i].split(":",limit=2)
            if(split.size==2&&split[1].isNotBlank())return split[1].trim()
            for(j in i+1..minOf(i+2,lines.lastIndex)){
                val n=canonical(lines[j])
                if(n.isBlank())continue
                if(n.startsWith("APELIDO")||n.startsWith("NOME(S)")||
                    n.startsWith("DATA DE ")||n.startsWith("NACIONALIDADE")||
                    n.startsWith("VALIDADE")||n.startsWith("SEXO")||
                    n.startsWith("IDENTIFICACAO")||n.startsWith("SEGURANCA SOCIAL"))
                    break
                return lines[j].trim()
            }
        }
        return ""
    }
    private fun readDate(value:String):String? {
        val match=dateRe.find(value)
        val iso=isoRe.find(value)
        val normalized=when{
            match!=null -> "${match.groupValues[1]}/${match.groupValues[2]}/${match.groupValues[3]}"
            iso!=null -> "${iso.groupValues[3]}/${iso.groupValues[2]}/${iso.groupValues[1]}"
            else->return null
        }
        return runCatching{
            LocalDate.parse(normalized,
                DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))
            normalized
        }.getOrNull()
    }
    private fun labelledDate(lines:List<String>,tags:List<String>):String?{
        for(i in lines.indices){
            if(tags.none{canonical(lines[i]).contains(it)})continue
            for(j in i..minOf(i+2,lines.lastIndex))readDate(lines[j])?.let{return it}
        }
        return null
    }
    private fun labelledNumber(lines:List<String>,tags:List<String>,length:Int):String? {
        for(i in lines.indices){
            if(tags.none{canonical(lines[i]).contains(it)})continue
            for(j in i..minOf(i+2,lines.lastIndex)){
                if(j>i&&listOf("IDENTIFICACAO FISCAL","SEGURANCA SOCIAL",
                    "UTENTE DE SAUDE").any{canonical(lines[j]).contains(it)})break
                for(match in numeric.findAll(lines[j])){
                    val digits=match.value.filter(Char::isDigit)
                    if(digits.length==length)return digits
                }
            }
        }
        return null
    }
    fun propose(frontText:String,backText:String):List<Proposed>{
        val front=rows(frontText)
        val back=rows(backText)
        val list=linkedMapOf<String,Proposed>()
        fun add(key:String,label:String,value:String?){
            if(!value.isNullOrBlank())list[key]=Proposed(key,label,value.trim())
        }
        fun plausibleName(s:String)=s.length in 2..90&&s.any(Char::isLetter)&&
            s.none(Char::isDigit)&&canonical(s) !in setOf(
                "NACIONALIDADE","NATIONALITY","SEXO","VALIDADE","PORTUGAL",
                "APELIDO(S)","SURNAME","NOME(S)","GIVEN NAME")
        val surname=nextValue(front,listOf("APELIDO(S)","APELIDOS","SURNAME"))
        val given=nextValue(front,listOf("NOME(S)","GIVEN NAME"))
        if(plausibleName(surname)&&plausibleName(given))
            add("full_name","Nome completo","$given $surname")
        add("birth_date","Data de nascimento",labelledDate(front,listOf(
            "DATA DE NASCIMENTO","DATE OF BIRTH","NASCIMENTO")))
        add("cc_valid_until","Validade do CC",labelledDate(front,listOf(
            "VALIDADE","EXPIRY DATE")))
        add("gender","Género",when(canonical(nextValue(front,listOf("SEXO","SEX")))){
            "F","FEMININO","FEMALE"->"Feminino"
            "M","MASCULINO","MALE"->"Masculino"
            else->null
        })
        val cc=nextValue(front,listOf("DOCUMENTO","DOCUMENT NO","DOC. N"))
        add("cc_number","Número do CC",numberCC.find(canonical(cc))?.value)
        // The Portuguese card may show nationality, height and family lineage.
        // Extract only explicitly labelled values; never infer missing content.
        val nationality=nextValue(front,listOf("NACIONALIDADE","NATIONALITY"))
            .replace(Regex("""[^\p{L} /-]"""),"").trim()
        if(nationality.length in 2..45)
            add("nationality","Nacionalidade",nationality)
        val height=nextValue(front,listOf("ALTURA","HEIGHT"))
        val heightMatch=Regex("""\b([1-2])[.,]?(\d{2})\b""").find(height)
        val cm=heightMatch?.let {
            val number=it.groupValues[1].toInt()*100+it.groupValues[2].toInt()
            number.takeIf{h->h in 100..240}?.toString()
        }
        add("height_cm","Altura (cm)",cm)
        val birthPlace=nextValue(front+back,listOf("NATURALIDADE","LOCAL DE NASCIMENTO","PLACE OF BIRTH"))
        if(birthPlace.length in 3..100 && birthPlace.none(Char::isDigit))
            add("birth_place","Naturalidade",birthPlace)
        val father=nextValue(back,listOf("NOME DO PAI","FATHER NAME","PAI:"))
        val mother=nextValue(back,listOf("NOME DA MAE","MOTHER NAME","MAE:"))
        if(plausibleName(father)) add("father_name","Nome do pai",father)
        if(plausibleName(mother)) add("mother_name","Nome da mãe",mother)
        val backNumbers=CitizenCardOcrParser.parse(backText)
        add("nif","NIF",backNumbers.nif.takeIf(CitizenCardOcrParser::validNif))
        add("sns_number","Número de utente SNS",backNumbers.snsNumber.takeIf{
            it.matches(Regex("""\d{9}"""))
        })
        add("niss","NISS",labelledNumber(back,
            listOf("SEGURANCA SOCIAL","SOCIAL SECURITY","NISS"),11))
        return list.values.toList()
    }
}
