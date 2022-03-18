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
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.utils.CommandUtils
import kotlinx.coroutines.Dispatchers
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist player")
class PlayerListCommand {

    @CommandPermission("RaceAssist.commands.player.list")
    @CommandMethod("list <raceId>")
    private fun displayPlayerList(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        RaceAssist.plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch

            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.select { PlayerList.raceId eq raceId }.forEach {
                    sender.sendMessage(Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])).name.toString())
                }
            }
        }
    }
}