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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Range
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@CommandMethod("ra|RaceAssist place")
class PlaceLapCommand {
    @CommandPermission("RaceAssist.commands.place.lap")
    @CommandMethod("lap <raceId> <lap>")
    fun setLap(sender: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "lap") @Range(min = "1", max = "100") lap: Int) {
        RaceAssist.plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch

            if (lap < 1) {
                sender.sendMessage(Lang.getComponent("to-need-enter-over-1", sender.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.update({ RaceList.raceId eq raceId }) {
                    it[this.lap] = lap
                }
            }
            sender.sendMessage(Lang.getComponent("to-set-lap", sender.locale()))
        }
    }
}