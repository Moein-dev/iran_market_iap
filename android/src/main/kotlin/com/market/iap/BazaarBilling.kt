package com.market.iap

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import org.json.JSONObject
import android.content.pm.PackageManager

/**
 * CafeBazaar billing implementation
 * Handles in-app billing operations for CafeBazaar market
 */
class BazaarBilling(private val context: Context) {
    companion object {
        private const val TAG = "BazaarBilling"
        private const val BAZAAR_PACKAGE = "com.farsitel.bazaar"
        private const val BAZAAR_BILLING_SERVICE = "com.farsitel.bazaar.service.InAppBillingService.BIND"
        private const val API_VERSION = 1
        private const val ITEM_TYPE_INAPP = "inapp"
        private const val ITEM_TYPE_SUBS = "subs"
    }

    private var billingService: IInAppBillingService? = null
    private var isConnected = false

    /**
     * Service connection for CafeBazaar billing service
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Bazaar billing service connected")
            billingService = IInAppBillingService.Stub.asInterface(service)
            isConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Bazaar billing service disconnected")
            billingService = null
            isConnected = false
        }
    }

    /**
     * Connect to CafeBazaar billing service
     */
    fun connect(): Boolean {
        return try {
            // First, check if the app is installed with more detailed logging
            Log.d(TAG, "Checking if CafeBazaar app is installed with package: $BAZAAR_PACKAGE")
            
            // Use a more reliable method to check if app is installed
            val appInstalled = try {
                // Try to get application info instead of package info
                val applicationInfo = context.packageManager.getApplicationInfo(BAZAAR_PACKAGE, 0)
                Log.d(TAG, "CafeBazaar app found: ${applicationInfo.packageName}")
                true
            } catch (e: Exception) {
                // If that fails, try to get package info
                try {
                    val packageInfo = context.packageManager.getPackageInfo(BAZAAR_PACKAGE, 0)
                    Log.d(TAG, "CafeBazaar app found via package info: ${packageInfo.packageName}")
                    true
                } catch (e2: Exception) {
                    Log.w(TAG, "CafeBazaar app not found: $e2")
                    false
                }
            }
            
            if (!appInstalled) {
                Log.w(TAG, "CafeBazaar app is not installed")
                return false
            }
            
            Log.d(TAG, "CafeBazaar app is installed, checking for billing services...")
            
            // List all services in the app for debugging
            try {
                val packageInfo = context.packageManager.getPackageInfo(BAZAAR_PACKAGE, PackageManager.GET_SERVICES)
                Log.d(TAG, "Available services in CafeBazaar:")
                packageInfo.services?.forEach { service ->
                    Log.d(TAG, "  - ${service.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not list services: $e")
            }
            
            // Try different service names
            val serviceNames = listOf(
                "com.farsitel.bazaar.service.InAppBillingService.BIND",
                "com.farsitel.bazaar.service.BillingService.BIND",
                "com.farsitel.bazaar.service.IInAppBillingService.BIND",
                "com.farsitel.bazaar.billing.InAppBillingService.BIND"
            )
            
            for (serviceName in serviceNames) {
                Log.d(TAG, "Trying service: $serviceName")
                val intent = Intent(serviceName)
                intent.setPackage(BAZAAR_PACKAGE)
                
                // Check if the service is available
                val resolveInfo = context.packageManager.resolveService(intent, 0)
                if (resolveInfo != null) {
                    Log.d(TAG, "Found service: $serviceName")
                    val result = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                    Log.d(TAG, "CafeBazaar billing service bind result: $result")
                    return result
                } else {
                    Log.d(TAG, "Service not found: $serviceName")
                }
            }
            
            Log.w(TAG, "CafeBazaar app is installed but billing service not found")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Bazaar billing service", e)
            return false
        }
    }

    /**
     * Disconnect from CafeBazaar billing service
     */
    fun disconnect() {
        try {
            if (isConnected) {
                context.unbindService(serviceConnection)
                isConnected = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect from Bazaar billing service", e)
        }
    }

    /**
     * Check if billing is supported
     */
    fun isBillingSupported(): Boolean {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return false
            }

            val response = billingService?.isBillingSupported(API_VERSION)
            response?.getInt("RESPONSE_CODE") == 0
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to check billing support", e)
            false
        }
    }

    /**
     * Get product details
     */
    fun getSkuDetails(productIds: List<String>): List<Map<String, Any>> {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return emptyList()
            }

            val skuList = Bundle().apply {
                putStringArrayList("ITEM_ID_LIST", ArrayList(productIds))
            }

            val response = billingService?.getSkuDetails(API_VERSION, context.packageName, skuList)
            val responseCode = response?.getInt("RESPONSE_CODE") ?: -1

            if (responseCode == 0) {
                val skuDetailsList = response?.getStringArrayList("DETAILS_LIST") ?: emptyList()
                skuDetailsList.mapNotNull { skuDetails ->
                    try {
                        val json = JSONObject(skuDetails)
                        mapOf<String, Any>(
                            "productId" to json.getString("productId"),
                            "title" to json.getString("title"),
                            "description" to json.getString("description"),
                            "price" to json.getString("price"),
                            "priceAmountMicros" to json.getString("price_amount_micros"),
                            "priceCurrencyCode" to json.getString("price_currency_code")
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse SKU details", e)
                        null
                    }
                }
            } else {
                Log.e(TAG, "Failed to get SKU details, response code: $responseCode")
                emptyList()
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get SKU details", e)
            emptyList()
        }
    }

    /**
     * Get purchases
     */
    fun getPurchases(): List<Map<String, Any>> {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return emptyList()
            }

            val response = billingService?.getPurchases(API_VERSION, context.packageName, ITEM_TYPE_INAPP, null)
            val responseCode = response?.getInt("RESPONSE_CODE") ?: -1

            if (responseCode == 0) {
                val purchaseDataList = response?.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: emptyList()
                val signatureList = response?.getStringArrayList("INAPP_DATA_SIGNATURE_LIST") ?: emptyList()

                purchaseDataList.mapIndexedNotNull { index, purchaseData ->
                    try {
                        val json = JSONObject(purchaseData)
                        mapOf<String, Any>(
                            "productId" to json.getString("productId"),
                            "purchaseToken" to json.getString("purchaseToken"),
                            "orderId" to json.getString("orderId"),
                            "purchaseTime" to json.getString("purchaseTime"),
                            "developerPayload" to json.optString("developerPayload", ""),
                            "isAutoRenewing" to json.optBoolean("autoRenewing", false),
                            "originalJson" to purchaseData,
                            "signature" to if (index < signatureList.size) signatureList[index] else ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse purchase data", e)
                        null
                    }
                }
            } else {
                Log.e(TAG, "Failed to get purchases, response code: $responseCode")
                emptyList()
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get purchases", e)
            emptyList()
        }
    }

    /**
     * Get purchase history
     */
    fun getPurchaseHistory(): List<Map<String, Any>> {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return emptyList()
            }

            val response = billingService?.getPurchaseHistory(API_VERSION, context.packageName, ITEM_TYPE_INAPP, null)
            val responseCode = response?.getInt("RESPONSE_CODE") ?: -1

            if (responseCode == 0) {
                val purchaseDataList = response?.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: emptyList()
                val signatureList = response?.getStringArrayList("INAPP_DATA_SIGNATURE_LIST") ?: emptyList()

                purchaseDataList.mapIndexedNotNull { index, purchaseData ->
                    try {
                        val json = JSONObject(purchaseData)
                        mapOf<String, Any>(
                            "productId" to json.getString("productId"),
                            "purchaseToken" to json.getString("purchaseToken"),
                            "orderId" to json.getString("orderId"),
                            "purchaseTime" to json.getString("purchaseTime"),
                            "developerPayload" to json.optString("developerPayload", ""),
                            "isAutoRenewing" to json.optBoolean("autoRenewing", false),
                            "originalJson" to purchaseData,
                            "signature" to if (index < signatureList.size) signatureList[index] else ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse purchase history", e)
                        null
                    }
                }
            } else {
                Log.e(TAG, "Failed to get purchase history, response code: $responseCode")
                emptyList()
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get purchase history", e)
            emptyList()
        }
    }

    /**
     * Consume a purchase
     */
    fun consumePurchase(purchaseToken: String): Boolean {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return false
            }

            val response = billingService?.consumePurchase(API_VERSION, context.packageName, purchaseToken)
            val responseCode = response?.getInt("RESPONSE_CODE") ?: -1

            if (responseCode == 0) {
                Log.d(TAG, "Purchase consumed successfully")
                true
            } else {
                Log.e(TAG, "Failed to consume purchase, response code: $responseCode")
                false
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to consume purchase", e)
            false
        }
    }

    /**
     * Acknowledge a purchase
     */
    fun acknowledgePurchase(purchaseToken: String, developerPayload: String? = null): Boolean {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return false
            }

            val response = billingService?.acknowledgePurchase(
                API_VERSION,
                context.packageName,
                purchaseToken,
                developerPayload ?: ""
            )
            val responseCode = response?.getInt("RESPONSE_CODE") ?: -1

            if (responseCode == 0) {
                Log.d(TAG, "Purchase acknowledged successfully")
                true
            } else {
                Log.e(TAG, "Failed to acknowledge purchase, response code: $responseCode")
                false
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to acknowledge purchase", e)
            false
        }
    }

    /**
     * Get buy intent for a product
     */
    fun getBuyIntent(productId: String, developerPayload: String? = null): Map<String, Any>? {
        return try {
            if (!isConnected) {
                Log.w(TAG, "Bazaar billing service not connected")
                return null
            }

            val response = billingService?.getBuyIntent(
                API_VERSION,
                context.packageName,
                productId,
                ITEM_TYPE_INAPP,
                developerPayload ?: ""
            )
            val responseCode = response?.getInt("RESPONSE_CODE") ?: -1

            if (responseCode == 0) {
                val pendingIntent = response?.getParcelable<android.app.PendingIntent>("BUY_INTENT")
                mapOf<String, Any>(
                    "pendingIntent" to (pendingIntent ?: ""),
                    "responseCode" to responseCode
                )
            } else {
                Log.e(TAG, "Failed to get buy intent, response code: $responseCode")
                null
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get buy intent", e)
            null
        }
    }
} 