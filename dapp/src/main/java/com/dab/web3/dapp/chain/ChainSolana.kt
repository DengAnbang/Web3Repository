package com.dab.web3.dapp.chain

import android.os.Build
import androidx.annotation.RequiresApi
import com.dab.dapp.LogUtils
import com.dab.dapp.mainAddressSolana
import com.dab.dapp.mainPrivateSolana
import com.dab.web3.dapp.Web3Config
import com.dab.web3.dapp.bean.BscResult
import com.dab.web3.dapp.bean.Web3Block
import com.dab.web3.dapp.bean.Web3Error
import com.dab.web3.dapp.bean.Web3KeyPair
import com.dab.web3.dapp.bean.Web3Result
import com.dab.web3.dapp.bean.Web3TransactionReceipt
import com.dab.web3.dapp.net.toRequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.solana.Solana
import com.solana.actions.sendSPLTokens
import com.solana.actions.serializeAndSendWithFee
import com.solana.api.getRecentBlockhash
import com.solana.core.Account
import com.solana.core.HotAccount
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.networking.HttpNetworkingRouter
import com.solana.networking.RPCEndpoint
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.TokenProgram
import okio.ByteString.Companion.toByteString
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.wallet.DeterministicSeed
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Hash
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom


/**
 * Created by dab on 2023/8/9 10:53
 * https://github.com/satoshilabs/slips/blob/master/slip-0010.md
 *
 *
 *
 * 主网  https://api.mainnet-beta.solana.com
 * 测试  https://api.testnet.solana.com
 * 开发  https://api.devnet.solana.com
 */
open class ChainSolana private constructor(private val url: String) : Web3Universal {
    /** Address of the SPL Token program */
    val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"

    /** Address of the SPL Token 2022 program */
    val TOKEN_2022_PROGRAM_ID = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"

    /** Address of the SPL Associated Token Account program */
    val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

    /** Address of the special mint for wrapped native SOL in spl-token */
    val NATIVE_MINT = "So11111111111111111111111111111111111111112"

    /** Address of the special mint for wrapped native SOL in spl-token-2022 */
    val NATIVE_MINT_2022 = "9pan9bMn5HatX4EJdBwg9VgCa7Uz5HL8N1m5D3NdXejP"

    companion object {
        val Mainnet = ChainSolana("https://api.mainnet-beta.solana.com")
        val Devnet = ChainSolana("https://api.devnet.solana.com")
        val Testnet = ChainSolana("https://api.testnet.solana.com")
        fun custom(url: String) = ChainSolana(url)
    }

    override val convertWei: Int
        get() = 9
    private val jsonrpc = mapOf("jsonrpc" to "2.0")


    override suspend fun getWeb3KeyPairByMnemonics(mnemonic: List<String>): Web3KeyPair {
        val seed = DeterministicSeed(mnemonic, null, "", 0L)
        val masterKeyI = getMasterKeyI(seed.seedBytes!!)
        //    m/44'/501'/0'/0'/0'
        val deduceI = deduceI(masterKeyI, 44, true)
        val coinTypeI = deduceI(deduceI, 501, true)
        val accountI = deduceI(coinTypeI, 0, true)
        val changeI = deduceI(accountI, 0, true)
        val ed25519PrivateKeyParameters = Ed25519PrivateKeyParameters(changeI.take(32).toByteArray())
        return Web3KeyPair(
            Base58.encode(ed25519PrivateKeyParameters.encoded + ed25519PrivateKeyParameters.generatePublicKey().encoded),
            Base58.encode(ed25519PrivateKeyParameters.generatePublicKey().encoded)
        )
    }

    override suspend fun getWeb3KeyPairByPrivateKey(privateKey: String): Web3KeyPair {
        val ed25519PrivateKeyParameters = Ed25519PrivateKeyParameters(Base58.decode(privateKey).take(32).toByteArray())
        return Web3KeyPair(
            Base58.encode(ed25519PrivateKeyParameters.encoded + ed25519PrivateKeyParameters.generatePublicKey().encoded),
            Base58.encode(ed25519PrivateKeyParameters.generatePublicKey().encoded)
        )

    }

    override suspend fun getBalance(address: String): Web3Result<BigInteger> {
        val mapOf = mapOf("method" to "getBalance", "id" to 1, "params" to listOf(address))
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), JsonObject::class.java)
                return Web3Result(fromJson.get("value").toString().toBigDecimal().toBigInteger(), null)
            }
            return Web3Result(BigInteger.ZERO, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigInteger.ZERO, Web3Error(-1, e.message))
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun transfer(privateKey: String, toAddress: String, value: BigDecimal): Web3Result<String> {
        val web3KeyPairByPrivateKey = getWeb3KeyPairByPrivateKey(privateKey)
        val amount = value.multiply(BigDecimal.TEN.pow(convertWei)).toLong()

        // 4 byte instruction index + 8 bytes lamports
        val data = ByteArray(4 + 8)
        Utils.uint32ToByteArrayLE(2, data, 0)
        Utils.int64ToByteArrayLE(amount, data, 4)


        val accountKeys = listOf(web3KeyPairByPrivateKey.address, toAddress, "11111111111111111111111111111111")
        val signature = signature(privateKey, accountKeys, data)
        val base64Trx: String = org.bouncycastle.util.encoders.Base64.toBase64String(signature)
        val mapOf = mapOf(
            "method" to "sendTransaction",
            "id" to 1,
            "params" to listOf(base64Trx, mapOf("encoding" to "base64", "skipPreflight" to false, "preflightCommitment" to "finalized")),
        )
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), JsonObject::class.java)
                return Web3Result(fromJson.get("value").asJsonObject.get("blockhash").toString(), null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }

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

    open suspend fun signature(privateKey: String, accountKeys: List<String>, data: ByteArray): ByteArray {

        val recentBlockhash = getLatestBlockHash().result
        val buffer = ByteBuffer.allocate(2048)
        buffer.put(1.toByte())//numRequiredSignatures
        buffer.put(0.toByte())//numReadonlySignedAccounts
        buffer.put(1.toByte())//numReadonlyUnsignedAccounts
        buffer.put(accountKeys.count().toByte())//keyCount
        for (accountKey in accountKeys) {
            buffer.put(Base58.decode(accountKey))
        }
        buffer.put(Base58.decode(recentBlockhash))//blockHash
        buffer.put(1.toByte())//instructionCount
        buffer.put(2.toByte())//programIdIndex
        buffer.put((accountKeys.count() - 1).toByte())//keyIndicesCount
        buffer.put(byteArrayOf(0.toByte(), 1.toByte()))//keyIndices
        val b = data.count().toByte()
        buffer.put(b)//dataLength
        buffer.put(data)//data
        buffer.flip()
        val serialize = buffer.toByteString().toByteArray()
        val signature = ed25519Sign(privateKey, serialize)

        val signatureCount = 1.toByte()
        val transactionLength = 1 + 1 * 64 + serialize.count()

        val wireTransaction = ByteBuffer.allocate(transactionLength)
        wireTransaction.put(signatureCount)
        wireTransaction.put(signature)
        wireTransaction.put(serialize)
        wireTransaction.flip()
        return wireTransaction.toByteString().toByteArray()
    }


    open suspend fun createTokenAccount(privateKey: String, tokenAddr: String, programId: String = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA") {
        val minimumBalanceForRentExemption = getMinimumBalanceForRentExemption()
        val data = ByteArray(4 + 8 + 8 + 32)
        //  const val PROGRAM_INDEX_CREATE_ACCOUNT = 0
        //    const val PROGRAM_INDEX_TRANSFER = 2
        Utils.uint32ToByteArrayLE(0.toLong(), data, 0)
        Utils.int64ToByteArrayLE(minimumBalanceForRentExemption.result.toLong(), data, 4)
        Utils.int64ToByteArrayLE(165L, data, 12)//REQUIRED_ACCOUNT_SPACE
        System.arraycopy(programId.toByteArray(), 0, data, 20, 32)


    }


    open suspend fun getLatestBlockHash(): Web3Result<String> {
        val mapOf = mapOf("method" to "getLatestBlockhash", "id" to 1, "params" to listOf(mapOf("commitment" to "finalized")))
//        val mapOf = mapOf("method" to "getRecentBlockhash", "id" to 1, "params" to listOf(mapOf("commitment" to "processed")))
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), JsonObject::class.java)
                return Web3Result(fromJson.get("value").asJsonObject.get("blockhash").asString, null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }
    }

    open suspend fun getMinimumBalanceForRentExemption(): Web3Result<BigDecimal> {
        val mapOf = mapOf("method" to "getMinimumBalanceForRentExemption", "id" to 1, "params" to listOf(165))
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), JsonObject::class.java)
                val toBigInteger = result.get("result").toString().toBigDecimal()
                return Web3Result(toBigInteger, null)
            }
            return Web3Result(BigDecimal.ZERO, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigDecimal.ZERO, Web3Error(-1, e.message))
        }
    }

    open suspend fun getTokenSupply(token: String): Web3Result<String> {
        val mapOf = mapOf("method" to "getTokenSupply", "id" to 1, "params" to listOf(token))
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), JsonObject::class.java)
                return Web3Result(fromJson.get("value").toString(), null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }
    }

    open suspend fun getRecentPrioritizationFees(blockHash: String): Web3Result<String> {
        val mapOf = mapOf("method" to "getRecentPrioritizationFees", "id" to 1, "params" to listOf<String>())
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), JsonObject::class.java)
                return Web3Result(fromJson.get("value").toString(), null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }
    }


//    @TestOnly
//    suspend fun requestAirdrop(address: String): Web3Result<String> {
//        val mapOf = mapOf("method" to "requestAirdrop", "id" to 1, "params" to listOf(address, 10_0000_0000))
//        try {
//            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
//            return Web3Result(result.toString(), null)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return Web3Result("", Web3Error(-1, e.message))
//        }
//    }


    suspend fun test() {

        try {
            val walletAddress = mainAddressSolana
            val tokenMintAddress = "55Asf49xJdnogZbSLMTEwsWfJb46xMHTymm3xrH4yLbT"
            val arrayListOf = arrayListOf(Base58.decode(walletAddress))
            val associatedTokenAddress = PublicKey.associatedTokenAddress(PublicKey(walletAddress), PublicKey(tokenMintAddress))
            val associatedProgramDerivedAddress = associatedTokenAddress.address
            LogUtils.e(associatedTokenAddress)
            arrayListOf.add(Base58.decode(TOKEN_PROGRAM_ID))
            arrayListOf.add(Base58.decode(tokenMintAddress))
            var nonce = 255
            while (nonce != 0) {
                try {
                    arrayListOf.add(byteArrayOf(nonce.toByte()))
                    val buffer = ByteArrayOutputStream()
                    for (seed in arrayListOf) {
                        require(arrayListOf.size <= 32) { "Max seed length exceeded" }
                        try {
                            buffer.write(seed)
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                    }
                    buffer.write(Base58.decode(ASSOCIATED_TOKEN_PROGRAM_ID))
                    buffer.write("ProgramDerivedAddress".toByteArray())
                    val hash = Sha256Hash.hash(buffer.toByteArray())

                    val ed25519Signer = Ed25519Signer()
                    val bytes = ByteArray(64)
                    SecureRandom().nextBytes(bytes)
                    ed25519Signer.update(bytes, 0, bytes.size)
                    val verifySignature = ed25519Signer.verifySignature(byteArrayOf())


                    LogUtils.e(verifySignature.toString() + "::" + Base58.encode(hash))
                    LogUtils.e(nonce.toString() + "::" + Base58.encode(hash))
                    if (verifySignature) {
                        break
                    } else {
                        arrayListOf.removeAt(arrayListOf.size - 1)
                        nonce--
                    }

//                     PublicKey.createProgramAddress(arrayListOf, programId)
                } catch (e: Exception) {
                    arrayListOf.removeAt(arrayListOf.size - 1)
                    nonce--
                    continue
                }

            }



            LogUtils.e("initResult")

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    suspend fun test1() {
        val walletPublicKey = mainAddressSolana
        val walletPrivateKey = mainPrivateSolana
        val mintAddress = mainAddressSolana
        val ownerAddress = mainAddressSolana

        try {
//
            val solana = Solana(HttpNetworkingRouter(RPCEndpoint.devnetSolana))


            val hotAccount = HotAccount(Base58.decode(walletPrivateKey))

//            val orNull = solana.action.sendSPLTokens(
//                PublicKey("55Asf49xJdnogZbSLMTEwsWfJb46xMHTymm3xrH4yLbT"),
//                PublicKey(walletPublicKey),
//                PublicKey(mintAddress),
//                10,
//                false,
//                HotAccount(Base58.decode(walletPrivateKey))
//            ).getOrNull()




            val transaction = Transaction()
            val sendInstruction = TokenProgram.transfer(PublicKey(walletPublicKey), PublicKey(mintAddress), 10, hotAccount.publicKey)
            transaction.add(sendInstruction)



            transaction.setRecentBlockHash(getLatestBlockHash().result)
            transaction.sign(hotAccount)
            val serialized = transaction.serialize()

            val base64Trx = org.bouncycastle.util.encoders.Base64.toBase64String(serialized)
            val mapOf = mapOf(
                "method" to "sendTransaction",
                "id" to 1,
                "params" to listOf(base64Trx, mapOf("encoding" to "base64", "skipPreflight" to false, "preflightCommitment" to "finalized")),
            )
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)



            LogUtils.e(bscResult)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    suspend fun test2() {
        val walletPublicKey = mainAddressSolana
        val walletPrivateKey = mainPrivateSolana
        val mintAddress = mainAddressSolana
        val ownerAddress = mainAddressSolana
        val web3KeyPairByPrivateKey = getWeb3KeyPairByPrivateKey(walletPrivateKey)

        try {
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    // 使用签名器进行签名
    private fun ed25519Sign(privateKey: String, byteArray: ByteArray): ByteArray {
        val ed25519PrivateKeyParameters = Ed25519PrivateKeyParameters(Base58.decode(privateKey).take(32).toByteArray())
        val signer = Ed25519Signer()
        signer.init(true, ed25519PrivateKeyParameters)
        signer.update(byteArray, 0, byteArray.size)
        val generateSignature = signer.generateSignature()
        LogUtils.e(Base58.encode(generateSignature))
        return generateSignature
    }

    /**
     * serialize a 32-bit unsigned integer i as a 4-byte sequence
     */
    private fun ser32(l: Long) = byteArrayOf((l shr 24).toByte(), (l shr 16).toByte(), (l shr 8).toByte(), (l).toByte())

    /**
     *需要将 I 拆分为两个 32 字节序列IL和IR
     * IL作为主密钥
     * IR作为链码
     */
    private fun getMasterKeyI(seed: ByteArray): ByteArray {
        val I = Hash.hmacSha512("ed25519 seed".toByteArray(), seed)
        val IL = BigInteger(1, I.take(32).toByteArray())
        if (IL.compareTo(BigInteger.ZERO) == 0 || IL > CustomNamedCurves.getByName("secp256k1").n) {
            return getMasterKeyI(I)
        }
        return I
    }

    /**
     * 推导下一层
     * Private parent key → private child key
    Let n denote the order of the curve.

    The function CKDpriv((kpar, cpar), i) → (ki, ci) computes a child extended private key from the parent extended private key:

    Check whether i ≥ 231 (whether the child is a hardened key).
    If so (hardened child): let I = HMAC-SHA512(Key = cpar, Data = 0x00 || ser256(kpar) || ser32(i)). (Note: The 0x00 pads the private key to make it 33 bytes long.)
    If not (normal child):
    If curve is ed25519: return failure.
    let I = HMAC-SHA512(Key = cpar, Data = serP(point(kpar)) || ser32(i)).
    Split I into two 32-byte sequences, IL and IR.
    The returned chain code ci is IR.
    If curve is ed25519: The returned child key ki is parse256(IL).
    If parse256(IL) ≥ n or parse256(IL) + kpar (mod n) = 0 (resulting key is invalid):
    let I = HMAC-SHA512(Key = cpar, Data = 0x01 || IR || ser32(i) and restart at step 2.
    Otherwise: The returned child key ki is parse256(IL) + kpar (mod n).
     */
    private fun deduceI(parentI: ByteArray, child: Long, isHardened: Boolean): ByteArray {
        val parentIL = parentI.take(32).toByteArray()
        val parentIR = parentI.takeLast(32).toByteArray()
        val byteArrayOf = byteArrayOf(0) + parentIL + ser32((if (isHardened) 0x80000000 else 0L) + child)
        val I = Hash.hmacSha512(parentIR, byteArrayOf)
        return I
    }
}