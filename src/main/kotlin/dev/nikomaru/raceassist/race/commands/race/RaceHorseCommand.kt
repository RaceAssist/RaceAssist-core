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
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.Lang
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
    suspend fun addHorse(sender: CommandSender, @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
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

        RaceSettingData.setHorse(raceId, owner.uniqueId, horse.uniqueId)
        sender.sendRichMessage("<color:green> $raceId に ${owner.name} の馬を追加しました。")
    }

    @CommandMethod("remove <operateRaceId> <playerName>")
    suspend fun removeHorse(sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        val locale = sender.locale()
        val player = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        RaceSettingData.removeHorse(raceId, player.uniqueId)
        sender.sendRichMessage("<color:green> $raceId から ${player.name} の馬を削除しました。")
    }

    @CommandMethod("list <operateRaceId>")
    suspend fun listHorse(sender: CommandSender, @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String) {
        val locale = sender.locale()
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        val horses = RaceSettingData.getHorse(raceId)
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
    suspend fun deleteHorse(sender: CommandSender, @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String) {
        val locale = sender.locale()
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        RaceSettingData.deleteHorse(raceId)
        sender.sendRichMessage("<color:green> $raceId の馬を全て削除しました。")
    }

}