// BillingManager.kt
package com.example.mojaparafia.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.example.mojaparafia.db.DatabaseInstance
import kotlinx.coroutines.*

class BillingManager private constructor(context: Context) {
    private val billingClient: BillingClient
    private val database = DatabaseInstance.getDatabase()
    private val dao = database.subscriptionDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium

    private val _subscriptionStatus = MutableLiveData(SubscriptionStatus.CHECKING)
    val subscriptionStatus: LiveData<SubscriptionStatus> = _subscriptionStatus

    private val _productDetails = MutableLiveData<ProductDetails?>()
    val productDetails: LiveData<ProductDetails?> = _productDetails

    // Callback po udanym zakupie świeczki
    var onCandlePurchaseSuccess: (() -> Unit)? = null

    interface BillingManagerListener {
        fun onPurchaseAcknowledged()
        fun onPurchaseError(error: String?)
    }

    private var listener: BillingManagerListener? = null
    fun setListener(listener: BillingManagerListener?) { this.listener = listener }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                // ROZGAŁĘZIENIE LOGIKI: Sprawdzamy czy to świeczka, czy subskrypcja
                if (purchase.products.contains(SKU_CANDLE)) {
                    handleCandlePurchase(purchase)
                } else {
                    handlePurchase(purchase) // Logika subskrypcji
                }
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            listener?.onPurchaseError("Anulowano zakup.")
        } else {
            listener?.onPurchaseError("Błąd zakupu. Kod: ${billingResult.responseCode}")
        }
    }

    init {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .enableAutoServiceReconnection() // Nowość w 8.x - automatyczne wznawianie
            .build()

        connectToGooglePlay()

        scope.launch {
            val status = dao.getStatus()
            val isFull = status?.isPremium ?: false
            _isPremium.postValue(isFull)
            _subscriptionStatus.postValue(if (isFull) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d("BillingManager", "Połączono z Google Play (v8.3.0)")
                    queryPurchasesAsync()
                    queryProductDetails()
                } else {
                    Log.e("BillingManager", "Błąd połączenia: ${billingResult.responseCode}")
                    _subscriptionStatus.postValue(SubscriptionStatus.NON_PREMIUM)
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.d("BillingManager", "Rozłączono z Google Play")
            }
        })
    }

    fun queryPurchasesAsync() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                var hasPremium = false
                var token: String? = null
                purchases.forEach { purchase ->
                    if (purchase.products.contains(SKU_REMOVE_ADS) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        hasPremium = true
                        token = purchase.purchaseToken
                        if (!purchase.isAcknowledged) handlePurchase(purchase)
                    }
                }
                updateLocalStatus(hasPremium, token)
            }
        }
    }

    private fun updateLocalStatus(hasPremium: Boolean, token: String?) {
        scope.launch {
            dao.insert(SubscriptionEntity(isPremium = hasPremium, purchaseToken = token))
            _isPremium.postValue(hasPremium)
            _subscriptionStatus.postValue(if (hasPremium) SubscriptionStatus.PREMIUM else SubscriptionStatus.NON_PREMIUM)
        }
    }

    // NAPRAWIONE DLA WERSJI 8.3.0
    fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_REMOVE_ADS)
                .setProductType(ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val code = billingResult.responseCode
            val list = queryProductDetailsResult.productDetailsList // To rozwiązuje błąd 'get' i 'isNotEmpty'

            // Logowanie produktów, których nie udało się pobrać (unfetched)
            queryProductDetailsResult.unfetchedProductList?.forEach { unfetched ->
                Log.e("BillingManager", "Produkt niepobrany: ${unfetched.productId}")
            }

            if (code == BillingResponseCode.OK && list.isNotEmpty()) {
                val product = list.find { it.productId == SKU_REMOVE_ADS }
                if (product != null) {
                    _productDetails.postValue(product)
                    Log.d("BillingManager", "Pobrano szczegóły dla Mojej Parafii: ${product.productId}")
                }
            } else {
                Log.e("BillingManager", "Błąd lub pusta lista: $code. Message: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, planId: String) {
        val offerDetails = productDetails.subscriptionOfferDetails?.find { it.basePlanId == planId }

        if (offerDetails == null) {
            listener?.onPurchaseError("Nie znaleziono planu: $planId")
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerDetails.offerToken)
                    .build()
            )).build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    // LOGIKA DLA SUBSKRYPCJI
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingResponseCode.OK) {
                    updateLocalStatus(true, purchase.purchaseToken)
                    listener?.onPurchaseAcknowledged()
                }
            }
        } else if (purchase.isAcknowledged) {
            updateLocalStatus(true, purchase.purchaseToken)
        }
    }

    // =========================================================
    // NOWE: LOGIKA DLA JEDNORAZOWEGO ZAKUPU ŚWIECZKI (INAPP)
    // =========================================================

    fun buyCandle(activity: Activity, onSuccess: () -> Unit) {
        this.onCandlePurchaseSuccess = onSuccess

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_CANDLE) // upewnij się że to "candle-24h"
                .setProductType(ProductType.INAPP) // WAŻNE: INAPP
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
            val list = queryProductDetailsResult.productDetailsList

            if (billingResult.responseCode == BillingResponseCode.OK && !list.isNullOrEmpty()) {
                val productDetails = list[0]

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                // WYMUSZENIE WĄTKU GŁÓWNEGO DLA OKNA GOOGLE PLAY
                activity.runOnUiThread {
                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
            } else {
                // DIAGNOSTYKA NA EKRANIE
                activity.runOnUiThread {
                    val reason = if (list.isNullOrEmpty()) "Pusta lista od Google (produkt jeszcze nieaktywny w konsoli?)" else "Błąd kodu: ${billingResult.responseCode}"
                    Toast.makeText(activity, "Błąd sklepu: $reason", Toast.LENGTH_LONG).show()
                }
                Log.e("BillingManager", "Błąd pobierania produktu świecy: ${billingResult.debugMessage}. Lista pusta? ${list.isNullOrEmpty()}")
            }
        }
    }

    // Konsumpcja produktu, aby można go było kupić ponownie w przyszłości
    private fun handleCandlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d("BillingManager", "Świeczka pomyślnie zakupiona i skonsumowana.")
                    // Zakup zatwierdzony i skonsumowany - wywołujemy akcję z Compose
                    onCandlePurchaseSuccess?.invoke()
                    onCandlePurchaseSuccess = null
                } else {
                    Log.e("BillingManager", "Błąd konsumpcji świeczki: ${billingResult.debugMessage}")
                    listener?.onPurchaseError("Wystąpił błąd podczas zatwierdzania zakupu świecy.")
                }
            }
        }
    }

    companion object {
        @Volatile private var INSTANCE: BillingManager? = null
        fun getInstance(context: Context): BillingManager = INSTANCE ?: synchronized(this) {
            INSTANCE ?: BillingManager(context).also { INSTANCE = it }
        }

        // STAŁE DLA MOJEJ PARAFII
        const val SKU_REMOVE_ADS = "remove_ads_monthly"
        const val PLAN_MONTHLY = "1-plan-miesieczny"
        const val PLAN_YEARLY = "wylaczenie-reklam-rok"

        // NOWY IDENTYFIKATOR DLA ŚWIECY
        const val SKU_CANDLE = "candle_24h"
    }
}