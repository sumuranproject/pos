package com.pentolrebus.kasir.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.pentolrebus.kasir.domain.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExport {
    /** Writes a real XLSX workbook into Downloads/Kasir. Returns a display path or null. */
    fun saveXlsx(
        context: Context,
        baseName: String,
        period: String,
        gross: Long,
        discount: Long,
        tax: Long,
        net: Long,
        expenses: Long,
        profit: Long,
        cash: Long,
        qris: Long,
        transactions: List<Transaction>
    ): String? = runCatching {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val name = "${baseName}_${stamp}.xlsx"
        val bytes = buildWorkbook(period, gross, discount, tax, net, expenses, profit, cash, qris, transactions)

        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Kasir")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("insert gagal")
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("output gagal")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
            "Download/Kasir/$name"
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Kasir").apply { mkdirs() }
            File(dir, name).also { it.writeBytes(bytes) }.absolutePath
        }
    }.getOrNull()

    private fun buildWorkbook(
        period: String,
        gross: Long,
        discount: Long,
        tax: Long,
        net: Long,
        expenses: Long,
        profit: Long,
        cash: Long,
        qris: Long,
        transactions: List<Transaction>
    ): ByteArray {
        val sheet = StringBuilder()
        sheet.append(xmlHeader())
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sheet.append("<sheetData>")

        row(sheet, 1, listOf("Laporan Keuangan", period))
        row(sheet, 2, listOf("Metrik", "Nilai"))
        row(sheet, 3, listOf("Penjualan kotor", gross))
        row(sheet, 4, listOf("Diskon", discount))
        row(sheet, 5, listOf("Pajak", tax))
        row(sheet, 6, listOf("Penjualan bersih", net))
        row(sheet, 7, listOf("Pengeluaran", expenses))
        row(sheet, 8, listOf("Laba bersih", profit))
        row(sheet, 9, listOf("Cash", cash))
        row(sheet, 10, listOf("QRIS", qris))
        row(sheet, 12, listOf("Transaksi", "Waktu", "Metode", "Total"))
        transactions.sortedBy { it.createdAt }.forEachIndexed { index, t ->
            row(sheet, 13 + index, listOf("#" + t.transactionId.takeLast(4).uppercase(), formatDateTime(t.createdAt), t.paymentMethod.name, t.total))
        }
        sheet.append("</sheetData></worksheet>")

        val workbook = """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Laporan Keuangan\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"""
        val rels = """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"""
        val workbookRels = """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"""
        val contentTypes = """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>
<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>"""

        return java.io.ByteArrayOutputStream().use { out ->
            ZipOutputStream(out).use { zip ->
                entry(zip, "[Content_Types].xml", contentTypes)
                entry(zip, "_rels/.rels", rels)
                entry(zip, "xl/workbook.xml", workbook)
                entry(zip, "xl/_rels/workbook.xml.rels", workbookRels)
                entry(zip, "xl/worksheets/sheet1.xml", sheet.toString())
            }
            out.toByteArray()
        }
    }

    private fun row(xml: StringBuilder, number: Int, values: List<Any>) {
        xml.append("<row r=\"").append(number).append("\">")
        values.forEachIndexed { index, value ->
            val ref = columnName(index + 1) + number
            if (value is Number) {
                xml.append("<c r=\"").append(ref).append("\"><v>").append(value).append("</v></c>")
            } else {
                xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escapeXml(value.toString())).append("</t></is></c>")
            }
        }
        xml.append("</row>")
    }

    private fun columnName(number: Int): String {
        var n = number
        val out = StringBuilder()
        while (n > 0) {
            val r = (n - 1) % 26
            out.append(('A'.code + r).toChar())
            n = (n - 1) / 26
        }
        return out.reverse().toString()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun entry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun xmlHeader() = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"

    private fun formatDateTime(ms: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID")).format(Date(ms))
}
