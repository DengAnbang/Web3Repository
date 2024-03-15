package com.dab.dapp

import android.app.Application
import com.dab.web3.dapp.Web3Config
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by dab on 2023/3/15 15:15
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Web3Config.setRetrofit = {
            Retrofit.Builder()
                .baseUrl("https://www.bing.com/")
                .addConverterFactory(GsonConverterFactory.create(Gson())) //用于Json数据的转换,非必须
                .client(getOkHttpClient)
                .build()
        }

    }


    val getOkHttpClient by lazy {
        getOkHttpClient()
    }

    private fun getOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient().newBuilder()
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(30, TimeUnit.SECONDS)

//        val logInterceptor = HttpLoggingInterceptor() {
//

//        }

//        logInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val logInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                if (message.startsWith("{")
                    || message.startsWith("--> GET")
                    || message.startsWith("--> POST")
                    || message.startsWith("<-- 200")
                ) {
                    LogUtils.e("OkHttp----->$message")
                } else {
                    LogUtils.d("OkHttp----->$message")
                }
            }

        })
        logInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addNetworkInterceptor(logInterceptor)
        return builder.build()
    }


}