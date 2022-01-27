/*
 * Copyright © 2022 Nikomaru <nikomaru@nikomaru.dev>
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

import net.kyori.adventure.text.Component.text
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.text.MessageFormat
import java.util.*

class RaceAudience {

    private val audience: HashSet<UUID> = HashSet()

    fun showTitleI18n(key: String, vararg args: String) {
        audience.forEach {
            val offlinePlayer = Bukkit.getOfflinePlayer(it)
            if (offlinePlayer.isOnline) {
                val player = offlinePlayer.player!!
                player.showTitle(title(text(MessageFormat.format(Lang.getText(key, player.locale()), *args)), text(" ")))
            }
        }
    }

    fun sendMessageI18n(key: String, vararg args: String) {
        audience.forEach {
            val offlinePlayer = Bukkit.getOfflinePlayer(it)
            if (offlinePlayer.isOnline) {
                val player = offlinePlayer.player!!
                player.sendMessage(text(MessageFormat.format(Lang.getText(key, player.locale()), *args)))
            }
        }
    }

    fun add(player: OfflinePlayer) {
        audience.add(player.uniqueId)
    }

    fun getUUID(): Set<UUID> {
        return audience
    }

}