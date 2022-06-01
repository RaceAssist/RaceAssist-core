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

package dev.nikomaru.raceassist.race.commands.player

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.CommandUtils.getRacePlayerAmount
import dev.nikomaru.raceassist.utils.CommandUtils.getRacePlayerExist
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist player")
class PlayerAddCommand {

    @CommandPermission("RaceAssist.commands.player.add")
    @CommandMethod("add <raceId> <playerName>")
    private fun addPlayer(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {

        val jockey: OfflinePlayer = Bukkit.getOfflinePlayer(playerName)

        val locale = if (sender is Player) sender.locale() else Locale.getDefault()
        if (!jockey.hasPlayedBefore()) {
            sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
            return
        }

        plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch
            if (getRacePlayerExist(raceId, jockey.uniqueId)) {
                sender.sendMessage(Lang.getComponent("already-exist-this-user", locale))
                return@launch
            }
            if (getRacePlayerAmount(raceId) > 7) {
                sender.sendMessage(Lang.getComponent("max-player-is-eight", locale))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.insert {
                    it[this.raceId] = raceId
                    it[playerUUID] = jockey.uniqueId.toString()
                }
            }
            sender.sendMessage(Lang.getComponent("player-add-to-race-group", locale, jockey.name.toString(), raceId))
        }
    }
}