/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2023-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:Suppress("LoggingStringTemplateAsArgument")

import org.jraf.klibslack.client.SlackClient
import org.jraf.klibslack.client.configuration.ClientConfiguration
import org.jraf.woobbankslack.arguments.AccountArgument
import org.jraf.woobbankslack.arguments.Arguments
import org.jraf.woobbankslack.woob.WoobBankExecutor
import org.jraf.woobbankslack.woob.json.JsonWoobBankAccount
import org.jraf.woobbankslack.woob.json.JsonWoobBankTransaction
import org.slf4j.LoggerFactory
import org.slf4j.simple.SimpleLogger
import java.util.concurrent.TimeUnit

private val LOGGER = run {
    // This must be done before any logger is initialized
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "trace")
    System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true")
    System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss")

    LoggerFactory.getLogger("Main")
}

suspend fun main(args: Array<String>) {
    println("Hello World!")
    val arguments = Arguments(args)

    val woobBankConfig = WoobBankExecutor.Config(woobDirectory = arguments.woobDirectory)
    val woobBankExecutor = WoobBankExecutor(woobBankConfig)

    val slackClient = SlackClient.newInstance(ClientConfiguration("", arguments.slackAuthToken))

    val lastTransactions = mutableMapOf<AccountArgument, List<JsonWoobBankTransaction>>()
    val failCount = mutableMapOf<AccountArgument, Int>()
    while (true) {
        try {
            var text = ""

            var allAccounts: List<JsonWoobBankAccount> = emptyList()

            LOGGER.debug("accountArguments=${arguments.accountArguments}")
            for (accountArgument in arguments.accountArguments) {
                LOGGER.debug("accountArgument=$accountArgument")

                // TODO remove this workaround once https://gitlab.com/woob/woob/-/issues/614 is fixed
                woobBankExecutor.axabanqueWorkaround()

                val transactions = woobBankExecutor.getTransactions(accountArgument.id)
                LOGGER.debug("transactions.size=${transactions.size}")
                LOGGER.debug("transactions=$transactions")
                if (transactions.isEmpty()) {
                    LOGGER.debug("Empty list, probably credential issues")
                    val failCountForAccount = failCount.getOrPut(accountArgument) { 0 } + 1
                    failCount[accountArgument] = failCountForAccount
                    if (failCountForAccount >= 8) {
                        text += "_${accountArgument.name}_\n:warning: Could not retrieve transactions for $failCountForAccount times. Check credentials!\n\n"
                    }
                    continue
                }
                val newTransactions = transactions - (lastTransactions[accountArgument] ?: emptyList()).toSet()
                LOGGER.debug("newTransactions.size=${newTransactions.size}")
                LOGGER.debug("newTransactions=$newTransactions")

                if (newTransactions.isNotEmpty()) {
                    text += "_${accountArgument.name}_\n"
                }
                for (transaction in newTransactions) {
                    val transactionText =
                        "${if (transaction.amount.startsWith('-')) "🔻" else ":small_green_triangle:"} *${transaction.amount}* - ${transaction.raw}\n"
                    LOGGER.debug(transactionText)
                    text += transactionText
                }
                lastTransactions[accountArgument] = transactions

                // Show balance if there was at least one transaction
                if (newTransactions.isNotEmpty()) {
                    if (allAccounts.isEmpty()) {
                        allAccounts = woobBankExecutor.getAccounts()
                        LOGGER.debug("allAccounts=$allAccounts")
                    }
                    val accountsByBank = allAccounts.groupBy { it.id.getBankId() }

                    text += ":sum: _${accountArgument.name}_ balance: *${accountsByBank[accountArgument.id.getBankId()]!!.sumByBalance()}*\n\n"
                }
            }

            LOGGER.debug("text=$text")
            if (text.isNotEmpty()) {
                slackClient.chatPostMessage(text = text, channel = arguments.slackChannel)
            }
        } catch (t: Throwable) {
            LOGGER.warn("Caught exception in main loop", t)
        }

        LOGGER.debug("Sleep 4 hours")
        TimeUnit.HOURS.sleep(4)
    }
}

private fun String.getBankId() = split('@')[1]


private fun Iterable<JsonWoobBankAccount>.sumByBalance(): Double =
    sumOf { (it.balance.toDouble() * 100).toInt() } / 100.0
