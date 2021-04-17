package org.jraf.woobbankslack.woob

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WoobBankTransaction(
    val id: String,
    val rdate: String,
    val raw: String,
    val amount: String,
)