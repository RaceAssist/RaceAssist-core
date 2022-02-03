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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist race")
class RaceCreateCommand {
    @CommandPermission("RaceAssist.commands.race")
    @CommandMethod("create <raceId>")
    fun create(sender: CommandSender, @Argument(value = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (getRaceCreator(raceID) != null) {
                sender.sendMessage(Lang.getText("already-used-the-name-race", (sender as Player).locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.insert {
                    it[this.raceID] = raceID
                    it[this.creator] = (sender as Player).uniqueId.toString()
                    it[this.reverse] = false
                    it[this.lap] = 1
                    it[this.centralXPoint] = null
                    it[this.centralYPoint] = null
                    it[this.goalDegree] = null
                }
                BetSetting.insert {
                    it[this.raceID] = raceID
                    it[this.canBet] = false
                    it[this.returnPercent] = 75
                    it[this.creator] = (sender as Player).uniqueId.toString()
                    it[this.spreadsheetId] = null
                }
            }
            RaceAssist.setRaceID()
            sender.sendMessage(Lang.getText("to-create-race", (sender as Player).locale()))
        }
    }
}