package com.dab.web3.dapp

import com.dab.web3.dapp.bean.Web3Block
import com.dab.web3.dapp.bean.Web3KeyPair
import com.dab.web3.dapp.bean.Web3Result
import com.dab.web3.dapp.bean.Web3TransactionReceipt
import com.dab.web3.dapp.chain.Web3Universal
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigDecimal
import java.math.BigInteger


/**
 * Created by dab on 2022/8/23 17:12
 */

class Web3jUtils private constructor() {
    lateinit var web3Universal: Web3Universal

    companion object {
        private val instance: Web3jUtils by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Web3jUtils()
        }

        fun get(web3Universal: Web3Universal): Web3jUtils {
            instance.web3Universal = web3Universal
            return instance
        }
    }

    val convertWei get() = web3Universal.convertWei
    suspend fun getWeb3KeyPairByMnemonics(mnemonic: List<String>): Web3KeyPair {
        return web3Universal.getWeb3KeyPairByMnemonics(mnemonic)
    }

    suspend fun getWeb3KeyPairByPrivateKey(privateKey: String): Web3KeyPair {
        return web3Universal.getWeb3KeyPairByPrivateKey(privateKey)
    }

    suspend fun getBalance(address: String): Web3Result<BigInteger> {
        return web3Universal.getBalance(address)
    }

    suspend fun transfer(privateKey: String, toAddress: String, value: BigDecimal): Web3Result<String> {
        return web3Universal.transfer(privateKey, toAddress, value)
    }

    suspend fun gasPrice(): Web3Result<BigInteger> {
        return web3Universal.gasPrice()
    }

    suspend fun getTokenBalance(
        address: String,
        methodName: String,
        contractAddress: String,
    ): Web3Result<BigInteger> {
        return web3Universal.getTokenBalance(address, methodName, contractAddress)
    }

    suspend fun transferToken(
        privateKey: String,
        toAddress: String,
        contractAddress: String,
        methodName: String,
        value: BigInteger,
        gasLimit: BigInteger
    ): Web3Result<String> {
        return web3Universal.transferToken(
            privateKey = privateKey,
            toAddress = toAddress,
            contractAddress = contractAddress,
            methodName = methodName,
            value = value,
            gasLimit = gasLimit,
            inputFormAddress = false
        )
    }

    suspend fun getTransactionReceipt(transactionHash: String): Web3Result<Web3TransactionReceipt?> {
        return web3Universal.getTransactionReceipt(transactionHash)
    }

    suspend fun getBlockByHash(blockHash: String): Web3Result<Web3Block?> {
        return web3Universal.getBlockByHash(blockHash)
    }

    suspend fun callContractMethod(
        address: String,
        inputParameters: List<Type<*>>,
        outputParameters: List<TypeReference<*>>,
        contractAddress: String,
        methodName: String
    ): Web3Result<String> {
        return web3Universal.callContractMethod(address, inputParameters, outputParameters, contractAddress, methodName)
    }


    ///////////////////////////////////////以下是业务方法
    //获取皮肤的Id
    suspend fun getTokenIdByOwnerIndex(
        address: String,
        index: Long,
        contractAddress: String,
    ): Web3Result<String> {
        return web3Universal.callContractMethod(
            address,
            listOf(Address(address), Uint256(index)),
            contractAddress = contractAddress,
            methodName = "tokenOfOwnerByIndex",
        )

    }

    //转移皮肤
    suspend fun transferSkin(
        privateKey: String,
        toAddress: String,
        tokenId: BigInteger,
        contractAddress: String,
    ): Web3Result<String> {
        return web3Universal.transferToken(
            privateKey = privateKey,
            toAddress = toAddress,
            gasLimit = BigInteger("170000"),
            contractAddress = contractAddress,
            methodName = "safeTransferFrom",
            value = tokenId,
            inputFormAddress = true,
        )
    }


}

