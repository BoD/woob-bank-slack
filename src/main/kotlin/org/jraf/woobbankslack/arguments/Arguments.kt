/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalCli::class)

package org.jraf.woobbankslack.arguments

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.required
import kotlinx.cli.vararg

@Suppress("PropertyName", "PrivatePropertyName")
class Arguments(av: Array<String>) {
    private val parser = ArgParser("woobbankslack")

    val woobDirectory: String by parser.option(ArgType.String, fullName = "woob-directory", shortName = "w", description = "Directory where woob is installed")
        .required()

    val slackAuthToken: String by parser.option(ArgType.String, fullName = "slack-auth-token", shortName = "s", description = "Slack auth token").required()

    val slackChannel: String by parser.option(ArgType.String, fullName = "slack-channel", shortName = "c", description = "Slack channel").required()

    private val accountsStr: List<String> by parser.argument(ArgType.String, "accounts", description = "Accounts").vararg()
    val accounts: List<Account> get() = accountsStr.map(String::toAccount)

    init {
        parser.parse(av)
    }
}

data class Account(val name: String, val id: String)

private fun String.toAccount(): Account {
    val (name, id) = split(':')
    return Account(name, id)
}