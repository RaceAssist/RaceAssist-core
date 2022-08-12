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
package dev.nikomaru.raceassist.files

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.*
import java.io.File

object Config {
    lateinit var config: ConfigData
    const val version: String = "1.0.0"

    @ExperimentalSerializationApi
    fun load() {
        val file = plugin.dataFolder.resolve("config.conf")

        createConfig(file)

        config = hocon.decodeFromConfig(ConfigFactory.parseFile(file))
    }

    @ExperimentalSerializationApi
    private fun createConfig(file: File) {
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        val spreadSheet = SpreadSheet(8888, arrayListOf())
        val discordWebHook = DiscordWebHook(arrayListOf(), arrayListOf())
        val configData = ConfigData(version, 40, 200, discordWebHook, spreadSheet, arrayListOf(), 600)

        val renderOptions = ConfigRenderOptions.defaults().setOriginComments(false).setComments(false).setFormatted(true).setJson(false)
        val string = hocon.encodeToConfig(configData).root().render(renderOptions)

        if (!file.exists()) {
            file.createNewFile()
            file.writeText(string)
        } else {
            val verNode = hocon.decodeFromConfig<ConfigData>(ConfigFactory.parseFile(file)).version
            if (verNode != version) {
                file.writeText(string)
            }
        }
    }

}

@ExperimentalSerializationApi
private val hocon = Hocon


