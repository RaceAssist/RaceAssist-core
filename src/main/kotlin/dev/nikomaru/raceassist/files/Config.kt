/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.nikomaru.raceassist.files

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.web.api.WebAPI
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

object Config : KoinComponent {
    val plugin: RaceAssist by inject()
    lateinit var config: ConfigData
    private const val VERSION: String = "2.0.0"

    @ExperimentalSerializationApi
    fun load() {
        val file = plugin.dataFolder.resolve("config.json")

        createConfig(file)

        config = json.decodeFromString(file.readText())
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun reload() {
        load()
        if (config.webAPI != null) {
            WebAPI.stopServer()
            plugin.settingWebAPI()
        }
    }

    @ExperimentalSerializationApi
    private fun createConfig(file: File) {
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        val discordWebHook = DiscordWebHook(arrayListOf(), arrayListOf(), arrayListOf(), arrayListOf(), arrayListOf())
        val recordHorse = RecordHorse(13.5, 3.8)
        val configData =
            ConfigData(VERSION, 40, 200, discordWebHook, recordHorse, null, 600000, null)

        val string = json.encodeToString(configData)

        if (!file.exists()) {
            file.createNewFile()
            println(file.name + "を作成しました。")
            file.writeText(string)
        } else {
            val verNode = json.decodeFromString<ConfigData>(file.readText()).version
            if (verNode != VERSION) {
                file.writeText(string)
            }
        }
    }

}

@ExperimentalSerializationApi
private val json = Json {
    isLenient = true
    prettyPrint = true
    ignoreUnknownKeys = true
}


