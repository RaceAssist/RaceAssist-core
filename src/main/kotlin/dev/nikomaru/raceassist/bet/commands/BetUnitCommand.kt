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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.*
import cloud.commandframework.annotations.specifier.Range
import dev.nikomaru.raceassist.data.files.BetSettingData
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetUnitCommand {

    @CommandPermission("raceassist.commands.bet.unit")
    @CommandMethod("unit <raceId> <unit>")
    @CommandDescription("最小の賭け単位を設定します")
    suspend fun setUnit(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "unit") @Range(min = "1", max = "100000") unit: Int) {
        val locale = sender.locale()
        if (Utils.returnCanRaceSetting(raceId, sender)) return
        BetSettingData.setBetUnit(raceId, unit)
        sender.sendMessage(Lang.getComponent("change-bet-unit-message", locale, raceId, unit))
    }
}