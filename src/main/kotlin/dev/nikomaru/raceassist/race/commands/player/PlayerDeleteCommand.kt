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

package dev.nikomaru.raceassist.race.commands.player

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist player")
class PlayerDeleteCommand {

    @CommandPermission("raceassist.commands.player.delete")
    @CommandMethod("delete <raceId>")
    suspend fun deletePlayer(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {

        if (Utils.returnCanRaceSetting(raceId, sender)) return

        RaceSettingData.getJockeys(raceId).forEach {
            RaceSettingData.removeJockey(raceId, it)
        }
        val locale = sender.locale()
        sender.sendMessage(Lang.getComponent("to-delete-all-player-from-race-group", locale, raceId))

    }
}