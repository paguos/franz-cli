package dev.franz.cli.kafka

import dev.franz.cli.config.ConfigException
import dev.franz.cli.config.ConfigManager
import dev.franz.cli.config.KafkaPropertiesBuilder
import dev.franz.cli.config.ResolvedContext
import dev.franz.cli.config.model.SecurityProtocol
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("KafkaService Context Integration")
class KafkaServiceContextTest {
    
    private lateinit var configManager: ConfigManager
    private lateinit var propertiesBuilder: KafkaPropertiesBuilder
    
    @BeforeEach
    fun setUp() {
        configManager = mockk()
        propertiesBuilder = mockk()
        KafkaService.resetInstance()
    }
    
    @AfterEach
    fun tearDown() {
        KafkaService.resetInstance()
    }
    
    @Test
    fun `configureFromContext creates service from resolved context`() {
        val resolvedContext = ResolvedContext(
            contextName = "dev",
            bootstrapServers = "localhost:9092",
            securityProtocol = SecurityProtocol.PLAINTEXT
        )
        
        every { configManager.resolveContext("dev") } returns resolvedContext
        every { propertiesBuilder.build(resolvedContext) } returns java.util.Properties().apply {
            put("bootstrap.servers", "localhost:9092")
            put("security.protocol", "PLAINTEXT")
        }
        
        KafkaService.configureFromContext("dev", configManager, propertiesBuilder)
        
        val service = KafkaService.getInstance()
        assertThat(service).isNotNull
    }
    
    @Test
    fun `configureFromContext uses current context when name is null`() {
        val resolvedContext = ResolvedContext(
            contextName = "default",
            bootstrapServers = "localhost:9092",
            securityProtocol = SecurityProtocol.PLAINTEXT
        )
        
        every { configManager.resolveContext(null) } returns resolvedContext
        every { propertiesBuilder.build(resolvedContext) } returns java.util.Properties().apply {
            put("bootstrap.servers", "localhost:9092")
            put("security.protocol", "PLAINTEXT")
        }
        
        KafkaService.configureFromContext(null, configManager, propertiesBuilder)
        
        verify { configManager.resolveContext(null) }
    }
    
    @Test
    fun `throws helpful error when no context configured`() {
        every { configManager.resolveContext(null) } throws 
            ConfigException("No current context set. Use 'franz config use-context <name>' to set one.")
        
        assertThatThrownBy {
            KafkaService.configureFromContext(null, configManager, propertiesBuilder)
        }.isInstanceOf(ConfigException::class.java)
         .hasMessageContaining("No current context")
         .hasMessageContaining("franz config use-context")
    }
    
    @Test
    fun `throws error when context not found`() {
        every { configManager.resolveContext("nonexistent") } throws 
            ConfigException("Context 'nonexistent' not found.")
        
        assertThatThrownBy {
            KafkaService.configureFromContext("nonexistent", configManager, propertiesBuilder)
        }.isInstanceOf(ConfigException::class.java)
         .hasMessageContaining("not found")
    }
}
