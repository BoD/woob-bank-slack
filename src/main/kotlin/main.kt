import org.jraf.woobbankslack.arguments.AccountArgument
import org.jraf.woobbankslack.arguments.Arguments
import org.jraf.woobbankslack.slack.AuthTokenProvider
import org.jraf.woobbankslack.slack.SlackClient
import org.jraf.woobbankslack.util.Log
import org.jraf.woobbankslack.woob.WoobBankAccount
import org.jraf.woobbankslack.woob.WoobBankExecutor
import org.jraf.woobbankslack.woob.WoobBankTransaction
import java.util.concurrent.TimeUnit

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun main(args: Array<String>) {
    println("Hello World!")
    val arguments = Arguments(args)

    val woobBankConfig = WoobBankExecutor.Config(woobDirectory = arguments.woobDirectory)
    val woobBankExecutor = WoobBankExecutor(woobBankConfig)

    val slackClient = SlackClient(object : AuthTokenProvider {
        override fun getAuthToken() = arguments.slackAuthToken
    })

    val lastTransactions = mutableMapOf<AccountArgument, List<WoobBankTransaction>>()
    while (true) {
        try {
            val allAccounts: List<WoobBankAccount> = woobBankExecutor.getAccounts()
            Log.d("allAccounts=$allAccounts")
            val accountsByBank = allAccounts.groupBy { it.id.getBankId() }

            Log.d("accountArguments=${arguments.accountArguments}")
            for (accountArgument in arguments.accountArguments) {
                val transactions = woobBankExecutor.getTransactions(accountArgument.id)
                Log.d(transactions)
                if (transactions.isEmpty()) {
                    Log.d("Empty list, probably a bug: ignore")
                    continue
                }
                val newTransactions = transactions - (lastTransactions[accountArgument] ?: emptyList())
                for (transaction in newTransactions) {
                    val text = """
                        _${accountArgument.name}_
                        ${if (transaction.amount.startsWith('-')) "🔻" else ":small_green_triangle:"} *${transaction.amount}* - ${transaction.raw}
                    """.trimIndent()
                    Log.d(text)
                    val postResult = slackClient.postMessage(text = text, channel = arguments.slackChannel)
                    Log.d("postResult=$postResult")

                    // Sleep a bit between posts
                    TimeUnit.SECONDS.sleep(1)
                }
                lastTransactions[accountArgument] = transactions

                // Show balance if there was at least one transaction
                if (newTransactions.isNotEmpty()) {
                    val text = """
                        :sum: _${accountArgument.name}_ balance: *${accountsByBank[accountArgument.id.getBankId()]!!.sumByBalance()}* 
                    """.trimIndent()
                    Log.d("text=$text")
                    val postResult = slackClient.postMessage(text = text, channel = arguments.slackChannel)
                    Log.d("postResult=$postResult")
                }
            }
        } catch (t: Throwable) {
            Log.w(t, "Caught exception in main loop")
        }

        Log.d("Sleep 3 hours")
        TimeUnit.HOURS.sleep(3)
    }
}

private fun String.getBankId() = split('@')[1]


private fun Iterable<WoobBankAccount>.sumByBalance(): Double = sumBy { (it.balance.toDouble() * 100).toInt() } / 100.0
