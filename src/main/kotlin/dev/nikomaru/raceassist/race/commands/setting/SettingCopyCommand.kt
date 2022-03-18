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
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.CommandUtils.getOwner
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.RaceStaffUtils
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist setting")
class SettingCopyCommand {

    @CommandPermission("RaceAssist.commands.setting.copy")
    @CommandMethod("copy <raceId_1> <raceId_2>")
    fun copy(sender: CommandSender,
        @Argument(value = "raceId_1", suggestions = "raceId") raceId_1: String,
        @Argument(value = "raceId_2") raceId_2: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        plugin.launch {
            if (getOwner(raceId_2) != null) {
                sender.sendMessage(Lang.getComponent("already-used-the-name-race", sender.locale()))
                return@launch
            }
            if (!CommandUtils.getCircuitExist(raceId_1, true) || !CommandUtils.getCircuitExist(raceId_1, false)) {
                sender.sendMessage(Lang.getComponent("no-exist-race", sender.locale()))
                return@launch
            }
            CommandUtils.getCentralPoint(raceId_1, true) ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-central-point",
                sender.locale()))
            CommandUtils.getCentralPoint(raceId_1, false) ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-central-point",
                sender.locale()))
            CommandUtils.getGoalDegree(raceId_1) ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-goal-degree", sender.locale()))

            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.select { BetSetting.raceId eq raceId_1 }.forEach { before ->
                    BetSetting.insert { after ->
                        after[raceId] = raceId_2
                        after[creator] = sender.uniqueId.toString()
                        after[canBet] = false
                        after[returnPercent] = before[returnPercent]
                        after[spreadsheetId] = null
                    }
                }
                CircuitPoint.select { CircuitPoint.raceId eq raceId_1 }.forEach { before ->
                    CircuitPoint.insert { after ->
                        after[raceId] = raceId_2
                        after[inside] = before[inside]
                        after[XPoint] = before[XPoint]
                        after[YPoint] = before[YPoint]
                    }
                }
                RaceList.select { RaceList.raceId eq raceId_1 }.forEach { before ->
                    RaceList.insert { after ->
                        after[raceId] = raceId_2
                        after[creator] = sender.uniqueId.toString()
                        after[reverse] = before[reverse]
                        after[lap] = before[lap]
                        after[centralXPoint] = before[centralXPoint]
                        after[centralYPoint] = before[centralYPoint]
                        after[goalDegree] = before[goalDegree]
                    }

                }
            }
            RaceStaffUtils.addStaff(raceId_2, sender.uniqueId)
        }
    }
}