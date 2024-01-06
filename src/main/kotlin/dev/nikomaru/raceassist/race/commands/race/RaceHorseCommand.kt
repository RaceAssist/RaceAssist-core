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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.Utils.toLivingHorse
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import java.util.*

@CommandMethod("ra|RaceAssist race horse")
@CommandPermission("raceassist.commands.race.horse")
class RaceHorseCommand {

    @CommandMethod("add <operateRaceId>")
    suspend fun addHorse(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val horse = sender.getTargetEntity(10)
        if (horse == null) {
            sender.sendMessage("There is no entity within 10 blocks.")
            return
        } else if (horse !is Horse) {
            sender.sendMessage("The entity is not a player.")
            return
        }
        val owner = horse.owner ?: return sender.sendMessage("The horse is not owned.")

        raceManager.setHorse(owner.uniqueId, horse.uniqueId)
        sender.sendRichMessage("<color:green> $raceId に ${owner.name} の馬を追加しました。")
    }

    @CommandMethod("remove <operateRaceId> <playerName>")
    suspend fun removeHorse(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "playerName", suggestions = SuggestionId.PLAYER_NAME) playerName: String
    ) {
        val locale = sender.locale()
        val player = Bukkit.getOfflinePlayerIfCached(playerName)
            ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return
        raceManager.removeHorse(player.uniqueId)
        sender.sendRichMessage("<color:green> $raceId から ${player.name} の馬を削除しました。")
    }

    @CommandMethod("list <operateRaceId>")
    suspend fun listHorse(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return
        val horses = raceManager.getHorse()
        if (horses.isEmpty()) {
            sender.sendRichMessage("<color:red> $raceId に馬が登録されていません。")
            return
        }
        sender.sendRichMessage("<color:green> $raceId の馬一覧")
        horses.forEach { (player, horse) ->
            val livingHorse = horse.toLivingHorse()
            if (livingHorse == null) {
                sender.sendRichMessage("<color:red> ${player.toOfflinePlayer().name} の馬は存在しません。")
            } else {
                sender.sendRichMessage("<color:green> ${player.toOfflinePlayer().name} : ${livingHorse.name}")
            }
        }
    }

    @CommandMethod("delete <operateRaceId>")
    suspend fun deleteHorse(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        val locale = sender.locale()
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return
        raceManager.deleteHorse()
        sender.sendRichMessage("<color:green> $raceId の馬を全て削除しました。")
    }

}