package com.dab.web3.dapp

import com.dab.web3.dapp.net.Web3Service
import com.google.gson.Gson
import retrofit2.Retrofit

/**
 * Created by dab on 2022/8/24 18:25
 */
object Web3Config {
    val web3Service: Web3Service by lazy {
        val retrofit = setRetrofit()
        retrofit?.create(Web3Service::class.java) ?: throw NullPointerException("no setRetrofit")

    }
    val gson = Gson()


    var setRetrofit: (() -> Retrofit?) = { null }

}