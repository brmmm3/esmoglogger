package com.wakeup.esmoglogger

import android.content.Context
import com.wakeup.esmoglogger.ui.cloud.getOkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class User(val id: String, val name: String)

object ApiClient {
    private const val BASE_URL = "https://your-api-server.com/"
    fun create(context: Context): ApiService {
        val okHttpClient = getOkHttpClient(context) // Your SSL-configured OkHttpClient
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }
}
