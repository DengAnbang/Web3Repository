package com.dab.web3.dapp.bean

/**
 * Created by dab on 2023/7/31 17:28
 */



data class BscResult(val jsonrpc: String, val id: Int, val result: Any?, val error: Web3Error?)



data class TronResult(val address: String, val balance: String, val Error: String?) {

}

data class Web3Error(val code: Int, val message: String?)

data class Web3Result<T>(val result: T, val error: Web3Error?)
data class Web3KeyPair(val privateKey: String, val address: String)
data class Web3TransactionReceipt(
    val transactionHash: String,
    val blockHash: String,
    val gasUsed: String,
    val effectiveGasPrice: String,
    val status: String
)

data class Web3Block(
    val timestamp: String,
)
