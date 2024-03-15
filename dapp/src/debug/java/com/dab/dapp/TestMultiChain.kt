package com.dab.dapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dab.web3.dapp.Web3jUtils
import com.dab.web3.dapp.chain.ChainETH
import com.dab.web3.dapp.chain.ChainSolana
import com.dab.web3.dapp.chain.ChainTron
import com.dab.web3.dapp.toConvertWei
import kotlinx.coroutines.launch
import org.web3j.utils.Convert
import java.math.BigDecimal


/**
 * Created by dab on 2023/8/22 10:29
 */
@Composable
fun TestMultiChain(type: MutableState<Int>) {
    val web3jUtils = remember {

        when (type.value) {
            1 -> Web3jUtils.get(ChainETH.EthSepolia)
            2 -> Web3jUtils.get(ChainETH.BscTestnet)
            3 -> Web3jUtils.get(ChainSolana.Devnet)
            4 -> Web3jUtils.get(ChainTron.Nile)
            else -> throw IllegalAccessException("")
        }

    }
    val mainAddress = remember {
        when (web3jUtils.web3Universal) {
            is ChainETH -> mainAddressEth
            is ChainTron -> mainAddressTron
            is ChainSolana -> mainAddressSolana
            else -> ""
        }
    }
    val mainPrivate = remember {
        when (web3jUtils.web3Universal) {
            is ChainETH -> mainPrivateEth
            is ChainTron -> mainPrivateTron
            is ChainSolana -> mainPrivateSolana
            else -> ""
        }
    }
    val testAddress = remember {
        when (web3jUtils.web3Universal) {
            is ChainETH -> testAddressEth
            is ChainTron -> testAddressTron
            is ChainSolana -> testAddressSolana
            else -> ""
        }
    }
    val rememberCoroutineScope = rememberCoroutineScope()
    val showText = remember {
        mutableStateOf("")
    }
    LazyColumn(content = {
        item {
            ShowItem(title = "返回上一步") {
                type.value = 0
            }
        }
        item {
            ShowItem(title = "生成私钥地址") {
                rememberCoroutineScope.launch {
                    val web3KeyPairByMnemonics = web3jUtils.getWeb3KeyPairByMnemonics(mainMnemonics.split(" ").filter {
                        it.isNotEmpty()
                    })
                    showText.value = web3KeyPairByMnemonics.toString()
                    LogUtils.e(web3KeyPairByMnemonics)
                }
            }
        }
        item {
            ShowItem(title = "getBalance") {
                rememberCoroutineScope.launch {
                    showText.value = ""
                    val mainAddressBalance = web3jUtils.getBalance(mainAddress)
                    if (mainAddressBalance.error == null) {
                        val main = "mainAddressBalance:" + mainAddressBalance.result.toBigDecimal().toConvertWei(web3jUtils.convertWei).toPlainString()
                        LogUtils.e(main)
                        showText.value = showText.value + "${main}\n "
                    }
                    val testAddressBalance = web3jUtils.getBalance(testAddress)
                    if (testAddressBalance.error == null) {
                        val test = "testAddressBalance:" + testAddressBalance.result.toBigDecimal().toConvertWei(web3jUtils.convertWei).toPlainString()
                        LogUtils.e(test)
                        showText.value = showText.value + "${test}\n "
                    }
                }

            }
            ShowItem(title = "gasPrice") {
                rememberCoroutineScope.launch {
                    val gasPrice = web3jUtils.gasPrice()
                    if (gasPrice.error == null) {
                        val s = "gasPrice:" + gasPrice.result.toBigDecimal().toConvertWei(web3jUtils.convertWei).toPlainString()
                        LogUtils.e(s)
                        showText.value = s
                    }
                }
            }
            ShowItem(title = "transfer") {
                rememberCoroutineScope.launch {
                    val mainAddressBalance = web3jUtils.transfer(mainPrivate, testAddress, BigDecimal("0.0001"))
                    LogUtils.e(mainAddressBalance)
                    showText.value = mainAddressBalance.toString()
                }
            }


            ShowItem(title = "test") {
                rememberCoroutineScope.launch {

                }
            }
        }
        item {
            ShowItem(title = "getBalance") {
                rememberCoroutineScope.launch {
                    val mainAddressBalance = web3jUtils.getBalance(mainAddress)
                    if (mainAddressBalance.error == null) {
                        LogUtils.e("mainAddressBalance:" + mainAddressBalance.result.toBigDecimal().toConvertWei(web3jUtils.convertWei).toPlainString())
                    }
                    val testAddressBalance = web3jUtils.getBalance(testAddress)
                    if (testAddressBalance.error == null) {
                        LogUtils.e("mainAddressBalance:" + testAddressBalance.result.toBigDecimal().toConvertWei(web3jUtils.convertWei).toPlainString())
                    }

//                        val gasPrice = web3jUtils.gasPrice()
//                        if (gasPrice.error == null) {
//                            LogUtils.e("gasPrice:" + gasPrice.result.toBigDecimal().toConvertWei(Convert.Unit.ETHER).toPlainString())
//                        }
//                        val transfer = web3jUtils.transfer(privateKey, "3Pcp4mCKXGoiD2sYBvtZdAxjX6k9uAxm5E4fwECrmxUK", Convert.toWei(BigDecimal("0.001"), Convert.Unit.ETHER).toBigIntegerExact())
//                        if (transfer.error == null) {
//                            LogUtils.e("transfer:" + transfer.result)
//                        }
//                        val balance2 = web3jUtils.getBalance(address2)
//                        if (balance2.error == null) {
//                            LogUtils.e("balance2:" + balance2.result.toBigDecimal().toConvertWei(Convert.Unit.ETHER).toPlainString())
//                        }
//                        val tokenBalance = web3jUtils.getTokenBalance(address, "balanceOf", contractAddressWord)
//                        if (tokenBalance.error == null) {
//                            LogUtils.e("tokenBalance:" + tokenBalance.result.toBigDecimal().toConvertWei(Convert.Unit.ETHER).toPlainString())
//                        }
//                        val transferToken = web3jUtils.transferToken(
//                            privateKey2,
//                            address,
//                            contractAddressWord,
//                            "transfer",
//                            Convert.toWei(BigDecimal("1"), Convert.Unit.ETHER).toBigIntegerExact(), java.math.BigInteger("81000")
//                        )
//                        if (transferToken.error == null) {
//                            LogUtils.e("transferToken:" + transferToken.result)
//                        }

//                        val hash = "0x8f467225c706afc6c4ef87686d709d0dc6040276033a1c8f0cd73ca8d4e4bfcc"
//                        val transactionReceipt = web3jUtils.getTransactionReceipt(hash)
//                        if (transactionReceipt.error == null) {
//                            LogUtils.e("transactionReceipt:" + transactionReceipt.result)
//                        }
//                        val getBlockByHash = web3jUtils.getBlockByHash(transactionReceipt.result?.blockHash ?: "")
//                        if (getBlockByHash.error == null) {
//                            LogUtils.e("transactionReceipt:" + getBlockByHash.result?.timestamp?.decodeQuantity())
//                        }
//                        val skinBalance = web3jUtils.getTokenBalance(address, "balanceOf", contractAddressSkin)
//                        if (skinBalance.error == null) {
//                            LogUtils.e("skinBalance:" + skinBalance.result.toBigDecimal().toPlainString())
//                        }
//                        val skinTokenId = web3jUtils.getTokenIdByOwnerIndex(address, 0, contractAddressSkin)
//                        if (skinTokenId.error == null) {
//                            LogUtils.e("skinTokenId:" + skinTokenId.result.decodeQuantity().toString())
//                            val transferSkin = web3jUtils.transferSkin(privateKey, address2, skinTokenId.result.decodeQuantity(), contractAddressSkin)
//                            if (transferSkin.error == null) {
//                                LogUtils.e("transferSkin:" + transferSkin.result)
//                            }
//
//                        }
//                        val transferSkin = web3jUtils.transferSkin(privateKey, address2, java.math.BigInteger("61804"), contractAddressSkin)
//                        if (transferSkin.error == null) {
//                            LogUtils.e("transferSkin:" + transferSkin.result)
//                        }

                }
            }

        }
        item {
            Column {
                Text(text = "结果:")
                Text(text = showText.value)
            }
        }
    })

}

@Composable
fun ShowItem(title: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .fillMaxWidth()
            .height(50.dp)
            .padding(10.dp), contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }

}