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

package dev.nikomaru.raceassist.race.commands.audience

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.audience
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist audience")
class AudienceJoinCommand {
    @CommandPermission("raceassist.commands.audience.join")
    @CommandMethod("join <raceId>")
    suspend fun join(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }

        if (!RaceSettingData.existsRace(raceId)) {
            sender.sendMessage(Lang.getComponent("not-found-this-race", sender.locale()))
            return
        }
        if (audience[raceId]?.contains(sender.uniqueId) == true) {
            sender.sendMessage(Lang.getComponent("already-joined", sender.locale()))
            return
        }
        if (!audience.containsKey(raceId)) {
            audience[raceId] = ArrayList()
        }
        audience[raceId]?.add(sender.uniqueId)
        sender.sendMessage(Lang.getComponent("joined-group", sender.locale()))

    }
}