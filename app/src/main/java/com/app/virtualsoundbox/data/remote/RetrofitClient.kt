package com.app.virtualsoundbox.data.remote

import com.app.virtualsoundbox.data.remote.api.SoundHoreeService // <--- INI SOLUSI ERRORNYA
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Ganti dengan IP Laptop Mas jika pakai HP asli (misal: "http://192.168.1.5:8080/api/v1/")
//    private const val BASE_URL = "http://192.168.100.3:8080/api/v1/"
    private const val BASE_URL = "https://algoritmakitadigital.id/api/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: SoundHoreeService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SoundHoreeService::class.java)
    }
}