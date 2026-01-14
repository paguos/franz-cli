package dev.franz.cli

import dev.franz.cli.kafka.KafkaService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files

@DisplayName("Franz CLI")
class FranzTest {
    
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var errorStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream
    private lateinit var originalUserHome: String
    
    @BeforeEach
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        errorStream = ByteArrayOutputStream()
        originalOut = System.out
        originalErr = System.err
        System.setOut(PrintStream(outputStream))
        System.setErr(PrintStream(errorStream))
        KafkaService.resetInstance()

        // Make tests hermetic: avoid reading developer machine config from ~/.franz/config
        originalUserHome = System.getProperty("user.home")
        val tempHome = Files.createTempDirectory("franz-cli-test-home").toAbsolutePath().toString()
        System.setProperty("user.home", tempHome)
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        System.setErr(originalErr)
        KafkaService.resetInstance()
        System.setProperty("user.home", originalUserHome)
    }

    private fun getAllOutput(): String = outputStream.toString() + errorStream.toString()
    
    @Test
    fun `--help shows context option`() {
        try {
            Franz().main(arrayOf("--help"))
        } catch (e: Exception) {
            // Clikt throws on help
        }
        
        val output = outputStream.toString()
        assertThat(output).contains("--context")
        assertThat(output).contains("-c")
    }
    
    @Test
    fun `--help does not show bootstrap-servers option`() {
        try {
            Franz().main(arrayOf("--help"))
        } catch (e: Exception) {
            // Clikt throws on help
        }
        
        val output = outputStream.toString()
        assertThat(output).doesNotContain("--bootstrap-servers")
        assertThat(output).doesNotContain("-b")
    }
    
    @Test
    fun `config subcommand is available`() {
        try {
            Franz().main(arrayOf("config", "--help"))
        } catch (e: Exception) {
            // Clikt throws on help
        }
        
        val output = outputStream.toString()
        assertThat(output).contains("get-contexts")
        assertThat(output).contains("use-context")
        assertThat(output).contains("set-context")
        assertThat(output).contains("set-cluster")
        assertThat(output).contains("set-credentials")
    }
    
    @Test
    fun `errors when no context configured`() {
        assertThatThrownBy {
            // In this Clikt integration, parse() doesn't invoke run(), so we call run() ourselves.
            val cmd = Franz()
            cmd.parse(arrayOf("get", "topic"))
            cmd.run()
        }.hasMessageContaining("No current context")
            .hasMessageContaining("franz config use-context")
    }
}
