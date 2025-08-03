package com.market.iap

import android.os.Bundle
import android.os.IInterface

/**
 * Interface for in-app billing service
 * This interface defines the contract for in-app billing operations
 * that will be implemented by both CafeBazaar and Myket
 */
interface IInAppBillingService : IInterface {
    /**
     * Check if billing is supported
     * @param apiVersion The API version to check
     * @return Bundle containing the result
     */
    fun isBillingSupported(apiVersion: Int): Bundle

    /**
     * Get product details
     * @param apiVersion The API version
     * @param packageName The package name
     * @param skuList List of product IDs
     * @return Bundle containing product details
     */
    fun getSkuDetails(apiVersion: Int, packageName: String, skuList: Bundle): Bundle

    /**
     * Get purchases
     * @param apiVersion The API version
     * @param packageName The package name
     * @param itemType The item type (inapp or subs)
     * @param continuationToken Continuation token for pagination
     * @return Bundle containing purchase information
     */
    fun getPurchases(apiVersion: Int, packageName: String, itemType: String, continuationToken: String?): Bundle

    /**
     * Get purchase history
     * @param apiVersion The API version
     * @param packageName The package name
     * @param itemType The item type (inapp or subs)
     * @param continuationToken Continuation token for pagination
     * @return Bundle containing purchase history
     */
    fun getPurchaseHistory(apiVersion: Int, packageName: String, itemType: String, continuationToken: String?): Bundle

    /**
     * Consume a purchase
     * @param apiVersion The API version
     * @param packageName The package name
     * @param purchaseToken The purchase token
     * @return Bundle containing the result
     */
    fun consumePurchase(apiVersion: Int, packageName: String, purchaseToken: String): Bundle

    /**
     * Acknowledge a purchase
     * @param apiVersion The API version
     * @param packageName The package name
     * @param purchaseToken The purchase token
     * @param developerPayload The developer payload
     * @return Bundle containing the result
     */
    fun acknowledgePurchase(apiVersion: Int, packageName: String, purchaseToken: String, developerPayload: String): Bundle

    /**
     * Get buy intent
     * @param apiVersion The API version
     * @param packageName The package name
     * @param sku The product ID
     * @param itemType The item type (inapp or subs)
     * @param developerPayload The developer payload
     * @return Bundle containing the buy intent
     */
    fun getBuyIntent(apiVersion: Int, packageName: String, sku: String, itemType: String, developerPayload: String): Bundle

    /**
     * Stub class for the interface
     */
    abstract class Stub : android.os.Binder(), IInAppBillingService {
        companion object {
            fun asInterface(obj: android.os.IBinder?): IInAppBillingService? {
                return obj as? IInAppBillingService
            }
        }
    }
} 