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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.*
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist bet")
class BetDeleteCommand {
    @CommandPermission("RaceAssist.commands.bet.delete")
    @CommandMethod("delete <raceId>")
    fun delete(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch
            if (canDelete[sender.uniqueId] == true) {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetList.deleteWhere { BetList.raceId eq raceId }
                }
                sender.sendMessage(Lang.getComponent("bet-remove-race", sender.locale(), raceId))
            } else {
                canDelete[sender.uniqueId] = true
                sender.sendMessage(Lang.getComponent("bet-remove-race-confirm-message", sender.locale(), raceId))
                delay(5000)
                canDelete.remove(sender.uniqueId)
            }
        }
    }

    companion object {
        val canDelete: HashMap<UUID, Boolean> = HashMap()
    }
}