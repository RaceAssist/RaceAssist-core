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

package dev.nikomaru.raceassist.utils.i18n.web

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
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

data class RegisterWebAccountData(override val type: LogDataType,
    override val date: ZonedDateTime = ZonedDateTime.now(),
    override val executor: UUID) : LogData {

    init {
        val data = this
        plugin.launch {
            sendConsoleMessage(data)
            sendDiscordWebhook(data)
            sendPlayerMessage(data)
            sendWebRecordBody(data)
        }
    }

    override suspend fun <T : LogData> sendDiscordWebhook(data: T) {
        data as RegisterWebAccountData
        withContext(Dispatchers.IO) {
            Config.config.discordWebHook.web.forEach {
                val url = it
                val content = "UUID: ${data.executor}がWebアカウントを登録しました。"
                val author = Author(name = "Webアカウント登録", iconUrl = "https://crafthead.net/avatar/${data.executor}")
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
        data as RegisterWebAccountData
        Bukkit.getPlayer(data.executor)?.sendRichMessage("<color:green>[RaceAssist]: Webアカウントを登録しました。")
    }

    override suspend fun <T : LogData> sendWebRecordBody(data: T) {
        data as RegisterWebAccountData
        val body = RecordLogData("web/register/${data.executor}", data)
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
        data as RegisterWebAccountData
        plugin.logger.info("UUID: ${data.executor}がWebアカウントを登録しました。")
    }
}
