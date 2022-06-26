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
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist bet")
class BetRemoveCommand {
    @CommandPermission("RaceAssist.commands.bet.revert")
    @CommandMethod("remove <raceId> <betId>")
    fun remove(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String, @Argument(value = "betId") betId: Int) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val eco: Economy = VaultAPI.getEconomy()
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (returnRaceSetting(raceId, sender)) return@withContext
                if (eco.getBalance(sender) < getBetSum(raceId)) {
                    sender.sendMessage(Lang.getComponent("no-have-money", sender.locale()))
                    return@withContext
                }
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { (BetList.rowNum eq betId) and (BetList.raceId eq raceId) }.forEach {
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

                BetList.deleteWhere { (BetList.rowNum eq betId) and (BetList.raceId eq raceId) }
            }

        }
    }

    private suspend fun getBetSum(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.select { BetList.raceId eq raceId }.sumOf {
            it[BetList.betting]
        }
    }
}