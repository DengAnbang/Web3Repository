package com.dab.web3.dapp.chain

import com.dab.web3.dapp.bean.Web3Block
import com.dab.web3.dapp.bean.Web3KeyPair
import com.dab.web3.dapp.bean.Web3Result
import com.dab.web3.dapp.bean.Web3TransactionReceipt
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Created by dab on 2023/2/9 16:18
 */
interface Web3Universal {

    /**
     * 通过助记词获取密钥对
     */
    suspend fun getWeb3KeyPairByMnemonics(mnemonic: List<String>): Web3KeyPair

    /**
     * 通过私钥获取密钥对
     */
    suspend fun getWeb3KeyPairByPrivateKey(privateKey: String): Web3KeyPair


    /**
     * 获取链上余额
     */
    suspend fun getBalance(address: String): Web3Result<BigInteger>

    /**
     * 获取wei
     */
    val convertWei: Int

    /**
     * 转移链上资产
     */
    suspend fun transfer(
        privateKey: String,
        toAddress: String,
        value: BigDecimal,
    ): Web3Result<String>

    /**
     * 获取gasPrice
     */
    suspend fun gasPrice(): Web3Result<BigInteger>{
        TODO("Not yet implemented")
    }

    /**
     * 获取代币的余额
     */
    suspend fun getTokenBalance(
        address: String,
        methodName: String,
        contractAddress: String,
    ): Web3Result<BigInteger>

    /**
     * 转移代币
     */
    suspend fun transferToken(
        privateKey: String,
        toAddress: String,
        contractAddress: String,
        methodName: String,
        value: BigInteger,
        gasLimit: BigInteger,
        inputFormAddress: Boolean,//是否添加自己的地址,部分合约需要,比如NFT,部分合约不需要,比如代币
    ): Web3Result<String>

    /**
     * 获取交易的回执
     */
    suspend fun getTransactionReceipt(transactionHash: String): Web3Result<Web3TransactionReceipt?>

    /**
     * 获取交易的的块
     */
    suspend fun getBlockByHash(blockHash: String): Web3Result<Web3Block?>

    /**
     * 调用合约的方法
     */
    suspend fun callContractMethod(
        address: String,
        inputParameters: List<Type<*>>,
        outputParameters: List<TypeReference<*>> = listOf(object : TypeReference<Uint256>() {}),
        contractAddress: String,
        methodName: String,
    ): Web3Result<String>

}