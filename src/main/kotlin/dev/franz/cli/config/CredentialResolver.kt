package dev.franz.cli.config

import java.io.File

/**
 * Exception thrown when credential resolution fails.
 */
class CredentialResolutionException(message: String) : RuntimeException(message)

/**
 * Resolves credentials from various sources:
 * - Inline values
 * - File references
 * - Environment variables
 * 
 * Supports tilde expansion for home directory paths.
 */
class CredentialResolver(
    private val homeDir: String = System.getProperty("user.home"),
    private val envProvider: (String) -> String? = { System.getenv(it) }
) {
    
    /**
     * Resolves a simple value (passthrough).
     */
    fun resolve(value: String?): String? {
        return value
    }
    
    /**
     * Reads a credential from a file.
     * Supports tilde expansion for home directory.
     * 
     * @param path Path to the file containing the credential
     * @return The file contents with whitespace trimmed, or null if path is null
     * @throws CredentialResolutionException if the file doesn't exist or can't be read
     */
    fun resolveFile(path: String?): String? {
        if (path == null) return null
        
        val expandedPath = expandPath(path) ?: return null
        val file = File(expandedPath)
        
        if (!file.exists()) {
            throw CredentialResolutionException("Credential file not found: $path")
        }
        
        return try {
            file.readText().trim()
        } catch (e: Exception) {
            throw CredentialResolutionException("Failed to read credential file: $path - ${e.message}")
        }
    }
    
    /**
     * Resolves environment variable references in a string.
     * Environment variables are referenced using ${VAR_NAME} syntax.
     * 
     * @param value String potentially containing environment variable references
     * @return The resolved string with all env vars substituted
     * @throws CredentialResolutionException if a referenced env var is not set
     */
    fun resolveEnvVar(value: String?): String? {
        if (value == null) return null
        
        val envVarPattern = """\$\{([^}]+)}""".toRegex()
        
        return envVarPattern.replace(value) { matchResult ->
            val varName = matchResult.groupValues[1]
            val envValue = envProvider(varName)
                ?: throw CredentialResolutionException(
                    "Environment variable '$varName' is not set"
                )
            envValue
        }
    }
    
    /**
     * Resolves a password from either inline value or file.
     * Inline password takes precedence over file.
     * Environment variables in inline password are resolved.
     * 
     * @param inlinePassword Direct password value (may contain env vars)
     * @param passwordFile Path to file containing password
     * @return The resolved password, or null if both sources are null
     */
    fun resolvePassword(inlinePassword: String?, passwordFile: String?): String? {
        return when {
            inlinePassword != null -> resolveEnvVar(inlinePassword)
            passwordFile != null -> resolveFile(passwordFile)
            else -> null
        }
    }
    
    /**
     * Expands tilde (~) to user's home directory.
     * Only expands tilde at the start of the path.
     * 
     * @param path Path to expand
     * @return Expanded path, or null if input is null
     */
    fun expandPath(path: String?): String? {
        if (path == null) return null
        
        return if (path.startsWith("~/")) {
            homeDir + path.substring(1)
        } else {
            path
        }
    }
}
