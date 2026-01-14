package dev.franz.cli.config

import com.charleskorn.kaml.Yaml
import dev.franz.cli.config.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.file.Path

/**
 * Exception thrown for configuration errors.
 */
class ConfigException(message: String) : RuntimeException(message)

/**
 * Resolved context containing all information needed to connect to a Kafka cluster.
 */
data class ResolvedContext(
    val contextName: String,
    val bootstrapServers: String,
    val securityProtocol: SecurityProtocol,
    val sasl: SaslConfig? = null,
    val ssl: SslConfig? = null,
    val kafkaProperties: Map<String, String> = emptyMap()
)

/**
 * Manages Franz CLI configuration stored at ~/.franz/config.
 * Handles loading, saving, and resolving contexts.
 */
class ConfigManager(
    private val configPath: Path = Path.of(System.getProperty("user.home"), ".franz", "config"),
    private val credentialResolver: CredentialResolver = CredentialResolver()
) {
    private var cachedConfig: FranzConfig? = null
    
    /**
     * Loads the configuration from disk.
     * Returns an empty config if the file doesn't exist.
     */
    fun loadConfig(): FranzConfig {
        val configFile = configPath.toFile()
        
        if (!configFile.exists()) {
            return FranzConfig()
        }
        
        return try {
            val yaml = configFile.readText()
            Yaml.default.decodeFromString<FranzConfig>(yaml)
        } catch (e: Exception) {
            throw ConfigException("Failed to load config: ${e.message}")
        }
    }
    
    /**
     * Gets the current context name from the config.
     */
    fun getCurrentContextName(): String? {
        return loadConfig().currentContext
    }
    
    /**
     * Gets a context by name.
     */
    fun getContext(name: String): ContextEntry? {
        return loadConfig().contexts.find { it.name == name }
    }
    
    /**
     * Gets a cluster by name.
     */
    fun getCluster(name: String): ClusterEntry? {
        return loadConfig().clusters.find { it.name == name }
    }
    
    /**
     * Gets an auth config by name.
     */
    fun getAuthConfig(name: String): AuthConfigEntry? {
        return loadConfig().authConfigs.find { it.name == name }
    }
    
    /**
     * Lists all contexts.
     */
    fun listContexts(): List<ContextEntry> {
        return loadConfig().contexts
    }
    
    /**
     * Lists all clusters.
     */
    fun listClusters(): List<ClusterEntry> {
        return loadConfig().clusters
    }
    
    /**
     * Lists all auth configs.
     */
    fun listAuthConfigs(): List<AuthConfigEntry> {
        return loadConfig().authConfigs
    }
    
    /**
     * Resolves a context name to a fully resolved context with all connection details.
     * If contextName is null, uses the current context.
     * 
     * @param contextName Name of the context to resolve, or null to use current context
     * @return Resolved context with connection details
     * @throws ConfigException if context, cluster, or auth config is not found
     */
    fun resolveContext(contextName: String?): ResolvedContext {
        val config = loadConfig()
        
        // Determine which context to use
        val effectiveContextName = contextName ?: config.currentContext
            ?: throw ConfigException("No current context set. Use 'franz config use-context <name>' to set one.")
        
        // Find the context
        val context = config.contexts.find { it.name == effectiveContextName }
            ?: throw ConfigException("Context '$effectiveContextName' not found. Use 'franz config get-contexts' to list available contexts.")
        
        // Find the cluster
        val cluster = config.clusters.find { it.name == context.cluster }
            ?: throw ConfigException("Cluster '${context.cluster}' not found for context '$effectiveContextName'.")
        
        // Find the auth config (optional)
        val authConfig = if (context.auth != null) {
            config.authConfigs.find { it.name == context.auth }
                ?: throw ConfigException("Auth config '${context.auth}' not found for context '$effectiveContextName'.")
        } else {
            null
        }
        
        return ResolvedContext(
            contextName = effectiveContextName,
            bootstrapServers = cluster.bootstrapServers,
            securityProtocol = authConfig?.securityProtocol ?: SecurityProtocol.PLAINTEXT,
            sasl = authConfig?.sasl,
            ssl = authConfig?.ssl,
            kafkaProperties = authConfig?.kafkaProperties ?: emptyMap()
        )
    }
    
    /**
     * Saves the configuration to disk.
     */
    fun saveConfig(config: FranzConfig) {
        val configFile = configPath.toFile()
        configFile.parentFile.mkdirs()
        
        val yaml = Yaml.default.encodeToString(config)
        configFile.writeText(yaml)
        cachedConfig = null
    }
    
    /**
     * Sets the current context.
     */
    fun setCurrentContext(contextName: String) {
        val config = loadConfig()
        
        // Verify the context exists
        if (config.contexts.none { it.name == contextName }) {
            throw ConfigException("Context '$contextName' not found.")
        }
        
        saveConfig(config.copy(currentContext = contextName))
    }
    
    /**
     * Adds or updates a context.
     */
    fun setContext(context: ContextEntry) {
        val config = loadConfig()
        val existingIndex = config.contexts.indexOfFirst { it.name == context.name }
        
        val newContexts = if (existingIndex >= 0) {
            config.contexts.toMutableList().apply { this[existingIndex] = context }
        } else {
            config.contexts + context
        }
        
        saveConfig(config.copy(contexts = newContexts))
    }
    
    /**
     * Adds or updates a cluster.
     */
    fun setCluster(cluster: ClusterEntry) {
        val config = loadConfig()
        val existingIndex = config.clusters.indexOfFirst { it.name == cluster.name }
        
        val newClusters = if (existingIndex >= 0) {
            config.clusters.toMutableList().apply { this[existingIndex] = cluster }
        } else {
            config.clusters + cluster
        }
        
        saveConfig(config.copy(clusters = newClusters))
    }
    
    /**
     * Adds or updates an auth config.
     */
    fun setAuthConfig(authConfig: AuthConfigEntry) {
        val config = loadConfig()
        val existingIndex = config.authConfigs.indexOfFirst { it.name == authConfig.name }
        
        val newAuthConfigs = if (existingIndex >= 0) {
            config.authConfigs.toMutableList().apply { this[existingIndex] = authConfig }
        } else {
            config.authConfigs + authConfig
        }
        
        saveConfig(config.copy(authConfigs = newAuthConfigs))
    }
    
    /**
     * Deletes a context by name.
     */
    fun deleteContext(contextName: String): Boolean {
        val config = loadConfig()
        val newContexts = config.contexts.filter { it.name != contextName }
        
        if (newContexts.size == config.contexts.size) {
            return false
        }
        
        val newCurrentContext = if (config.currentContext == contextName) null else config.currentContext
        saveConfig(config.copy(contexts = newContexts, currentContext = newCurrentContext))
        return true
    }
    
    /**
     * Deletes a cluster by name.
     */
    fun deleteCluster(clusterName: String): Boolean {
        val config = loadConfig()
        val newClusters = config.clusters.filter { it.name != clusterName }
        
        if (newClusters.size == config.clusters.size) {
            return false
        }
        
        saveConfig(config.copy(clusters = newClusters))
        return true
    }
    
    /**
     * Deletes an auth config by name.
     */
    fun deleteAuthConfig(authConfigName: String): Boolean {
        val config = loadConfig()
        val newAuthConfigs = config.authConfigs.filter { it.name != authConfigName }
        
        if (newAuthConfigs.size == config.authConfigs.size) {
            return false
        }
        
        saveConfig(config.copy(authConfigs = newAuthConfigs))
        return true
    }
}
