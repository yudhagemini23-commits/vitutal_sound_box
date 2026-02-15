package com.app.virtualsoundbox.data.remote.api

import com.app.virtualsoundbox.data.remote.model.LoginRequest
import com.app.virtualsoundbox.data.remote.model.AuthResponse
import com.app.virtualsoundbox.data.remote.model.TransactionDto
import com.app.virtualsoundbox.data.remote.model.UpgradeRequest
import com.app.virtualsoundbox.data.remote.model.UpgradeResponse
import retrofit2.Response
import retrofit2.http.*

interface SoundHoreeService {

    // Login / Register
    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<AuthResponse>

    // Sync Profile
    @POST("profile/sync")
    suspend fun syncProfile(
        @Header("Authorization") token: String,
        @Body profile: Map<String, Any>
    ): Response<Any>

    // Sync Transactions (Batch List)
    @POST("transactions/sync")
    suspend fun syncTransactions(
        @Header("Authorization") token: String,
        @Body transactions: List<TransactionDto>
    ): Response<Any>

    @GET("transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String,
        @Query("start") start: Long,
        @Query("end") end: Long
    ): Response<List<TransactionDto>>

    @POST("subscription/upgrade")
    suspend fun upgradeToPremium(
        @Header("Authorization") token: String,
        @Body request: UpgradeRequest
    ): Response<UpgradeResponse>
}