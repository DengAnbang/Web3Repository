package com.dab.dapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


class DappMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val type = remember {
                mutableStateOf(0)
            }
            when (type.value) {
                1,2,3,4 -> {
                    TestMultiChain(type)
                }

                else -> {
                    LazyColumn(content = {
                        item {
                            Text(text = "选择链:")
                        }
                        item {
                            ShowItem(title = "ETH") {
                                type.value = 1
                            }
                        }
                        item {
                            ShowItem(title = "BSC") {
                                type.value = 2
                            }
                        }
                        item {
                            ShowItem(title = "Solana") {
                                type.value = 3
                            }
                        }
                        item {
                            ShowItem(title = "Tron") {
                                type.value = 4
                            }
                        }

                    })
                }
            }


        }
    }


}