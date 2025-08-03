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
        try {
            val intent = Intent(BAZAAR_BILLING_SERVICE)
            intent.setPackage(BAZAAR_PACKAGE)
            return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
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
                        mapOf(
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
                        mapOf(
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
                        mapOf(
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
                mapOf(
                    "pendingIntent" to pendingIntent,
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