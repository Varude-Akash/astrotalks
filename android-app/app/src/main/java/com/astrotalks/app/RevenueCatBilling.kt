package com.astrotalks.app

import android.content.Context
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

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
}
