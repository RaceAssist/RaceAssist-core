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

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Single
import co.aikar.commands.annotation.Subcommand
import dev.nikomaru.raceassist.database.BetSetting
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

@CommandAlias("ra|RaceAssist")
@Subcommand("bet")
class SetBetCommand : BaseCommand() {

    @Subcommand("can")
    @CommandCompletion("@RaceID on|off")
    fun setCanBet(player: Player, @Single raceID: String, @Single type: String) {
        if (transaction { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] } != player.uniqueId.toString()) {
            player.sendMessage("ほかのプレイヤーのレースを設定することはできません")
        }
        if (type == "on") {
            transaction {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[canBet] = true
                }
            }
            player.sendMessage("${raceID}のレースにはベットが可能になりました")
        } else if (type == "off") {
            transaction {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[canBet] = false
                }
            }
            player.sendMessage("${raceID}のレースにはベットが不可能になりました")
        }
    }

    @Subcommand("rate")
    @CommandCompletion("@RaceID ")
    fun setRate(player: Player, @Single raceID: String, @Single rate: Int) {
        if (transaction { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] } != player.uniqueId.toString()) {
            player.sendMessage("ほかのプレイヤーのレースを設定することはできません")
        }
        if (rate !in 1..100) {
            player.sendMessage("1から100までの数字を入力してください")
            return
        }
        transaction {
            BetSetting.update({ BetSetting.raceID eq raceID }) {
                it[returnPercent] = rate
            }
        }
        player.sendMessage("${raceID}のレースのベットレートを${rate}に設定しました")
    }
}