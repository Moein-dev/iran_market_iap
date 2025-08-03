package com.market.iap

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Configuration manager for reading .env file and determining market settings
 */
object ConfigManager {
    private const val TAG = "ConfigManager"
    
    // Default values
    private const val DEFAULT_MARKET = "cafebazaar"
    private const val DEFAULT_DEBUG_LOGGING = true
    
    // Configuration keys
    private const val KEY_MARKET_TYPE = "MARKET_TYPE"
    private const val KEY_CAFEBAZAAR_RSA_KEY = "CAFEBAZAAR_RSA_KEY"
    private const val KEY_MYKET_RSA_KEY = "MYKET_RSA_KEY"
    private const val KEY_DEBUG_LOGGING = "DEBUG_LOGGING"
    
    private var config: Map<String, String> = emptyMap()
    
    /**
     * Initialize configuration from .env file
     */
    fun initialize(context: Context) {
        try {
            val inputStream = context.assets.open(".env")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val envConfig = mutableMapOf<String, String>()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim().removeSurrounding("\"")
                            envConfig[key] = value
                        }
                    }
                }
            }
            
            config = envConfig
            Log.d(TAG, "Configuration loaded: $config")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load .env file, using defaults: ${e.message}")
            config = emptyMap()
        }
    }
    
    /**
     * Get the market type from configuration
     */
    fun getMarketType(): String {
        return config[KEY_MARKET_TYPE] ?: DEFAULT_MARKET
    }
    
    /**
     * Get CafeBazaar RSA key
     */
    fun getCafeBazaarRsaKey(): String? {
        return config[KEY_CAFEBAZAAR_RSA_KEY]
    }
    
    /**
     * Get Myket RSA key
     */
    fun getMyketRsaKey(): String? {
        return config[KEY_MYKET_RSA_KEY]
    }
    
    /**
     * Get RSA key for current market
     */
    fun getRsaKeyForMarket(market: String): String? {
        return when (market.lowercase()) {
            "cafebazaar" -> getCafeBazaarRsaKey()
            "myket" -> getMyketRsaKey()
            else -> null
        }
    }
    
    /**
     * Check if debug logging is enabled
     */
    fun isDebugLoggingEnabled(): Boolean {
        return config[KEY_DEBUG_LOGGING]?.toBoolean() ?: DEFAULT_DEBUG_LOGGING
    }
    
    /**
     * Get all configuration
     */
    fun getConfig(): Map<String, String> {
        return config.toMap()
    }
} 