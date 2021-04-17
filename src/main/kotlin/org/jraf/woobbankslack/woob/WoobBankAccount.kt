package org.jraf.woobbankslack.woob

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WoobBankAccount(
    val id: String,
    val label: String,
    val balance: String,
)