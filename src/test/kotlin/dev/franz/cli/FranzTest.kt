package dev.franz.cli

import dev.franz.cli.kafka.KafkaService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@DisplayName("Franz CLI")
class FranzTest {
    
    private lateinit var outputStream: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    
    @BeforeEach
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        originalOut = System.out
        System.setOut(PrintStream(outputStream))
        KafkaService.resetInstance()
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        KafkaService.resetInstance()
    }
    
    @Test
    fun `--mock flag uses mock data`() {
        Franz().main(arrayOf("--mock", "get", "topic"))
        
        val output = outputStream.toString()
        // Mock data includes these topics
        assertThat(output).contains("my-topic")
    }
    
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
    fun `defaults to mock when no context configured`() {
        // When no context is configured, should fall back to mock
        Franz().main(arrayOf("get", "topic"))
        
        val output = outputStream.toString()
        // Should work (mock mode) rather than fail
        assertThat(output).contains("Listing topics")
    }
}
