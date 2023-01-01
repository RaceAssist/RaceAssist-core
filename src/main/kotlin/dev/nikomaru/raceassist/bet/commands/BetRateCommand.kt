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
import dev.nikomaru.raceassist.data.files.RaceUtils.hasRaceControlPermission
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.i18n.Lang
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetRateCommand {
    @CommandPermission("raceassist.commands.bet.rate")
    @CommandMethod("rate <operateRaceId> <rate>")
    @CommandDescription("レースの賭けのレートを設定します")
    suspend fun setRate(sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "rate") @Range(min = "0", max = "100") rate: Int) {
        val locale = sender.locale()
        if (!hasRaceControlPermission(raceId, sender)) return
        BetSettingData.setReturnPercent(raceId, rate)
        sender.sendMessage(Lang.getComponent("change-bet-rate-message", locale, raceId, rate))
    }
}