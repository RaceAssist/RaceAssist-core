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

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.*
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist setting")
class SettingPlaceIdCommand {

    @CommandPermission("raceassist.commands.setting.placeId")
    @CommandMethod("placeId <operateRaceId> <placeId>")
    suspend fun setPlaceId(sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "placeId", suggestions = "placeId") placeId: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        RaceSettingData.setPlaceId(raceId, placeId)
        sender.sendRichMessage("$raceId の placeId を $placeId に設定しました。")
    }

}