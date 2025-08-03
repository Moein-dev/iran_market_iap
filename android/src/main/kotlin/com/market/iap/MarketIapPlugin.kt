package com.market.iap

import android.app.Activity
import android.content.Context
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import androidx.annotation.NonNull

/**
 * MarketIapPlugin
 * A Flutter plugin for in-app purchases supporting Myket and CafeBazaar markets
 * Following the same patterns as the official plugins
 */
class MarketIapPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var activity: Activity? = null
    
    // Billing implementations
    private var cafeBazaarBilling: CafeBazaarBilling? = null
    private var myketBilling: MyketBilling? = null
    private var currentMarket: String? = null
    
    companion object {
        private const val TAG = "MarketIapPlugin"
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.flutterEngine.dartExecutor, "market_iap")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        
        // Initialize configuration
        ConfigManager.initialize(context)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "init" -> handleInit(call, result)
            "connect" -> handleConnect(call, result)
            "disconnect" -> handleDisconnect(call, result)
            "purchase" -> handlePurchase(call, result)
            "consume" -> handleConsume(call, result)
            "getPurchases" -> handleGetPurchases(call, result)
            "getSkuDetails" -> handleGetSkuDetails(call, result)
            "isBillingSupported" -> handleIsBillingSupported(call, result)
            else -> result.notImplemented()
        }
    }

    private fun handleInit(call: MethodCall, result: Result) {
        try {
            // Get market type from .env file or use provided parameter
            val market = call.argument<String>("market") ?: ConfigManager.getMarketType()
            currentMarket = market
            
            val rsaKey = call.argument<String>("rsaKey") ?: ConfigManager.getRsaKeyForMarket(market)
            val enableDebugLogging = call.argument<Boolean>("enableDebugLogging") ?: ConfigManager.isDebugLoggingEnabled()

            Log.d(TAG, "Initializing Market IAP with market: $market")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    if (activity == null) {
                        Log.e(TAG, "Activity is null during initialization")
                        result.success(mapOf(
                            "success" to false,
                            "error" to "Activity is null"
                        ))
                        return
                    }
                    
                    cafeBazaarBilling = CafeBazaarBilling(activity!!)
                    val initialized = cafeBazaarBilling?.initialize(rsaKey, enableDebugLogging) ?: false
                    
                    if (initialized) {
                        Log.d(TAG, "CafeBazaar billing initialized successfully")
                        result.success(mapOf(
                            "success" to true,
                            "market" to market,
                            "message" to "CafeBazaar initialized successfully"
                        ))
                    } else {
                        Log.e(TAG, "Failed to initialize CafeBazaar billing")
                        result.success(mapOf(
                            "success" to false,
                            "error" to "Failed to initialize CafeBazaar billing",
                            "market" to market
                        ))
                    }
                }
                "myket" -> {
                    if (activity == null) {
                        Log.e(TAG, "Activity is null during initialization")
                        result.success(mapOf(
                            "success" to false,
                            "error" to "Activity is null"
                        ))
                        return
                    }
                    
                    myketBilling = MyketBilling(activity!!)
                    val initialized = myketBilling?.initialize(rsaKey, enableDebugLogging) ?: false
                    
                    if (initialized) {
                        Log.d(TAG, "Myket billing initialized successfully")
                        result.success(mapOf(
                            "success" to true,
                            "market" to market,
                            "message" to "Myket initialized successfully"
                        ))
                    } else {
                        Log.e(TAG, "Failed to initialize Myket billing")
                        result.success(mapOf(
                            "success" to false,
                            "error" to "Failed to initialize Myket billing",
                            "market" to market
                        ))
                    }
                }
                else -> {
                    Log.e(TAG, "Unsupported market: $market")
                    result.success(mapOf(
                        "success" to false,
                        "error" to "Unsupported market: $market"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handleConnect(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()
            
            Log.d(TAG, "Connecting to market: $market")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    cafeBazaarBilling?.connect { connected ->
                        if (connected) {
                            Log.d(TAG, "CafeBazaar connected successfully")
                            result.success(mapOf(
                                "success" to true,
                                "market" to market,
                                "state" to "connected"
                            ))
                        } else {
                            Log.e(TAG, "CafeBazaar connection failed")
                            result.success(mapOf(
                                "success" to false,
                                "error" to "CafeBazaar connection failed",
                                "market" to market
                            ))
                        }
                    }
                }
                "myket" -> {
                    myketBilling?.connect { connected ->
                        if (connected) {
                            Log.d(TAG, "Myket connected successfully")
                            result.success(mapOf(
                                "success" to true,
                                "market" to market,
                                "state" to "connected"
                            ))
                        } else {
                            Log.e(TAG, "Myket connection failed")
                            result.success(mapOf(
                                "success" to false,
                                "error" to "Myket connection failed",
                                "market" to market
                            ))
                        }
                    }
                }
                else -> {
                    result.success(mapOf(
                        "success" to false,
                        "error" to "Unsupported market: $market"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handleDisconnect(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()
            
            Log.d(TAG, "Disconnecting from market: $market")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    cafeBazaarBilling?.disconnect()
                    Log.d(TAG, "CafeBazaar disconnected")
                }
                "myket" -> {
                    myketBilling?.disconnect()
                    Log.d(TAG, "Myket disconnected")
                }
            }
            
            result.success(mapOf(
                "success" to true,
                "market" to market
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handlePurchase(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()
            val productId = call.argument<String>("productId") ?: ""
            val developerPayload = call.argument<String>("developerPayload")

            Log.d(TAG, "Purchase request for product: $productId in market: $market")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    cafeBazaarBilling?.purchase(productId, developerPayload) { response ->
                        result.success(response)
                    }
                }
                "myket" -> {
                    myketBilling?.purchase(productId, developerPayload) { response ->
                        result.success(response)
                    }
                }
                else -> {
                    result.success(mapOf(
                        "success" to false,
                        "error" to "Unsupported market: $market"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purchase", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handleConsume(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()
            val purchaseToken = call.argument<String>("purchaseToken") ?: ""

            Log.d(TAG, "Consume request for purchase token: $purchaseToken in market: $market")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    cafeBazaarBilling?.consume(purchaseToken) { response ->
                        result.success(response)
                    }
                }
                "myket" -> {
                    myketBilling?.consume(purchaseToken) { response ->
                        result.success(response)
                    }
                }
                else -> {
                    result.success(mapOf(
                        "success" to false,
                        "error" to "Unsupported market: $market"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to consume", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handleGetPurchases(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()

            Log.d(TAG, "Get purchases request for market: $market")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    cafeBazaarBilling?.getPurchases { response ->
                        result.success(response)
                    }
                }
                "myket" -> {
                    myketBilling?.getPurchases { response ->
                        result.success(response)
                    }
                }
                else -> {
                    result.success(mapOf(
                        "success" to false,
                        "error" to "Unsupported market: $market"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get purchases", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handleGetSkuDetails(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()
            val skuIds = call.argument<List<String>>("skuIds") ?: emptyList()

            Log.d(TAG, "Get SKU details request for market: $market, SKUs: $skuIds")

            when (market.lowercase()) {
                "cafebazaar" -> {
                    cafeBazaarBilling?.getSkuDetails(skuIds) { response ->
                        result.success(response)
                    }
                }
                "myket" -> {
                    myketBilling?.getSkuDetails(skuIds) { response ->
                        result.success(response)
                    }
                }
                else -> {
                    result.success(mapOf(
                        "success" to false,
                        "error" to "Unsupported market: $market"
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SKU details", e)
            result.success(mapOf(
                "success" to false,
                "error" to e.message
            ))
        }
    }

    private fun handleIsBillingSupported(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: ConfigManager.getMarketType()

            Log.d(TAG, "Billing supported check for market: $market")

            val isSupported = when (market.lowercase()) {
                "cafebazaar" -> cafeBazaarBilling?.isBillingSupported() ?: false
                "myket" -> myketBilling?.isBillingSupported() ?: false
                else -> false
            }
            
            result.success(isSupported)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check billing support", e)
            result.success(false)
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        // Clean up billing connections
        cafeBazaarBilling?.disconnect()
        myketBilling?.disconnect()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
} 