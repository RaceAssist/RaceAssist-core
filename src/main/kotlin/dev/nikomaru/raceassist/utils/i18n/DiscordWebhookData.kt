/*
 *     Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.utils.i18n

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.awt.Color

@Serializable
data class DiscordWebhookData(val username: String = "RaceAssist",
    val avatar_url: String = "https://github.com/Nlkomaru/RaceAssist-web/blob/bed3817eb04c73aed2726ea7923113776d468dbb/public/favicon.png?raw=true",
    val content: String = "",
    val embeds: ArrayList<DiscordWebhookEmbed>)

@Serializable
data class DiscordWebhookEmbed(
    val author: Author? = null,
    val title: String? = null,
    val url: String? = null,
    val description: String? = null,
    val color: Int? = Integer.valueOf(Integer.toHexString(Color.GREEN.rgb).substring(2), 16),
)

@Serializable
data class Author(val name: String, val url: String? = null, @SerialName("icon_url") val iconUrl: String)
