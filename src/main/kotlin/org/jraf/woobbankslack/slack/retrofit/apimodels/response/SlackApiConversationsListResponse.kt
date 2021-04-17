/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2020-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.woobbankslack.slack.retrofit.apimodels.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SlackApiConversationsListResponse(
    val ok: Boolean,

    val channels: List<SlackApiChannel>,

    @Json(name = "response_metadata")
    val responseMetadata: SlackApiResponseMetadata?,
)

@JsonClass(generateAdapter = true)
data class SlackApiChannel(
    val name: String,
    val topic: SlackApiChannelTopicOrPurpose?,
    val purpose: SlackApiChannelTopicOrPurpose?,
)

@JsonClass(generateAdapter = true)
data class SlackApiChannelTopicOrPurpose(
    val value: String?,
)

@JsonClass(generateAdapter = true)
data class SlackApiResponseMetadata(
    @Json(name = "next_cursor")
    val nextCursor: String?,
)
