package ir.hesabdari.shop.report

object TextRenderer {
    fun render(doc: ReportDoc): String {
        val sb = StringBuilder()
        sb.appendLine(doc.title)
        doc.subtitle?.takeIf { it.isNotBlank() }?.let { sb.appendLine(it) }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        for (b in doc.blocks) {
            when (b) {
                is Block.Heading -> {
                    sb.appendLine()
                    sb.appendLine("■ ${b.text}")
                }
                is Block.Paragraph -> sb.appendLine(b.text)
                is Block.KeyValues -> b.rows.forEach { sb.appendLine("${it.label}: ${it.value}") }
                is Block.Table -> {
                    b.rows.forEachIndexed { i, row ->
                        sb.appendLine("${i + 1}) " + row.joinToString(" | "))
                    }
                    b.footer?.let { sb.appendLine("∑ " + it.filter { c -> c.isNotBlank() }.joinToString(" | ")) }
                }
                Block.Spacer -> sb.appendLine()
            }
        }
        return sb.toString().trimEnd() + "\n"
    }
}
