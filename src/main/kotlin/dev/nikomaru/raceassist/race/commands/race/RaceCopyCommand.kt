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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.CommandUtils
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist race")
class RaceCopyCommand {

    @CommandPermission("RaceAssist.commands.race.copy")
    @CommandMethod("delete <raceId_1> <raceId_2>")
    fun copy(sender: Player, @Argument(value = "raceId_1", suggestions = "raceId") raceId_1: String, @Argument(value = "raceId_2") raceId_2: String) {
        plugin.launch {
            if (CommandUtils.getRaceCreator(raceId_2) != null) {
                sender.sendMessage(Lang.getText("already-used-the-name-race", sender.locale()))
                return@launch
            }
            if (!CommandUtils.getCircuitExist(raceId_1, true) || !CommandUtils.getCircuitExist(raceId_1, false)) {
                sender.sendMessage(Component.text(Lang.getText("no-exist-race", sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
                return@launch
            }
            CommandUtils.getCentralPoint(raceId_1, true) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
            CommandUtils.getCentralPoint(raceId_1, false) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
            CommandUtils.getGoalDegree(raceId_1) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-goal-degree",
                sender.locale()), TextColor.color(NamedTextColor.YELLOW)))

            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.select { BetSetting.raceID eq raceId_1 }.forEach { before ->
                    BetSetting.insert { after ->
                        after[raceID] = raceId_2
                        after[creator] = sender.uniqueId.toString()
                        after[canBet] = false
                        after[returnPercent] = before[returnPercent]
                        after[spreadsheetId] = null
                    }
                }
                CircuitPoint.select { CircuitPoint.raceID eq raceId_1 }.forEach { before ->
                    CircuitPoint.insert { after ->
                        after[raceID] = raceId_2
                        after[inside] = before[inside]
                        after[XPoint] = before[XPoint]
                        after[YPoint] = before[YPoint]
                    }
                }
                RaceList.select { RaceList.raceID eq raceId_1 }.forEach { before ->
                    RaceList.insert { after ->
                        after[raceID] = raceId_2
                        after[creator] = sender.uniqueId.toString()
                        after[reverse] = before[reverse]
                        after[lap] = before[lap]
                        after[centralXPoint] = before[centralXPoint]
                        after[centralYPoint] = before[centralYPoint]
                        after[goalDegree] = before[goalDegree]
                    }

                }
            }
        }
    }
}
