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
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.event.BetGuiClickEvent.Companion.getBetOwner
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.milkbowl.vault.economy.Economy
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist bet")
class BetRevertCommand {
    @CommandPermission("RaceAssist.commands.bet.revert")
    @CommandMethod("revert <raceId>")
    fun revert(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        val eco: Economy = VaultAPI.getEconomy()
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (returnRaceSetting(raceId, sender)) return@withContext
                if (eco.getBalance(sender) < getBetSum(raceId)) {
                    sender.sendMessage(Lang.getComponent("no-have-money", sender.locale()))
                    return@withContext
                }
            }

            if (canRevert[sender.uniqueId] == true) {

                newSuspendedTransaction(Dispatchers.IO) {

                    BetList.select { BetList.raceId eq raceId }.forEach {
                        val receiver = getBetOwner(raceId)
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

    private suspend fun getBetSum(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.select { BetList.raceId eq raceId }.sumOf {
            it[BetList.betting]
        }
    }

    companion object {
        val canRevert: HashMap<UUID, Boolean> = HashMap()
    }
}