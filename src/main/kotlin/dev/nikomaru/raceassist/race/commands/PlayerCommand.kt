/*
 * Copyright © 2021 Nikomaru <nikomaru@nikomaru.dev>
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
import dev.nikomaru.raceassist.database.PlayerList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("player")
class PlayerCommand : BaseCommand() {

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("add")
    @CommandCompletion("@RaceID @players")
    private fun addPlayer(sender: Player, raceID: String, @Single onlinePlayer: OnlinePlayer) {

        val player: Player = onlinePlayer.player
        if (RaceCommand.getRaceCreator(raceID) != sender.uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか追加することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }
        if (getRacePlayerExist(raceID, player.uniqueId)) {
            sender.sendMessage(Component.text("すでにそのプレイヤーは既に存在します", TextColor.color(NamedTextColor.YELLOW)))
            return
        }
        transaction {
            PlayerList.insert {
                it[this.raceID] = raceID
                it[playerUUID] = player.uniqueId.toString()
            }
        }
        sender.sendMessage("${player.name} を $raceID に追加しました ")
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("remove")
    @CommandCompletion("@RaceID")
    private fun removePlayer(sender: CommandSender, @Single raceID: String, @Single onlinePlayer: OnlinePlayer) {
        if (RaceCommand.getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか削除することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }

        transaction {
            PlayerList.deleteWhere { (PlayerList.raceID eq raceID) and (PlayerList.playerUUID eq onlinePlayer.player.uniqueId.toString()) }
        }
        sender.sendMessage("$raceID から対象のプレイヤーを削除しました")
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    private fun deletePlayer(sender: CommandSender, @Single raceID: String) {
        if (RaceCommand.getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか削除することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }

        transaction {
            PlayerList.deleteWhere { PlayerList.raceID eq raceID }
        }
        sender.sendMessage("$raceID から全てのプレイヤーを削除しました")
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("list")
    @CommandCompletion("@RaceID")
    private fun displayPlayerList(sender: CommandSender, @Single raceID: String) {
        if (RaceCommand.getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか表示することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }

        transaction {
            PlayerList.select { PlayerList.raceID eq raceID }.forEach {
                sender.sendMessage(
                    Component.text(
                        Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])).name.toString(), TextColor.color
                            (
                            NamedTextColor
                                .YELLOW
                        )
                    )
                )
            }
        }
    }

    private fun getRacePlayerExist(RaceID: String, playerUUID: UUID): Boolean {
        var playerExist = false

        transaction {
            playerExist = PlayerList.select { (PlayerList.raceID eq RaceID) and (PlayerList.playerUUID eq playerUUID.toString()) }.count() > 0
        }
        return playerExist
    }
}