package ir.hesabdari.shop.report

enum class Tone { NEUTRAL, GAIN, LOSS, MUTED }

data class KV(val label: String, val value: String, val tone: Tone = Tone.NEUTRAL, val bold: Boolean = false)

/** A format independent document that can be shown on screen, shared as text or written to a PDF. */
sealed interface Block {
    data class Heading(val text: String) : Block
    data class Paragraph(val text: String, val small: Boolean = false, val center: Boolean = false) : Block
    data class KeyValues(val rows: List<KV>) : Block
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        /** Relative column widths. */
        val weights: List<Float> = headers.map { 1f },
        val footer: List<String>? = null,
    ) : Block

    data object Spacer : Block
}

data class ReportDoc(
    val title: String,
    val subtitle: String? = null,
    val blocks: List<Block>,
    val fileName: String = "report",
)
