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
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.event.Lang
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetUnitCommand {

    @CommandPermission("raceassist.commands.bet.unit")
    @CommandMethod("unit <operateRaceId> <unit>")
    @CommandDescription("最小の賭け単位を設定します")
    fun setUnit(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "unit") @Range(min = "1", max = "100000") unit: Int
    ) {
        val locale = sender.locale()
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        val betManager = RaceAssist.api.getBetManager(raceId)
            ?: return sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
        betManager.setBetUnit(unit)
        sender.sendMessage(Lang.getComponent("change-bet-unit-message", locale, raceId, unit))
    }
}