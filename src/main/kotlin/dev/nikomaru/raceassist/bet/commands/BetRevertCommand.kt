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
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist bet")
class BetRevertCommand {
    @CommandPermission("RaceAssist.commands.bet.revert")
    @CommandMethod("revert <raceId>")
    fun revert(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        val eco: Economy = VaultAPI.getEconomy()
        RaceAssist.plugin.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceId)) {
                    sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
                    return@withContext
                }
                if (returnRaceSetting(raceId, sender)) return@withContext
                if (eco.getBalance(sender) < getBetSum(raceId)) {
                    sender.sendMessage(Lang.getComponent("no-have-money", sender.locale()))
                    return@withContext
                }
            }

            if (canRevert[sender.uniqueId] == true) {

                newSuspendedTransaction(Dispatchers.IO) {

                    BetList.select { BetList.raceId eq raceId }.forEach {
                        val receiver = Bukkit.getOfflinePlayer(UUID.fromString(it[BetList.playerUUID].toString()))
                        withContext(minecraft) {
                            eco.withdrawPlayer(sender, it[BetList.betting].toDouble())
                            eco.depositPlayer(receiver, it[BetList.betting].toDouble())
                        }
                        sender.sendMessage(Lang.getComponent("bet-revert-return-message-owner", sender.locale(), receiver.name, it[BetList.betting]))

                        if (receiver.isOnline) {
                            (receiver as Player).sendMessage(Lang.getComponent("bet-revert-return-message-player",
                                receiver.locale(),
                                sender.name,
                                it[BetList.raceId],
                                it[BetList.jockey],
                                it[BetList.betting]))
                        }
                    }
                    BetList.deleteWhere { BetList.raceId eq raceId }
                }
                sender.sendMessage(Lang.getComponent("bet-revert-complete-message", sender.locale(), raceId))
            } else {
                canRevert[sender.uniqueId] = true
                sender.sendMessage(Lang.getComponent("bet-revert-race-confirm-message", sender.locale(), raceId))
                delay(5000)
                canRevert.remove(sender.uniqueId)
            }
        }
    }

    private suspend fun raceExist(raceId: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceId eq raceId }.count() > 0
        }
        return exist
    }

    private suspend fun getBetSum(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.select { BetList.raceId eq raceId }.sumOf {
            it[BetList.betting]
        }
    }

    companion object {
        val canRevert: HashMap<UUID, Boolean> = HashMap()
    }
}