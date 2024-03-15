package com.dab.web3.dapp.chain

import android.util.Log
import com.dab.web3.dapp.Web3Config
import com.dab.web3.dapp.bean.TronResult
import com.dab.web3.dapp.bean.Web3Block
import com.dab.web3.dapp.bean.Web3Error
import com.dab.web3.dapp.bean.Web3KeyPair
import com.dab.web3.dapp.bean.Web3Result
import com.dab.web3.dapp.bean.Web3TransactionReceipt
import com.dab.web3.dapp.net.toRequestBody
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.bitcoinj.core.Base58
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.bouncycastle.util.encoders.Hex
import org.jetbrains.annotations.TestOnly
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Created by dab on 2023/8/9 10:53
 * 主网	        https://api.trongrid.io
 * Shasta 	    https://api.shasta.trongrid.io
 * Nile 	    https://nile.trongrid.io
 */
open class ChainTron private constructor(private val url: String) : Web3Universal {
    companion object {
        val Mainnet = ChainTron("https://api.trongrid.io")
        val Nile = ChainTron("https://nile.trongrid.io")
        fun custom(url: String) = ChainTron(url)
    }

    override val convertWei: Int
        get() = 6
    private val jsonrpc = mapOf("jsonrpc" to "2.0")
    private val trxKey = "e82b598f-4fb4-4260-b018-062de31d2fa1"
    private val api = "/wallet/"

    //    m/44'/195'/0'/0'/0
    val BIP44_TRX_ACCOUNT = listOf<ChildNumber>(
        ChildNumber(44, true),
        ChildNumber(195, true),
        ChildNumber.ZERO_HARDENED,
        ChildNumber.ZERO,
        ChildNumber.ZERO,
    )

    override suspend fun getWeb3KeyPairByMnemonics(mnemonic: List<String>): Web3KeyPair {
        val seed = DeterministicSeed(mnemonic, null, "", 0L)
        val keyChain = DeterministicKeyChain.builder().seed(seed).build()
        val prvKeyBytes = keyChain.getKeyByPath(BIP44_TRX_ACCOUNT, true).privKeyBytes
        val key = ECKey.fromPrivate(prvKeyBytes, false)
        return Web3KeyPair(key.privKey.toString(16), getAddress(key.pubKey))
    }

    override suspend fun getWeb3KeyPairByPrivateKey(privateKey: String): Web3KeyPair {
        val key = ECKey.fromPrivate(Numeric.toBigInt(privateKey), false)
        return Web3KeyPair(key.privKey.toString(16), getAddress(key.pubKey))
    }


    override suspend fun getBalance(address: String): Web3Result<BigInteger> {
        val mapOf = mapOf("address" to address, "visible" to true)
        try {
            val method = "getaccount"
            val result = Web3Config.web3Service.rpcBody(url + api + method, body = mapOf.toRequestBody(), header = mapOf("TRON-PRO-API-KEY" to trxKey))

            val tronResult = Web3Config.gson.fromJson(result, TronResult::class.java)
            if (tronResult.Error != null) {
                return Web3Result(BigInteger.ZERO, Web3Error(-1, tronResult.Error))
            }
            if (tronResult.balance.isNullOrEmpty()) {
                return Web3Result(BigInteger.ZERO, null)
            }
            return Web3Result(tronResult.balance.toBigInteger(), null)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigInteger.ZERO, Web3Error(-1, e.message))
        }
    }


    override suspend fun transfer(privateKey: String, toAddress: String, value: BigDecimal): Web3Result<String> {
        val keyPair = getWeb3KeyPairByPrivateKey(privateKey)
        try {
            val createTransaction = createTransaction(keyPair.address, toAddress, value)
            return signatureAndBroadcast(privateKey, createTransaction)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }
    }

    suspend fun createTransaction(formAddress: String, toAddress: String, value: BigDecimal): JsonObject {
        val mapOf =
            mapOf("owner_address" to formAddress, "to_address" to toAddress, "amount" to value.multiply(BigDecimal.TEN.pow(convertWei)).toBigIntegerExact(), "visible" to true)
        val method = "createtransaction"
        val result = Web3Config.web3Service.rpcBody(url + api + method, body = mapOf.toRequestBody(), header = mapOf("TRON-PRO-API-KEY" to trxKey))
        val jsonObject = Web3Config.gson.fromJson(result, JsonObject::class.java)
        return jsonObject
    }

    suspend fun signatureAndBroadcast(privateKey: String, createTransaction: JsonObject): Web3Result<String> {
        val signMessage = Sign.signMessage(Hash.sha256(Hex.decode(createTransaction.get("raw_data_hex").asString)), ECKeyPair.create(Numeric.toBigInt(privateKey)), false)
        val signature = Hex.toHexString(signMessage.r) + Hex.toHexString(signMessage.s) + Hex.toHexString(signMessage.v)
        val jsonArray = JsonArray()
        jsonArray.add(signature)
        createTransaction.add("signature", jsonArray)
        val broadcast = Web3Config.web3Service.rpcBody(
            url + api + "broadcasttransaction",
            body = Gson().toJson(createTransaction).toRequestBody("application/json".toMediaTypeOrNull()),
            header = mapOf("TRON-PRO-API-KEY" to trxKey)
        )
        val broadcastObject = Web3Config.gson.fromJson(broadcast, JsonObject::class.java)
        if (broadcastObject.get("Error") != null) {
            return Web3Result("", Web3Error(-1, broadcastObject.get("Error").asString))
        }
        return Web3Result(broadcastObject["txid"].toString(), null)
    }

    override suspend fun gasPrice(): Web3Result<BigInteger> {
        TODO("Tron not gasPrice , use fee(String,Int)")
    }

    /**
     * https://developers.tron.network/docs/faq
     */
    fun fee(raw_data_hex: String, signatureSize: Int = 1): Int {
        val DATA_HEX_PROTOBUF_EXTRA = 3
        val MAX_RESULT_SIZE_IN_TX = 64
        val A_SIGNATURE = 67
        var len = raw_data_hex.length / 2 + DATA_HEX_PROTOBUF_EXTRA + MAX_RESULT_SIZE_IN_TX
        for (i in 0 until signatureSize) {
            len += A_SIGNATURE
        }
        return len
    }

    override suspend fun getTokenBalance(address: String, methodName: String, contractAddress: String): Web3Result<BigInteger> {
        TODO("Not yet implemented")
    }

    override suspend fun transferToken(
        privateKey: String,
        toAddress: String,
        contractAddress: String,
        methodName: String,
        value: BigInteger,
        gasLimit: BigInteger,
        inputFormAddress: Boolean
    ): Web3Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getTransactionReceipt(transactionHash: String): Web3Result<Web3TransactionReceipt?> {
        TODO("Not yet implemented")
    }

    override suspend fun getBlockByHash(blockHash: String): Web3Result<Web3Block?> {
        TODO("Not yet implemented")
    }

    override suspend fun callContractMethod(
        address: String,
        inputParameters: List<Type<*>>,
        outputParameters: List<TypeReference<*>>,
        contractAddress: String,
        methodName: String
    ): Web3Result<String> {
        TODO("Not yet implemented")
    }


    @TestOnly
    suspend fun requestAirdrop(address: String): Web3Result<String> {
        TODO("Not yet implemented")
    }

    /**
     * https://tronprotocol.github.io/documentation-zh/mechanism-algorithm/account/
     * 1.用公钥P作为输入，计算SHA3得到结果H, 这里公钥长度为64字节，SHA3选用Keccak256。
     * 2.取H的最后20字节，在前面填充一个字节0x41得到address。
     * 3.对address进行basecheck计算得到最终地址，所有地址的第一个字符为T。
     * 其中basecheck的计算过程为：
     * 1.首先对address计算sha256得到h1，
     * 2.再对h1计算sha256得到h2，
     * 3.取其前4字节作为check填充到address之后得到address||check，
     * 1.对其进行base58编码得到最终结果。
     */
    private fun getAddress(pubKey: ByteArray): String {
        //这里公钥长度为65字节,非压缩格式公钥首字节为0x04,取后64字节
        val pubKeySha3 = Hash.sha3(pubKey.takeLast(64).toByteArray())
        val address = byteArrayOf(0x41.toByte()) + pubKeySha3.slice(pubKeySha3.size - 20 until pubKeySha3.size)
        val baseCheck = Hash.sha256(Hash.sha256(address))
        val addressCheck = address + baseCheck.slice(0 until 4).toByteArray()
        return Base58.encode(addressCheck)
    }

    private fun String.base58ToHex() = Hex.toHexString(Base58.decode(this)).take(42)

//    private fun String.hexToBase58() = Base58.encode(BigInteger(this,16).toByteArray())
}