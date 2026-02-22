package com.app.virtualsoundbox.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient

    // Menyimpan data harga asli dari Google Play (Bisa dipantau oleh UI)
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails = _productDetails.asStateFlow()

    // Memantau status pembayaran: Idle, Loading, Success, Error
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState = _purchaseState.asStateFlow()

    // ID Produk yang Mas buat di Play Console
    private val PREMIUM_ID = "sound_horee_premium_monthly"

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        connectToGooglePlay()
    }

    private fun connectToGooglePlay() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("AKD_BILLING", "Terhubung ke Google Play")
                    queryProducts()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Coba konek lagi kalau terputus
                connectToGooglePlay()
            }
        })
    }

    private fun queryProducts() {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                _productDetails.value = productDetailsList[0]
            }
        }
    }

    // Dipanggil saat tombol "Beli" diklik
    fun launchPurchaseFlow(activity: Activity) {
        val product = _productDetails.value
        if (product == null) {
            _purchaseState.value = PurchaseState.Error("Produk belum siap, coba lagi.")
            return
        }

        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    // Callback otomatis dari Google Play setelah pop-up pembayaran selesai
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                verifyAndAcknowledgePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseState.value = PurchaseState.Error("Pembelian dibatalkan")
        } else {
            _purchaseState.value = PurchaseState.Error("Error: ${billingResult.debugMessage}")
        }
    }

    private fun verifyAndAcknowledgePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // KEMBALIKAN TOKEN KE UI UNTUK DIKIRIM KE BACKEND
                        _purchaseState.value = PurchaseState.Success(
                            token = purchase.purchaseToken,
                            orderId = purchase.orderId ?: "TEST_ORDER"
                        )
                    }
                }
            } else {
                _purchaseState.value = PurchaseState.Success(
                    token = purchase.purchaseToken,
                    orderId = purchase.orderId ?: "TEST_ORDER"
                )
            }
        }
    }

    fun resetState() {
        _purchaseState.value = PurchaseState.Idle
    }

    sealed class PurchaseState {
        object Idle : PurchaseState()
        data class Success(val token: String, val orderId: String) : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }
}