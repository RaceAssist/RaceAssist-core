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

package dev.nikomaru.raceassist.race.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.text.MessageFormat
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("player")
class PlayerCommand : BaseCommand() {

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("add")
    @CommandCompletion("@RaceID @players")
    private fun addPlayer(player: Player, raceID: String, @Single onlinePlayer: OnlinePlayer) {

        plugin!!.launch {
            val jockey = onlinePlayer.player
            if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(
                    Component.text(
                        Lang.getText("only-race-creator-can-setting", player.locale()),
                        TextColor.color(NamedTextColor.RED)
                    )
                )
                return@launch
            }
            if (getRacePlayerExist(raceID, jockey.uniqueId)) {
                player.sendMessage(Component.text(Lang.getText("already-exist-this-user", player.locale()), TextColor.color(NamedTextColor.YELLOW)))
                return@launch
            }
            if (getRacePlayerAmount() >= 8) {
                player.sendMessage(Component.text(Lang.getText("max-player-is-eight", player.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.insert {
                    it[this.raceID] = raceID
                    it[playerUUID] = jockey.uniqueId.toString()
                }
            }
            player.sendMessage(MessageFormat.format(Lang.getText("player-add-to-race-group", player.locale()), player.name, raceID))
        }
    }

    private suspend fun getRacePlayerAmount(): Long = newSuspendedTransaction {
        PlayerList.select {
            PlayerList.raceID eq "raceID"
        }.count()
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("remove")
    @CommandCompletion("@RaceID")
    private fun removePlayer(sender: Player, @Single raceID: String, @Single onlinePlayer: OnlinePlayer) {
        plugin!!.launch {
            if (RaceCommand.getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(Component.text(Lang.getText("only-race-creator-can-delete", sender.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }

            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.deleteWhere { (PlayerList.raceID eq raceID) and (PlayerList.playerUUID eq onlinePlayer.player.uniqueId.toString()) }
            }
            sender.sendMessage(MessageFormat.format(Lang.getText("to-delete-player-from-race-group", sender.locale()), raceID))
        }
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    private fun deletePlayer(sender: Player, @Single raceID: String) {
        plugin!!.launch {
            if (RaceCommand.getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(Component.text(Lang.getText("only-race-creator-can-delete", sender.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }

            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.deleteWhere { PlayerList.raceID eq raceID }
            }
            sender.sendMessage(MessageFormat.format(Lang.getText("to-delete-all-player-from-race-group", sender.locale()), raceID))
        }
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("list")
    @CommandCompletion("@RaceID")
    private fun displayPlayerList(sender: Player, @Single raceID: String) {
        plugin!!.launch {
            if (RaceCommand.getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(
                    Component.text(
                        Lang.getText("only-race-creator-can-display", sender.locale()),
                        TextColor.color(NamedTextColor.RED)
                    )
                )
                return@launch
            }

            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.select { PlayerList.raceID eq raceID }.forEach {
                    sender.sendMessage(
                        Component.text(
                            Bukkit.getOfflinePlayer(
                                UUID.fromString(
                                    it[PlayerList.playerUUID]
                                )
                            ).name.toString(), TextColor.color(NamedTextColor.YELLOW)
                        )
                    )
                }
            }
        }
    }

    private suspend fun getRacePlayerExist(RaceID: String, playerUUID: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        PlayerList.select { (PlayerList.raceID eq RaceID) and (PlayerList.playerUUID eq playerUUID.toString()) }.count() > 0
    }
}
