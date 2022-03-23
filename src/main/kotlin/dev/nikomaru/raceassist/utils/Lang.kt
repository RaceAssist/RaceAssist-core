/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
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

package dev.nikomaru.raceassist.utils

import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.io.File
import java.io.InputStreamReader
import java.text.MessageFormat
import java.util.*

object Lang {
    private val langList: HashMap<String, Properties> = HashMap()

    fun load() {
        plugin.launch {
            withContext(Dispatchers.IO) {
                val lang = listOf("ja_JP")

                lang.forEach { locale ->
                    val conf = Properties()
                    conf.load(InputStreamReader(this.javaClass.classLoader.getResourceAsStream("lang/$locale.properties")!!, "UTF-8"))
                    langList[locale] = conf
                }
                if (!plugin.dataFolder.exists()) {
                    plugin.dataFolder.mkdir()
                }
                val pluginDir = File(plugin.dataFolder, "lang")
                if (!pluginDir.exists()) {
                    pluginDir.mkdir()
                }
                withContext(Dispatchers.IO) {
                    pluginDir.listFiles()?.forEach {
                        langList[it.nameWithoutExtension] = Properties().apply {
                            plugin.logger.info("Loading lang file for ${it.nameWithoutExtension}")
                            load(InputStreamReader(it.inputStream(), "UTF-8"))
                        }
                    }
                }
            }
        }
    }

    fun getComponent(key: String, locale: Locale, vararg args: Any?): Component {
        val lang = langList[locale.toString()] ?: langList["ja_JP"]
        return lang?.getProperty(key)?.let { MiniMessage.miniMessage().deserialize(MessageFormat.format(it, *args)) } ?: MiniMessage.miniMessage()
            .deserialize(key)
    }

    fun getText(key: String, locale: Locale, vararg args: Any?): String {
        val lang = langList[locale.toString()] ?: langList["ja_JP"]
        return lang?.getProperty(key)?.let { MessageFormat.format(it, *args) } ?: key
    }

}
