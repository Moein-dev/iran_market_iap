package com.market.iap;

import android.os.Bundle;

/**
 * AIDL interface for in-app billing service
 * This interface defines the contract for in-app billing operations
 * that will be implemented by both CafeBazaar and Myket
 */
interface IInAppBillingService {
    /**
     * Check if billing is supported
     * @param apiVersion The API version to check
     * @return Bundle containing the result
     */
    Bundle isBillingSupported(int apiVersion);

    /**
     * Get product details
     * @param apiVersion The API version
     * @param packageName The package name
     * @param skuList List of product IDs
     * @return Bundle containing product details
     */
    Bundle getSkuDetails(int apiVersion, String packageName, Bundle skuList);

    /**
     * Get purchases
     * @param apiVersion The API version
     * @param packageName The package name
     * @param itemType The item type (inapp or subs)
     * @param continuationToken Continuation token for pagination
     * @return Bundle containing purchase information
     */
    Bundle getPurchases(int apiVersion, String packageName, String itemType, String continuationToken);

    /**
     * Get purchase history
     * @param apiVersion The API version
     * @param packageName The package name
     * @param itemType The item type (inapp or subs)
     * @param continuationToken Continuation token for pagination
     * @return Bundle containing purchase history
     */
    Bundle getPurchaseHistory(int apiVersion, String packageName, String itemType, String continuationToken);

    /**
     * Consume a purchase
     * @param apiVersion The API version
     * @param packageName The package name
     * @param purchaseToken The purchase token
     * @return Bundle containing the result
     */
    Bundle consumePurchase(int apiVersion, String packageName, String purchaseToken);

    /**
     * Acknowledge a purchase
     * @param apiVersion The API version
     * @param packageName The package name
     * @param purchaseToken The purchase token
     * @param developerPayload The developer payload
     * @return Bundle containing the result
     */
    Bundle acknowledgePurchase(int apiVersion, String packageName, String purchaseToken, String developerPayload);

    /**
     * Get buy intent
     * @param apiVersion The API version
     * @param packageName The package name
     * @param sku The product ID
     * @param itemType The item type (inapp or subs)
     * @param developerPayload The developer payload
     * @return Bundle containing the buy intent
     */
    Bundle getBuyIntent(int apiVersion, String packageName, String sku, String itemType, String developerPayload);
} 