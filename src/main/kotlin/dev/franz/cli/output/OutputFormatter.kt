package dev.franz.cli.output

class OutputFormatter(private val emit: (String) -> Unit) {

    fun line(text: String = "") {
        emit(text)
    }

    fun section(title: String) {
        val normalized = if (title.endsWith(":")) title else "$title:"
        emit(normalized)
    }

    fun table(headers: List<String>, rows: List<List<String>>) {
        if (headers.isEmpty()) return
        val columnCount = headers.size
        val widths = IntArray(columnCount)

        headers.forEachIndexed { index, header ->
            widths[index] = header.length
        }
        rows.forEach { row ->
            row.forEachIndexed { index, value ->
                if (index < columnCount) {
                    widths[index] = maxOf(widths[index], value.length)
                }
            }
        }

        emit(formatRow(headers, widths))
        rows.forEach { row ->
            val normalizedRow = if (row.size == columnCount) row else row + List(columnCount - row.size) { "" }
            emit(formatRow(normalizedRow, widths))
        }
    }

    fun kvTable(pairs: List<Pair<String, String>>) {
        if (pairs.isEmpty()) return
        val keyWidth = pairs.maxOf { (key, _) -> "${key}:".length }
        pairs.forEach { (key, value) ->
            val keyLabel = "${key}:".padEnd(keyWidth)
            emit("$keyLabel  $value")
        }
    }

    private fun formatRow(columns: List<String>, widths: IntArray): String {
        val padded = columns.mapIndexed { index, value ->
            value.padEnd(widths[index])
        }
        return padded.joinToString("  ").trimEnd()
    }
}
