/*
 * Copyright © 2021 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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

package dev.nikomaru.raceassist.transport.discord

import dev.nikomaru.raceassist.files.Config
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class DiscordWebhook {

    fun sendWebHook(json: String) {
        try {
            val webHookUrl = URL(Config.discordWebHook)
            val con: HttpsURLConnection = (webHookUrl.openConnection() as HttpsURLConnection)

            con.addRequestProperty("Content-Type", "application/JSON; charset=utf-8")
            con.addRequestProperty("User-Agent", "DiscordBot")
            con.doOutput = true
            con.requestMethod = "POST"

            con.setRequestProperty("Content-Length", json.length.toString())

            val stream: OutputStream = con.outputStream
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.close()

            val status: Int = con.responseCode
            if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_NO_CONTENT) {
                println("error:$status")
            }
            con.disconnect()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}