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

package dev.nikomaru.raceassist.race.commands.player

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRacePlayerAmount
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRacePlayerExist
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.text.MessageFormat

@CommandMethod("ra|RaceAssist player")
class PlayerAddCommand {

    @CommandPermission("RaceAssist.commands.player.add")
    @CommandMethod("add <raceId> <playerName>")
    private fun addPlayer(player: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceID: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {

        val jockey: OfflinePlayer = Bukkit.getOfflinePlayer(playerName)

        if (!jockey.hasPlayedBefore()) {
            player.sendMessage(Lang.getText("player-add-not-exist", player.locale()))
            return
        }

        RaceAssist.plugin.launch {
            if (getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(Component.text(Lang.getText("only-race-creator-can-setting", player.locale()),
                    TextColor.color(NamedTextColor.RED)))
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
}