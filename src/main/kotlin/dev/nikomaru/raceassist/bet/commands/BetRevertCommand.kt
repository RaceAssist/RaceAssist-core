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
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
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
    suspend fun revert(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        val eco: Economy = VaultAPI.getEconomy()
        val owner = RaceSettingData.getOwner(raceId)
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()
        withContext(Dispatchers.IO) {
            if (returnRaceSetting(raceId, sender)) return@withContext
            if (eco.getBalance(owner) < getBetSum(raceId)) {
                sender.sendMessage(Lang.getComponent("no-have-money", locale))
                return@withContext
            }
        }



        newSuspendedTransaction(Dispatchers.IO) {

            BetList.select { BetList.raceId eq raceId }.forEach {
                val receiver = Bukkit.getOfflinePlayer(UUID.fromString(it[BetList.playerUUID]))
                withContext(minecraft) {
                    eco.withdrawPlayer(owner, it[BetList.betting].toDouble())
                    eco.depositPlayer(receiver, it[BetList.betting].toDouble())
                }
                sender.sendMessage(Lang.getComponent("bet-revert-return-message-owner", locale, receiver.name, it[BetList.betting]))
                if (receiver.isOnline) {
                    (receiver as Player).sendMessage(Lang.getComponent("bet-revert-return-message-player",
                        receiver.locale(),
                        owner.name,
                        it[BetList.raceId],
                        it[BetList.jockey],
                        it[BetList.betting]))
                }
            }
            BetList.deleteWhere { BetList.raceId eq raceId }
        }
        sender.sendMessage(Lang.getComponent("bet-revert-complete-message", locale, raceId))

    }

    private suspend fun getBetSum(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.select { BetList.raceId eq raceId }.sumOf {
            it[BetList.betting]
        }
    }
}
