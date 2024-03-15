package com.dab.web3.dapp

import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Created by dab on 2023/8/1 14:24
 */

fun BigDecimal.toConvertWei(bitNum: Convert.Unit): BigDecimal {
    return Convert.fromWei(this, bitNum)
}

fun BigDecimal.toConvertWei(bitNum: Int): BigDecimal {
    return this.divide(BigDecimal.TEN.pow(bitNum))
}

fun BigDecimal.toShowGasPrice(gasLimit: BigDecimal = BigDecimal("21000")): String {
    return this.toConvertWei(Convert.Unit.ETHER).multiply(gasLimit)
        .setScale(6, RoundingMode.DOWN).toString()
}

fun BigDecimal.toShowGasOriginalPrice(gasLimit: BigDecimal = BigDecimal("21000")): BigDecimal {
    return this.toConvertWei(Convert.Unit.ETHER).multiply(gasLimit)
}