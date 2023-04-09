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

package org.jraf.woobbankslack.woob

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jraf.woobbankslack.woob.json.JsonWoobBankAccount
import org.jraf.woobbankslack.woob.json.JsonWoobBankTransaction
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

private val LOGGER = LoggerFactory.getLogger(WoobBankExecutor::class.java)

class WoobBankExecutor(private val config: Config) {
    fun getTransactions(accountId: String): List<JsonWoobBankTransaction> {
        val commandResult = runCommand(
            workingDir = File(config.woobDirectory),
            config.woobDirectory + "/" + "woob",
            "bank",
            "history",
            accountId,
            "-f",
            "json",
            "-n",
            "16",
            "--auto-update",
        )
        return Json.decodeFromString<List<JsonWoobBankTransaction>>(commandResult).reversed()
    }

    fun getAccounts(): List<JsonWoobBankAccount> {
        val commandResult = runCommand(
            workingDir = File(config.woobDirectory),
            config.woobDirectory + "/" + "woob",
            "bank",
            "list",
            "-f",
            "json",
            "--auto-update",
        )
        return Json.decodeFromString<List<JsonWoobBankAccount>>(commandResult)
    }

    fun axabanqueWorkaround() {
        runCommand(
            workingDir = File(config.woobDirectory),
            config.woobDirectory + "/" + "woob",
            "bank",
            "storage",
            "flush",
            "axabanque",
        )
    }

    private fun runCommand(workingDir: File, vararg command: String): String {
        LOGGER.debug("runCommand workingDir=$workingDir command=${command.asList()}")
        val process = ProcessBuilder(command.asList())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val success = process.waitFor(6, TimeUnit.MINUTES)
        if (!success) {
            process.destroyForcibly()
            LOGGER.warn("Timeout reached while executing the command")
            throw Exception("Timeout reached while executing the command")
        }
        val res = process.inputStream.bufferedReader().readText().trim()
        LOGGER.debug("Command executed successfully")
        return res
    }

    data class Config(
        val woobDirectory: String,
    )
}
