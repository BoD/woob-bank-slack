package org.jraf.woobbankslack.woob

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.jraf.woobbankslack.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

class WoobBankExecutor(private val config: Config) {
    private val moshi = Moshi.Builder().build()

    private val transactionJsonAdapter: JsonAdapter<List<WoobBankTransaction>> = moshi.adapter(Types.newParameterizedType(List::class.java, WoobBankTransaction::class.java))
    private val accountJsonAdapter: JsonAdapter<List<WoobBankAccount>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, WoobBankAccount::class.java))


    fun getTransactions(accountId: String): List<WoobBankTransaction> {
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
        return transactionJsonAdapter.fromJson(commandResult)!!.reversed()
    }

    fun getAccounts(): List<WoobBankAccount> {
        val commandResult = runCommand(
            workingDir = File(config.woobDirectory),
            config.woobDirectory + "/" + "woob",
            "bank",
            "list",
            "-f",
            "json",
            "--auto-update",
        )
        return accountJsonAdapter.fromJson(commandResult)!!
    }


    private fun runCommand(workingDir: File, vararg command: String): String {
        Log.d("runCommand workingDir=$workingDir command=${command.asList()}")
        val process = ProcessBuilder(command.asList())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val success = process.waitFor(1, TimeUnit.MINUTES)
        if (!success) {
            process.destroyForcibly()
            Log.w("Timeout reached while executing the command")
            throw Exception("Timeout reached while executing the command")
        }
        val res = process.inputStream.bufferedReader().readText().trim()
        Log.d("Command executed successfully")
        return res
    }

    data class Config(
        val woobDirectory: String,
    )
}