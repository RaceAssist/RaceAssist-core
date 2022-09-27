/*
 *     Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 *
 *     This program is free software: you can redistribute it and/or modify
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

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.race.RaceJudgement
import dev.nikomaru.raceassist.utils.Utils
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist race")
class RaceStartCommand {

    @CommandPermission("raceassist.commands.race.start")
    @CommandMethod("start <raceId> [raceUniqueId]")
    suspend fun start(
        sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "raceUniqueId") raceUniqueId: String?,
    ) {
        if (Utils.returnCanRaceSetting(raceId, sender)) return
        val raceJudgement = RaceJudgement(raceId, sender, raceUniqueId)
        if (!raceJudgement.setting()) {
            return
        }
        raceJudgement.start()
        while (raceJudgement.finished) {
            raceJudgement.calculate()
            raceJudgement.display()
        }
        raceJudgement.last()
        if (!raceJudgement.suspend) {
            raceJudgement.payDividend()
        }
    }

}

