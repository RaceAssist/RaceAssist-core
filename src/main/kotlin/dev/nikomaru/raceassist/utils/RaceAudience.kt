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

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

class RaceAudience {

    private val audience: HashSet<UUID> = HashSet()

    fun showTitleI18n(key: String, vararg args: String) {
        plugin.launch {
            audience.map {
                async {
                    val offlinePlayer = Bukkit.getOfflinePlayer(it)
                    if (offlinePlayer.isOnline) {
                        val player = offlinePlayer.player!!
                        player.showTitle(title(Lang.getComponent(key, player.locale(), *args), text(" ")))
                    }
                }
            }.awaitAll()
        }
    }

    fun showTitle(title: Title) {
        plugin.launch {
            audience.map {
                async {
                    val offlinePlayer = Bukkit.getOfflinePlayer(it)
                    if (offlinePlayer.isOnline) {
                        val player = offlinePlayer.player!!
                        player.showTitle(title)
                    }
                }
            }.awaitAll()
        }
    }

    fun sendMessageI18n(key: String, vararg args: Any) {
        plugin.launch {
            audience.map {
                async {
                    val offlinePlayer = Bukkit.getOfflinePlayer(it)
                    if (offlinePlayer.isOnline) {
                        val player = offlinePlayer.player!!
                        player.sendMessage(Lang.getComponent(key, player.locale(), *args))
                    }
                }
            }.awaitAll()
        }
    }

    fun add(player: OfflinePlayer) {
        audience.add(player.uniqueId)
    }

    fun set(players: Collection<OfflinePlayer>) {
        audience.clear()
        players.forEach {
            audience.add(it.uniqueId)
        }
    }

    fun getUUID(): Collection<UUID> {
        return audience
    }

}