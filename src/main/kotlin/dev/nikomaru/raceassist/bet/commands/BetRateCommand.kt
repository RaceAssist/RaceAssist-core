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

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Range
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@CommandMethod("ra|RaceAssist bet")
class BetRateCommand {
    @CommandPermission("RaceAssist.commands.bet.rate")
    @CommandMethod("rate <raceId> <rate>")
    fun setRate(sender: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "rate") @Range(min = "0", max = "100") rate: Int) {
        RaceAssist.plugin.launch {
            if (returnRaceSetting(raceId, sender)) return@launch
            if (rate !in 1..100) {
                sender.sendMessage(Lang.getComponent("set-rate-message-in1-100", sender.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceId eq raceId }) {
                    it[returnPercent] = rate
                }
            }
        }
        sender.sendMessage(Lang.getComponent("change-bet-rate-message", sender.locale(), raceId, rate))

    }

}