package com.dab.dapp.bean

/**
 * Created by dab on 2023/4/12 10:04
 */
sealed class Chains(open val chainName: String, open val chainNamespace: String, open val chainReference: String, open val methods: List<String>, open val events: List<String>) {
    object EthereumMain : Chains(chainName = "Ethereum", chainNamespace = Info.Eth.chain, chainReference = "1", methods = Info.Eth.defaultMethods, events = Info.Eth.defaultEvents)
    object BNBMainnet : Chains(chainName = "BNB Smart Chain", chainNamespace = Info.Eth.chain, chainReference = "56", methods = Info.Eth.defaultMethods, events = Info.Eth.defaultEvents)
    object BNBTestnet : Chains(chainName = "BNB Smart Chain", chainNamespace = Info.Eth.chain, chainReference = "97", methods = Info.Eth.defaultMethods, events = Info.Eth.defaultEvents)
    data class Custom(
        override val chainName: String,
        override val chainNamespace: String,
        override val chainReference: String,
        override val methods: List<String>,
        override val events: List<String>
    ) : Chains(chainName = chainName, chainNamespace = chainNamespace, chainReference = chainReference, methods = methods, events = events)

    val chainId: String = "$chainNamespace:$chainReference"
}


sealed class Info(open val chain: String, open val defaultEvents: List<String>, open val defaultMethods: List<String>) {
    object Eth : Info(
        chain = "eip155",
        defaultEvents = listOf("chainChanged", "accountChanged"),
        defaultMethods = listOf(
            "eth_sendTransaction",
            "personal_sign",
            "eth_sign",
            "eth_signTypedData"
        )
    )

    object Cosmos : Info(
        chain = "cosmos",
        defaultEvents = listOf("chainChanged", "accountChanged"),
        defaultMethods = listOf(
            "cosmos_signDirect",
            "cosmos_signAmino"
        )
    )


    data class Custom(
        override val chain: String,
        override val defaultEvents: List<String>,
        override val defaultMethods: List<String>,
    ) : Info(chain = chain, defaultEvents = defaultEvents, defaultMethods = defaultMethods)
}