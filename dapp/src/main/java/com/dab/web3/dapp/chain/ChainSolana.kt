package com.dab.web3.dapp.chain

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
import org.bitcoinj.core.Base58
import org.bitcoinj.wallet.DeterministicSeed
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Hash
import java.math.BigDecimal
import java.math.BigInteger


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

    companion object{
        val Mainnet = ChainSolana("https://api.mainnet-beta.solana.com")
        val Devnet = ChainSolana("https://api.devnet.solana.com")
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


    override suspend fun transfer(privateKey: String, toAddress: String, value: BigDecimal): Web3Result<String> {
        TODO("Not yet implemented")
    }


    override suspend fun gasPrice(): Web3Result<BigInteger> {
        TODO("Not yet implemented")
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