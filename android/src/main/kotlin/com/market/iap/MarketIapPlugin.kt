package com.market.iap

import android.app.Activity
import android.content.Intent
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Main plugin class for Market IAP
 * Handles platform channel communication and routes calls to appropriate billing implementation
 */
class MarketIapPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    companion object {
        private const val TAG = "MarketIapPlugin"
        private const val CHANNEL_NAME = "market_iap"
        private const val REQUEST_CODE_PURCHASE = 1001
    }

    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var bazaarBilling: BazaarBilling? = null
    private var myketBilling: MyketBilling? = null
    private var currentMarket: String? = null
    private var rsaPublicKey: String? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "init" -> handleInit(call, result)
            "isBillingSupported" -> handleIsBillingSupported(call, result)
            "getProducts" -> handleGetProducts(call, result)
            "purchase" -> handlePurchase(call, result)
            "consume" -> handleConsume(call, result)
            "getPurchases" -> handleGetPurchases(call, result)
            "getPurchaseHistory" -> handleGetPurchaseHistory(call, result)
            "acknowledgePurchase" -> handleAcknowledgePurchase(call, result)
            "verifyPurchaseSignature" -> handleVerifyPurchaseSignature(call, result)
            else -> result.notImplemented()
        }
    }

    /**
     * Handle initialization
     */
    private fun handleInit(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: "cafebazaar"
            currentMarket = market

            // Check if activity is available
            if (activity == null) {
                Log.w(TAG, "Activity is null during initialization")
                result.success(false)
                return
            }

            when (market) {
                "cafebazaar" -> {
                    bazaarBilling = BazaarBilling(activity!!)
                    val connected = bazaarBilling?.connect() ?: false
                    Log.d(TAG, "CafeBazaar billing initialized: $connected")
                    result.success(connected)
                }
                "myket" -> {
                    myketBilling = MyketBilling(activity!!)
                    val connected = myketBilling?.connect() ?: false
                    Log.d(TAG, "Myket billing initialized: $connected")
                    result.success(connected)
                }
                else -> {
                    Log.e(TAG, "Unsupported market: $market")
                    result.success(false)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            result.success(false)
        }
    }

    /**
     * Handle billing support check
     */
    private fun handleIsBillingSupported(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val isSupported = when (market) {
                "cafebazaar" -> bazaarBilling?.isBillingSupported() ?: false
                "myket" -> myketBilling?.isBillingSupported() ?: false
                else -> false
            }
            result.success(isSupported)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check billing support", e)
            result.success(false)
        }
    }

    /**
     * Handle get products
     */
    private fun handleGetProducts(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val productIds = call.argument<List<String>>("productIds") ?: emptyList()

            val products = when (market) {
                "cafebazaar" -> bazaarBilling?.getSkuDetails(productIds) ?: emptyList()
                "myket" -> myketBilling?.getSkuDetails(productIds) ?: emptyList()
                else -> emptyList()
            }
            result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get products", e)
            result.success(emptyList<Map<String, Any>>())
        }
    }

    /**
     * Handle purchase
     */
    private fun handlePurchase(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val productId = call.argument<String>("productId") ?: ""
            val developerPayload = call.argument<String>("developerPayload")
            val rsaConfig = call.argument<Map<String, Any>>("rsaConfig")

            // Set RSA key if provided
            if (rsaConfig != null) {
                rsaPublicKey = rsaConfig["publicKey"] as? String
            }

            val buyIntent = when (market) {
                "cafebazaar" -> bazaarBilling?.getBuyIntent(productId, developerPayload)
                "myket" -> myketBilling?.getBuyIntent(productId, developerPayload)
                else -> null
            }

            if (buyIntent != null) {
                val pendingIntent = buyIntent["pendingIntent"] as? android.app.PendingIntent
                if (pendingIntent != null && activity != null) {
                    activity!!.startIntentSenderForResult(
                        pendingIntent.intentSender,
                        REQUEST_CODE_PURCHASE,
                        null,
                        0,
                        0,
                        0
                    )
                    // Store the result callback to handle the purchase result
                    // This is a simplified implementation - in a real app you'd need to handle the result properly
                    result.success(null)
                } else {
                    result.success(null)
                }
            } else {
                result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purchase", e)
            result.success(null)
        }
    }

    /**
     * Handle consume purchase
     */
    private fun handleConsume(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val purchaseToken = call.argument<String>("purchaseToken") ?: ""

            val success = when (market) {
                "cafebazaar" -> bazaarBilling?.consumePurchase(purchaseToken) ?: false
                "myket" -> myketBilling?.consumePurchase(purchaseToken) ?: false
                else -> false
            }
            result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to consume purchase", e)
            result.success(false)
        }
    }

    /**
     * Handle get purchases
     */
    private fun handleGetPurchases(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val rsaConfig = call.argument<Map<String, Any>>("rsaConfig")

            // Set RSA key if provided
            if (rsaConfig != null) {
                rsaPublicKey = rsaConfig["publicKey"] as? String
            }

            val purchases = when (market) {
                "cafebazaar" -> bazaarBilling?.getPurchases() ?: emptyList()
                "myket" -> myketBilling?.getPurchases() ?: emptyList()
                else -> emptyList()
            }

            // Verify signatures if RSA key is available
            val verifiedPurchases = if (rsaPublicKey != null) {
                purchases.mapNotNull { purchase ->
                    val signature = purchase["signature"] as? String
                    if (signature != null && verifySignature(purchase["originalJson"] as String, signature)) {
                        purchase
                    } else {
                        Log.w(TAG, "Purchase signature verification failed")
                        null
                    }
                }
            } else {
                purchases
            }

            result.success(verifiedPurchases)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get purchases", e)
            result.success(emptyList<Map<String, Any>>())
        }
    }

    /**
     * Handle get purchase history
     */
    private fun handleGetPurchaseHistory(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val rsaConfig = call.argument<Map<String, Any>>("rsaConfig")

            // Set RSA key if provided
            if (rsaConfig != null) {
                rsaPublicKey = rsaConfig["publicKey"] as? String
            }

            val purchaseHistory = when (market) {
                "cafebazaar" -> bazaarBilling?.getPurchaseHistory() ?: emptyList()
                "myket" -> myketBilling?.getPurchaseHistory() ?: emptyList()
                else -> emptyList()
            }

            // Verify signatures if RSA key is available
            val verifiedHistory = if (rsaPublicKey != null) {
                purchaseHistory.mapNotNull { purchase ->
                    val signature = purchase["signature"] as? String
                    if (signature != null && verifySignature(purchase["originalJson"] as String, signature)) {
                        purchase
                    } else {
                        Log.w(TAG, "Purchase history signature verification failed")
                        null
                    }
                }
            } else {
                purchaseHistory
            }

            result.success(verifiedHistory)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get purchase history", e)
            result.success(emptyList<Map<String, Any>>())
        }
    }

    /**
     * Handle acknowledge purchase
     */
    private fun handleAcknowledgePurchase(call: MethodCall, result: Result) {
        try {
            val market = call.argument<String>("market") ?: currentMarket ?: "cafebazaar"
            val purchaseToken = call.argument<String>("purchaseToken") ?: ""
            val developerPayload = call.argument<String>("developerPayload")

            val success = when (market) {
                "cafebazaar" -> bazaarBilling?.acknowledgePurchase(purchaseToken, developerPayload) ?: false
                "myket" -> myketBilling?.acknowledgePurchase(purchaseToken, developerPayload) ?: false
                else -> false
            }
            result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acknowledge purchase", e)
            result.success(false)
        }
    }

    /**
     * Handle verify purchase signature
     */
    private fun handleVerifyPurchaseSignature(call: MethodCall, result: Result) {
        try {
            val purchaseData = call.argument<Map<String, Any>>("purchaseData")
            val rsaConfig = call.argument<Map<String, Any>>("rsaConfig")

            if (purchaseData == null) {
                result.success(false)
                return
            }

            val originalJson = purchaseData["originalJson"] as? String
            val signature = purchaseData["signature"] as? String

            if (originalJson == null || signature == null) {
                result.success(false)
                return
            }

            // Set RSA key if provided
            if (rsaConfig != null) {
                rsaPublicKey = rsaConfig["publicKey"] as? String
            }

            val isValid = if (rsaPublicKey != null) {
                verifySignature(originalJson, signature)
            } else {
                Log.w(TAG, "No RSA public key provided for signature verification")
                false
            }

            result.success(isValid)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify purchase signature", e)
            result.success(false)
        }
    }

    /**
     * Verify purchase signature using RSA public key
     */
    private fun verifySignature(data: String, signature: String): Boolean {
        return try {
            val currentRsaKey = rsaPublicKey
            if (currentRsaKey.isNullOrEmpty()) {
                Log.w(TAG, "RSA public key is not set")
                return false
            }

            Log.d(TAG, "Verifying signature with RSA key length: ${currentRsaKey.length}")
            Log.d(TAG, "Data length: ${data.length}, Signature length: ${signature.length}")

            // Decode the public key
            val keyBytes = Base64.getDecoder().decode(currentRsaKey)
            Log.d(TAG, "Decoded key bytes length: ${keyBytes.size}")
            
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec)

            // Create signature verifier
            val signatureVerifier = Signature.getInstance("SHA256withRSA")
            signatureVerifier.initVerify(publicKey)
            signatureVerifier.update(data.toByteArray())

            // Decode the signature
            val signatureBytes = Base64.getDecoder().decode(signature)
            Log.d(TAG, "Decoded signature bytes length: ${signatureBytes.size}")

            // Verify the signature
            val isValid = signatureVerifier.verify(signatureBytes)
            Log.d(TAG, "Signature verification result: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify signature", e)
            Log.e(TAG, "RSA key: ${rsaPublicKey?.take(50)}...")
            Log.e(TAG, "Data: ${data.take(100)}...")
            Log.e(TAG, "Signature: ${signature.take(50)}...")
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_PURCHASE) {
            // Handle purchase result
            // This is a simplified implementation - in a real app you'd need to handle the result properly
            Log.d(TAG, "Purchase result: $resultCode")
            return true
        }
        return false
    }
} 