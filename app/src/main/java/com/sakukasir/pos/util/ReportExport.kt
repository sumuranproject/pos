package com.sakukasir.pos.util

import android.os.Environment
import com.sakukasir.pos.domain.Expense
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExport {
    fun exportExpenses(expenses:List<Expense>):File {
        val dir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"SakuKasir").apply{mkdirs()}
        val out=File(dir,"pengeluaran-${System.currentTimeMillis()}.xlsx")
        ZipOutputStream(out.outputStream()).use{zip->
            fun entry(name:String,text:String){zip.putNextEntry(ZipEntry(name));zip.write(text.toByteArray());zip.closeEntry()}
            entry("[Content_Types].xml","""<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
            entry("_rels/.rels","""<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
            entry("xl/workbook.xml","""<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Pengeluaran" sheetId="1" r:id="rId1"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels","""<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
            val rows=buildString{
                append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
                fun row(vararg cells:String){append("<row>");cells.forEach{append("<c t=\"inlineStr\"><is><t>${it.replace("&","&amp;").replace("<","&lt;")}</t></is></c>")};append("</row>")}
                row("ID","Jumlah","Kategori","Catatan","Oleh")
                expenses.forEach{row(it.id,it.amount.toString(),it.category,it.note,it.by)}
                append("</sheetData></worksheet>")
            }
            entry("xl/worksheets/sheet1.xml",rows)
        }
        return out
    }
}
