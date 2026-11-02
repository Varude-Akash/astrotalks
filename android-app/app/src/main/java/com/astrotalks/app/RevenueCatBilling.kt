package com.astrotalks.app

import android.content.Context
import android.app.Activity
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback

/** RevenueCat is configured only after Supabase identifies the Google user. */
object RevenueCatBilling {
    fun configure(context: Context, appUserId: String) {
        if (BuildConfig.REVENUECAT_PUBLIC_KEY.isBlank()) return
        if (Purchases.isConfigured) return
        Purchases.configure(
            PurchasesConfiguration.Builder(context.applicationContext, BuildConfig.REVENUECAT_PUBLIC_KEY)
                .appUserID(appUserId)
                .build()
        )
    }

    fun purchase(activity: Activity, packageId: String, onResult: (Result<String>) -> Unit) {
        if (!Purchases.isConfigured) { onResult(Result.failure(IllegalStateException("Payments are not ready yet."))); return }
        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val pkg = offerings.current?.availablePackages?.firstOrNull { it.identifier == packageId }
                if (pkg == null) { onResult(Result.failure(IllegalStateException("This points package is unavailable."))); return }
                Purchases.sharedInstance.purchase(PurchaseParams.Builder(activity, pkg).build(), object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        onResult(Result.success(storeTransaction.productIds.firstOrNull() ?: packageId))
                    }
                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        onResult(Result.failure(IllegalStateException(if (userCancelled) "Purchase cancelled" else error.message)))
                    }
                })
            }
            override fun onError(error: PurchasesError) { onResult(Result.failure(IllegalStateException(error.message))) }
        })
    }
}
