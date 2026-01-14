package dev.franz.cli.output

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OutputFormatterTest {

    @Test
    fun `table renders kubectl-like aligned columns`() {
        val lines = mutableListOf<String>()
        val formatter = OutputFormatter { line -> lines.add(line) }

        formatter.table(
            headers = listOf("NAME", "READY", "UP-TO-DATE", "AVAILABLE", "AGE"),
            rows = listOf(
                listOf("nginx", "1/1", "1", "1", "1d"),
                listOf("api-server", "2/2", "2", "2", "10d")
            )
        )

        assertThat(lines).containsExactly(
            "NAME        READY  UP-TO-DATE  AVAILABLE  AGE",
            "nginx       1/1    1           1          1d",
            "api-server  2/2    2           2          10d"
        )
    }

    @Test
    fun `kvTable aligns keys with colon like kubectl describe`() {
        val lines = mutableListOf<String>()
        val formatter = OutputFormatter { line -> lines.add(line) }

        formatter.kvTable(
            listOf(
                "Name" to "nginx",
                "Namespace" to "default",
                "Labels" to "app=nginx"
            )
        )

        assertThat(lines).containsExactly(
            "Name:       nginx",
            "Namespace:  default",
            "Labels:     app=nginx"
        )
    }
}
