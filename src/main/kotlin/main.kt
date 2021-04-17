import org.jraf.woobbankslack.arguments.Account
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

    val allAccounts = woobBankExecutor.getAccounts()
    Log.d("allAccounts=$allAccounts")
    val accountsByBank = allAccounts.groupBy { it.id.getBankId() }

    val lastTransactions = mutableMapOf<Account, List<WoobBankTransaction>>()
    while (true) {
        try {
            Log.d("accounts=${arguments.accounts}")
            for (account in arguments.accounts) {
                val transactions = woobBankExecutor.getTransactions(account.id)
                Log.d(transactions)
                if (transactions.isEmpty()) {
                    Log.d("Empty list, probably a bug: ignore")
                    continue
                }
                val newTransactions = transactions - (lastTransactions[account] ?: emptyList())
                for (transaction in newTransactions) {
                    val text = """
                        _${account.name}_
                        ${if (transaction.amount.startsWith('-')) "🔻" else ":small_green_triangle:"} *${transaction.amount}* - ${transaction.raw}
                    """.trimIndent()
                    Log.d(text)
                    val postResult = slackClient.postMessage(text = text, channel = arguments.slackChannel)
                    Log.d("postResult=$postResult")

                    // Sleep a bit between posts
                    TimeUnit.SECONDS.sleep(1)
                }
                lastTransactions[account] = transactions

                // Show balance if there was at least one transaction
                if (newTransactions.isNotEmpty()) {
                    val text = """
                        :sum: _${account.name}_ balance: *${accountsByBank[account.id.getBankId()]!!.sumByBalance()}* 
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
