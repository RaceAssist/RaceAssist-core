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
class BetCanCommand {

    @CommandMethod("can <raceId> <type>")
    fun setCanBet(player: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceID: String,
        @Argument(value = "type", suggestions = "betType") type: String) {
        RaceAssist.plugin.launch {
            if (!raceExist(raceID)) {
                player.sendMessage(MessageFormat.format(Lang.getText("no-exist-this-raceid-race", player.locale()), raceID))
                return@launch
            }
            if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                return@launch
            }
            if (type == "on") {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.update({ BetSetting.raceID eq raceID }) {
                        it[canBet] = true
                    }
                }
                player.sendMessage(MessageFormat.format(Lang.getText("can-bet-this-raceid", player.locale()), raceID))
            } else if (type == "off") {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.update({ BetSetting.raceID eq raceID }) {
                        it[canBet] = false
                    }
                }
                player.sendMessage(MessageFormat.format(Lang.getText("cannot-bet-this-raceid", player.locale()), raceID))
            }
        }
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