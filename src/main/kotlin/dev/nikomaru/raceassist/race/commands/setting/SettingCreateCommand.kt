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

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.utils.CommandUtils.getOwner
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.RaceStaffUtils
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist setting")
class SettingCreateCommand {
    @CommandPermission("RaceAssist.commands.setting.create")
    @CommandMethod("create <raceId>")
    fun create(sender: CommandSender, @Argument(value = "raceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        plugin.launch {
            if (getOwner(raceId) != null) {
                sender.sendMessage(Lang.getComponent("already-used-the-name-race", sender.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.insert {
                    it[this.raceId] = raceId
                    it[this.creator] = sender.uniqueId.toString()
                    it[this.reverse] = false
                    it[this.lap] = 1
                    it[this.centralXPoint] = null
                    it[this.centralYPoint] = null
                    it[this.goalDegree] = null
                }
                BetSetting.insert {
                    it[this.raceId] = raceId
                    it[this.canBet] = false
                    it[this.returnPercent] = 75
                    it[this.creator] = sender.uniqueId.toString()
                    it[this.spreadsheetId] = null
                }
            }
            RaceStaffUtils.addStaff(raceId, sender.uniqueId)
            sender.sendMessage(Lang.getComponent("to-create-race", sender.locale()))
        }
    }
}