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

package org.jraf.woobbankslack.arguments

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.cli.vararg

class Arguments(av: Array<String>) {
    private val parser = ArgParser("woobbankslack")

    val woobDirectory: String by parser.option(ArgType.String, fullName = "woob-directory", shortName = "w", description = "Directory where woob is installed")
        .required()

    val slackAuthToken: String by parser.option(ArgType.String, fullName = "slack-auth-token", shortName = "s", description = "Slack auth token").required()

    val slackChannel: String by parser.option(ArgType.String, fullName = "slack-channel", shortName = "c", description = "Slack channel").required()

    private val accountsStr: List<String> by parser.argument(ArgType.String, "accounts", description = "Accounts").vararg()
    val accountArguments: List<AccountArgument> get() = accountsStr.map(String::toAccountArgument)

    init {
        parser.parse(av)
    }
}

data class AccountArgument(val name: String, val id: String)

private fun String.toAccountArgument(): AccountArgument {
    val (name, id) = split(':')
    return AccountArgument(name, id)
}
