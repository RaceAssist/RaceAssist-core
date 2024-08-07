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

package dev.nikomaru.raceassist.utils

import dev.nikomaru.raceassist.RaceAssist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.InputStreamReader
import java.text.MessageFormat
import java.util.*

object Lang : KoinComponent {
    val plugin: RaceAssist by inject()
    private val langList: HashMap<String, Properties> = HashMap()

    private val mm = MiniMessage.miniMessage()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            val lang = listOf("ja_JP", "en_US")

            lang.forEach { locale ->
                val conf = Properties()
                plugin.logger.info("Loading lang file for $locale")
                conf.load(
                    InputStreamReader(
                        this.javaClass.classLoader.getResourceAsStream("lang/$locale.properties")!!,
                        "UTF-8"
                    )
                )
                langList[locale] = conf
            }

            if (!plugin.dataFolder.exists()) {
                plugin.dataFolder.mkdir()
            }
            val langDir = File(plugin.dataFolder, "lang")
            if (!langDir.exists()) {
                langDir.mkdir()
            }
            withContext(Dispatchers.IO) {
                langDir.listFiles()?.forEach {
                    plugin.logger.info("Loading local lang file for ${it.nameWithoutExtension}")
                    langList[it.nameWithoutExtension] = Properties().apply {
                        load(InputStreamReader(it.inputStream(), "UTF-8"))
                    }
                }
            }
        }

    }

    fun getComponent(key: String, locale: Locale, vararg args: Any?): Component {
        val lang = langList[locale.toString()] ?: langList["ja_JP"]
        return lang?.getProperty(key)?.let { mm.deserialize(MessageFormat.format(it, *args)) } ?: mm.deserialize(key)
    }

    fun getText(key: String, locale: Locale, vararg args: Any?): String {
        val lang = langList[locale.toString()] ?: langList["ja_JP"]
        return lang?.getProperty(key)?.let { MessageFormat.format(it, *args) } ?: key
    }

    private fun getTranslatedText(key: String, locale: Locale, vararg args: Any?): String {
        val lang = langList[locale.toString()] ?: langList["ja_JP"]
        return lang?.getProperty(key)?.let { MessageFormat.format(it, *args) } ?: key
    }

    fun CommandSender.sendI18nRichMessage(key: String, vararg args: Any?) {
        val locale = if (this is Player) this.locale() else Locale.getDefault()
        println("locale: $locale")
        println("key: $key")
        this.sendRichMessage(getTranslatedText(key, locale, *args))
    }

}
