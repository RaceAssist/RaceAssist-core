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

package dev.nikomaru.raceassist.utils.i18n.bet

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.i18n.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import java.time.ZonedDateTime
import java.util.*

data class ChangeAvailableBetData(
    override val type: LogDataType,
    val raceId: String,
    override val executor: UUID?,
    val available: Boolean,
    override val date: ZonedDateTime = ZonedDateTime.now(),
) : LogData {
    init {
        val data = this
        RaceAssist.plugin.launch {
            sendConsoleMessage(data)
            sendDiscordWebhook(data)
            sendPlayerMessage(data)
            sendWebRecordBody(data)
        }
    }

    override suspend fun <T : LogData> sendDiscordWebhook(data: T) {
        data as ChangeAvailableBetData
        withContext(Dispatchers.IO) {
            val status = if (data.available) "有効" else "無効"
            val name = data.executor?.let { Bukkit.getOfflinePlayer(it).name } ?: "コンソール"
            val content = "${name}により${data.raceId}のレースのベットが${status}に変更されました。"
            Config.config.discordWebHook.web.forEach {
                val url = if (it.last() != '/') "$it/" else it
                val iconUrl =
                    if (data.executor != null) "https://crafthead.net/avatar/${data.executor}" else "https://pub-fd809fa548a2432587b4ff9c40d8242d.r2.dev/terminal.png"
                val author = Author(name = "賭けステータス変更", iconUrl = iconUrl)
                val embed = DiscordWebhookEmbed(author = author, description = content)
                val body = DiscordWebhookData(embeds = arrayListOf(embed))
                Utils.client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)

                }
            }
        }
    }

    override fun <T : LogData> sendPlayerMessage(data: T) {
        data as ChangeAvailableBetData
        val status = if (data.available) "有効" else "無効"
        data.executor?.let { Bukkit.getPlayer(it)?.sendRichMessage("<color:green>[RaceAssist]: ${data.raceId}のレースのベットを${status}にしました。") }
    }

    override suspend fun <T : LogData> sendWebRecordBody(data: T) {
        data as ChangeAvailableBetData
        val uniqueId = data.executor?.let { Bukkit.getOfflinePlayer(it).uniqueId.toString() } ?: "console"
        val body = RecordLogData("bet/available/${uniqueId}", data)
        withContext(Dispatchers.IO) {
            Config.config.webAPI?.recordUrl?.forEach {
                Utils.client.post(it.url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    headers {
                        val token = Base64.getEncoder().encodeToString("${it.name}:${it.password}".toByteArray())
                        append("Authorization", "Basic $token")
                    }
                }
            }
        }
    }

    override suspend fun <T : LogData> sendConsoleMessage(data: T) {
        data as ChangeAvailableBetData
        val status = if (data.available) "有効" else "無効"
        RaceAssist.plugin.logger.info("${data.raceId}のレースのベットを${status}にしました。")
    }
}
