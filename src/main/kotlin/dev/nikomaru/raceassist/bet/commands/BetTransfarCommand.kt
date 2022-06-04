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
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.*

@CommandMethod("ra|RaceAssist bet")
class BetTransfarCommand {
    @CommandPermission("RaceAssist.commands.bet.transfar")
    @CommandMethod("transfar <raceId> <playerName>")
    fun transfer(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (CommandUtils.returnRaceSetting(raceId, sender)) return@withContext
            }
            val player: OfflinePlayer? = Bukkit.getOfflinePlayerIfCached(playerName)

            val locale = if (sender is Player) sender.locale() else Locale.getDefault()
            if (player == null) {
                sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
                return@launch
            }

            if (!RaceStaffUtils.existStaff(raceId, player.uniqueId)) {
                sender.sendMessage(Lang.getComponent("cant-set-not-staff", locale))
                return@launch
            }

            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceId eq raceId }) {
                    it[creator] = player.uniqueId.toString()
                }
            }
        }
    }
}