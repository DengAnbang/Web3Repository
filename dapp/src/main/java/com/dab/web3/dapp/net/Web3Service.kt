package com.dab.web3.dapp.net

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.web3j.utils.Numeric
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Created by dab on 2022/8/12 16:58
 */
interface Web3Service {
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): ResponseBody

    @POST
    suspend fun  rpcBody(@Url url: String, @Body body: RequestBody, @HeaderMap header: Map<String, String> = mapOf()): JsonObject


}

fun Map<String, Any>.toRequestBody() =
    Gson().toJson(this).toRequestBody("application/json".toMediaTypeOrNull())

fun String.decodeQuantity() = Numeric.decodeQuantity(this)
