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
    val failCount = mutableMapOf<AccountArgument, Int>()
    while (true) {
        try {
            var text = ""

            var allAccounts: List<WoobBankAccount> = emptyList()

            Log.d("accountArguments=${arguments.accountArguments}")
            for (accountArgument in arguments.accountArguments) {
                Log.d("accountArgument=$accountArgument")

                // TODO remove this workaround once https://gitlab.com/woob/woob/-/issues/614 is fixed
                woobBankExecutor.axabanqueWorkaround()

                val transactions = woobBankExecutor.getTransactions(accountArgument.id)
                Log.d("transactions.size=${transactions.size}")
                Log.d("transactions=$transactions")
                if (transactions.isEmpty()) {
                    Log.d("Empty list, probably credential issues")
                    val failCountForAccount = failCount.getOrPut(accountArgument) { 0 } + 1
                    failCount[accountArgument] = failCountForAccount
                    if (failCountForAccount >= 8) {
                        text += "_${accountArgument.name}_\n:warning: Could not retrieve transactions for $failCountForAccount times. Check credentials!\n\n"
                    }
                    continue
                }
                val newTransactions = transactions - (lastTransactions[accountArgument] ?: emptyList()).toSet()
                Log.d("newTransactions.size=${newTransactions.size}")
                Log.d("newTransactions=$newTransactions")

                if (newTransactions.isNotEmpty()) {
                    text += "_${accountArgument.name}_\n"
                }
                for (transaction in newTransactions) {
                    val transactionText =
                        "${if (transaction.amount.startsWith('-')) "🔻" else ":small_green_triangle:"} *${transaction.amount}* - ${transaction.raw}\n"
                    Log.d(transactionText)
                    text += transactionText
                }
                lastTransactions[accountArgument] = transactions

                // Show balance if there was at least one transaction
                if (newTransactions.isNotEmpty()) {
                    if (allAccounts.isEmpty()) {
                        allAccounts = woobBankExecutor.getAccounts()
                        Log.d("allAccounts=$allAccounts")
                    }
                    val accountsByBank = allAccounts.groupBy { it.id.getBankId() }

                    text += ":sum: _${accountArgument.name}_ balance: *${accountsByBank[accountArgument.id.getBankId()]!!.sumByBalance()}*\n\n"
                }
            }

            Log.d("text=$text")
            if (text.isNotEmpty()) {
                val postResult = slackClient.postMessage(text = text, channel = arguments.slackChannel)
                Log.d("postResult=$postResult")
            }
        } catch (t: Throwable) {
            Log.w(t, "Caught exception in main loop")
        }

        Log.d("Sleep 4 hours")
        TimeUnit.HOURS.sleep(4)
    }
}

private fun String.getBankId() = split('@')[1]


private fun Iterable<WoobBankAccount>.sumByBalance(): Double = sumOf { (it.balance.toDouble() * 100).toInt() } / 100.0
