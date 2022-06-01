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
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist player")
class PlayerDeleteCommand {

    @CommandPermission("RaceAssist.commands.player.delete")
    @CommandMethod("delete <raceId>")
    private fun deletePlayer(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {

        plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch

            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.deleteWhere { PlayerList.raceId eq raceId }
            }
            val locale = if (sender is Player) sender.locale() else Locale.getDefault()
            sender.sendMessage(Lang.getComponent("to-delete-all-player-from-race-group", locale, raceId))
        }
    }
}