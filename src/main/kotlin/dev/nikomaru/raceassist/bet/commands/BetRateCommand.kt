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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Range
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.text.MessageFormat

@CommandMethod("ra|RaceAssist bet")
class BetRateCommand {
    @CommandMethod("rate <raceId> <rate>")
    fun setRate(player: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceID: String,
        @Argument(value = "rate") @Range(min = "0", max = "100") rate: Int) {
        RaceAssist.plugin.launch {
            if (!raceExist(raceID)) {
                player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                return@launch
            }
            if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                return@launch
            }
            if (rate !in 1..100) {
                player.sendMessage(Lang.getText("set-rate-message-in1-100", player.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[returnPercent] = rate
                }
            }
        }
        player.sendMessage(MessageFormat.format(Lang.getText("change-bet-rate-message", player.locale()), raceID, rate))

    }

    private suspend fun getRaceCreator(raceID: String) =
        newSuspendedTransaction(Dispatchers.IO) { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] }

    private suspend fun raceExist(raceID: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceID eq raceID }.count() > 0
        }
        return exist
    }
}