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
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.database.BetSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("bet")
class SetBetCommand : BaseCommand() {

    @Subcommand("can")
    @CommandCompletion("@RaceID on|off")
    fun setCanBet(player: Player, @Single raceID: String, @Single type: String) {
        plugin!!.launch {
            if (!raceExist(raceID)) {
                player.sendMessage("${raceID}のレースは存在しません")
                return@launch
            }
            if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                player.sendMessage("ほかのプレイヤーのレースを設定することはできません")
                return@launch
            }
            if (type == "on") {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.update({ BetSetting.raceID eq raceID }) {
                        it[canBet] = true
                    }
                }
                player.sendMessage("${raceID}のレースにはベットが可能になりました")
            } else if (type == "off") {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.update({ BetSetting.raceID eq raceID }) {
                        it[canBet] = false
                    }
                }
                player.sendMessage("${raceID}のレースにはベットが不可能になりました")
            }
        }
    }

    @Subcommand("rate")
    @CommandCompletion("@RaceID")
    fun setRate(player: Player, @Single raceID: String, @Single rate: Int) {
        plugin!!.launch {
            if (!raceExist(raceID)) {
                player.sendMessage("${raceID}のレースは存在しません")
                return@launch
            }
            if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                player.sendMessage("ほかのプレイヤーのレースを設定することはできません")
                return@launch
            }
            if (rate !in 1..100) {
                player.sendMessage("1から100までの数字を入力してください")
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[returnPercent] = rate
                }
            }
        }
        player.sendMessage("${raceID}のレースのベットレートを${rate}に設定しました")
    }

    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    fun delete(player: Player, @Single raceID: String) {
        plugin!!.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage("${raceID}のレースは存在しません")
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage("ほかのプレイヤーのレースを設定することはできません")
                    return@withContext
                }
            }


            if (canDelete[player.uniqueId] == true) {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetList.deleteWhere { BetList.raceID eq raceID }
                }
                player.sendMessage("${raceID}のレースを削除しました")
            } else {

                canDelete[player.uniqueId] = true
                player.sendMessage("${raceID}のレースを削除するには、5秒以内にもう一度同じコマンドを実行してください")
                delay(5000)
                canDelete.remove(player.uniqueId)

            }
        }
    }

    @Subcommand("list")
    @CommandCompletion("@RaceID")
    fun list(player: Player, @Single raceID: String) {
        plugin!!.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage("${raceID}のレースは存在しません")
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage("ほかのプレイヤーのレースのリストを見ることはできません")
                    return@withContext
                }
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { BetList.raceID eq raceID }.forEach {
                    player.sendMessage(
                        "${it[BetList.rowNum]} ${it[BetList.timeStamp]}   プレイヤー:${it[BetList.playerName]} ->  騎手:${
                            it[BetList.jockey]
                        } " + "${
                            it[BetList.betting]
                        }円"
                    )
                }
            }
        }
    }

    private fun getRaceCreator(raceID: String) = transaction { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] }

    private suspend fun raceExist(raceID: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceID eq raceID }.count() > 0
        }
        return exist
    }

    companion object {
        val canDelete: HashMap<UUID, Boolean> = HashMap()
    }
}