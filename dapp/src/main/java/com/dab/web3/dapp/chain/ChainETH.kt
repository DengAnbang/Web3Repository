package com.dab.web3.dapp.chain

import com.dab.dapp.LogUtils
import com.dab.web3.dapp.Web3Config
import com.dab.web3.dapp.bean.BscResult
import com.dab.web3.dapp.bean.Web3Block
import com.dab.web3.dapp.bean.Web3Error
import com.dab.web3.dapp.bean.Web3KeyPair
import com.dab.web3.dapp.bean.Web3Result
import com.dab.web3.dapp.bean.Web3TransactionReceipt
import com.dab.web3.dapp.net.decodeQuantity
import com.dab.web3.dapp.net.toRequestBody
import com.google.gson.Gson
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Created by dab on 2023/2/9 16:23
 * https://github.com/satoshilabs/slips/blob/master/slip-0044.md
 */
open class ChainETH private constructor(private val url: String) : Web3Universal {
    companion object {
        val EthMainnet = ChainETH("https://eth-mainnet.g.alchemy.com/v2/JJ6qeYIRbhSwVao9eQajE3k9NvG0gAty")
//        val EthMainnet = ChainETH("https://eth-sepolia.g.alchemy.com/v2/")
        val EthSepolia = ChainETH("https://eth-sepolia.g.alchemy.com/v2/JJ6qeYIRbhSwVao9eQajE3k9NvG0gAty")
        val BscMainnet = ChainETH("https://bsc-dataseed.binance.org/")
        val BscTestnet = ChainETH("https://bsc-testnet-dataseed.bnbchain.org")

        //        val BscTestnet = ChainETH("https://data-seed-prebsc-1-s1.bnbchain.org:8545")
        fun custom(url: String) = ChainETH(url)
    }


    override val convertWei: Int
        get() = 18

    //    m/44'/60'/0'/0/0
    private val BIP44_BSC_ACCOUNT_ZERO_PATH = listOf<ChildNumber>(
        ChildNumber(44, true),
        ChildNumber(60, true),
        ChildNumber.ZERO_HARDENED,
        ChildNumber.ZERO,
        ChildNumber.ZERO,
    )
    private val jsonrpc = mapOf("jsonrpc" to "2.0")
    private val latest = "latest"
    override suspend fun getWeb3KeyPairByMnemonics(mnemonic: List<String>): Web3KeyPair {
        val seed = DeterministicSeed(mnemonic, null, "", 0L)
        val keyChain = DeterministicKeyChain.builder().seed(seed).build()
        val prvKeyBytes = keyChain.getKeyByPath(BIP44_BSC_ACCOUNT_ZERO_PATH, true).privKeyBytes
        val key = ECKey.fromPrivate(prvKeyBytes, false)
        val keyPair: ECKeyPair = ECKeyPair.create(key.privKey)
        val privateKey = "0x" + key.privKey.toString(16)
        return Web3KeyPair(privateKey, Keys.toChecksumAddress(Keys.getAddress(keyPair)))
    }

    override suspend fun getWeb3KeyPairByPrivateKey(privateKey: String): Web3KeyPair {
        val create = Credentials.create(privateKey)
        return Web3KeyPair(privateKey, Keys.toChecksumAddress(create.address))
    }

    private suspend fun getTransactionCount(address: String): Web3Result<BigInteger> {
        val mapOf = mapOf("method" to "eth_getTransactionCount", "id" to 1, "params" to listOf(address, latest))
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString().decodeQuantity(), null)
            }
            return Web3Result(BigInteger.ZERO, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigInteger.ZERO, Web3Error(-1, e.message))
        }

    }


    override suspend fun getBalance(address: String): Web3Result<BigInteger> {
        val mapOf = mapOf("method" to "eth_getBalance", "id" to 1, "params" to listOf(address, latest))
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString().decodeQuantity(), null)
            }
            return Web3Result(BigInteger.ZERO, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigInteger.ZERO, Web3Error(-1, e.message))
        }

    }


    override suspend fun transfer(privateKey: String, toAddress: String, value: BigDecimal): Web3Result<String> {
        try {
            val credentials = Credentials.create(privateKey)
            val transactionCount = getTransactionCount(credentials.address)
            if (transactionCount.error != null) {
                return Web3Result("", transactionCount.error)
            }
            val gasPrice = gasPrice()
            if (gasPrice.error != null) {
                return Web3Result("", gasPrice.error)
            }

            val rawTransaction = RawTransaction.createEtherTransaction(
                transactionCount.result, gasPrice.result, BigInteger.valueOf(21000),
                toAddress, value.multiply(BigDecimal.TEN.pow(convertWei)).toBigIntegerExact()
            )
            val signMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val toHexString = Numeric.toHexString(signMessage)
            val mapOf = mapOf("method" to "eth_sendRawTransaction", "id" to 1, "params" to listOf(toHexString))
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString(), null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }

    }

    override suspend fun gasPrice(): Web3Result<BigInteger> {
        val mapOf = mapOf("method" to "eth_gasPrice", "id" to 73)
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString().decodeQuantity(), null)
            }
            return Web3Result(BigInteger.ZERO, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigInteger.ZERO, Web3Error(-1, e.message))
        }
    }


    override suspend fun getTokenBalance(address: String, methodName: String, contractAddress: String): Web3Result<BigInteger> {
        val inputParameters = listOf(Address(address))
        val typeReference = object : TypeReference<Uint256>() {}
        val outputParameters = listOf(typeReference)
        val function = Function(methodName, inputParameters, outputParameters)
        val data = FunctionEncoder.encode(function)
        val mapOf = mapOf(
            "method" to "eth_call", "id" to 1, "params" to listOf(
                mapOf(
                    "from" to address,
                    "to" to contractAddress,
                    "gasPrice" to "",
                    "value" to "",
                    "data" to data,
                ), "latest"
            )
        )
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString().decodeQuantity(), null)
            }
            return Web3Result(BigInteger.ZERO, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(BigInteger.ZERO, Web3Error(-1, e.message))
        }

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
        try {
            val credentials = Credentials.create(privateKey)
            val transactionCount = getTransactionCount(credentials.address)
            if (transactionCount.error != null) {
                return Web3Result("", transactionCount.error)
            }
            val gasPrice = gasPrice()
            if (gasPrice.error != null) {
                return Web3Result("", gasPrice.error)
            }
            val inputParameters: ArrayList<Type<*>> = arrayListOf()
            if (inputFormAddress) {
                inputParameters.add(Address(credentials.address))
            }
            inputParameters.add(Address(toAddress))
            inputParameters.add(Uint256(value))
            val typeReference = object : TypeReference<Uint256>() {}
            val outputParameters = listOf(typeReference)
            val function = Function(methodName, inputParameters, outputParameters)
            val data = FunctionEncoder.encode(function)
            LogUtils.e(gasPrice.result)
            LogUtils.e(gasLimit)
            val rawTransaction = RawTransaction.createTransaction(
                transactionCount.result, gasPrice.result, gasLimit,
                contractAddress, null, data
            )
            val signMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
            val toHexString = Numeric.toHexString(signMessage)
            val mapOf = mapOf("method" to "eth_sendRawTransaction", "id" to 1, "params" to listOf(toHexString))
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString(), null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }
    }

    override suspend fun getTransactionReceipt(transactionHash: String): Web3Result<Web3TransactionReceipt?> {
        try {
            val mapOf = mapOf("method" to "eth_getTransactionReceipt", "id" to 1, "params" to listOf(transactionHash))
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), Web3TransactionReceipt::class.java)
                return Web3Result(fromJson, null)
            }
            return Web3Result(null, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(null, Web3Error(-1, e.message))
        }

    }

    override suspend fun getBlockByHash(blockHash: String): Web3Result<Web3Block?> {
        try {
            val mapOf = mapOf("method" to "eth_getBlockByHash", "id" to 1, "params" to listOf(blockHash, true))
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                val fromJson = Gson().fromJson(bscResult.result.toString(), Web3Block::class.java)
                return Web3Result(fromJson, null)
            }
            return Web3Result(null, bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result(null, Web3Error(-1, e.message))
        }
    }

    override suspend fun callContractMethod(
        address: String,
        inputParameters: List<Type<*>>,
        outputParameters: List<TypeReference<*>>,
        contractAddress: String,
        methodName: String
    ): Web3Result<String> {
        val function = Function(methodName, inputParameters, outputParameters)
        val data = FunctionEncoder.encode(function)
        val mapOf = mapOf(
            "method" to "eth_call", "id" to 1, "params" to listOf(
                mapOf(
                    "from" to address,
                    "to" to contractAddress,
//                    "gasPrice" to "",
//                    "value" to "",
                    "data" to data,
                ), "latest"
            )
        )
        try {
            val result = Web3Config.web3Service.rpcBody(url, (jsonrpc + mapOf).toRequestBody())
            val bscResult = Web3Config.gson.fromJson(result, BscResult::class.java)
            if (bscResult.error == null) {
                return Web3Result(bscResult.result.toString(), null)
            }
            return Web3Result("", bscResult.error)
        } catch (e: Exception) {
            e.printStackTrace()
            return Web3Result("", Web3Error(-1, e.message))
        }

    }


}