/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.Regex
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.Lang.sendI18nRichMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceCreateCommand {

    @CommandPermission("raceassist.commands.place.reverse")
    @CommandMethod("create <placeId>")
    suspend fun reverse(
        sender: CommandSender,
        @Argument(value = "placeId") @Regex(value = "[^_]+_\\d+$") placeId: String
    ) {
        if (sender !is Player) {
            sender.sendI18nRichMessage("only-player-can-do-this")
            return
        }

        RaceAssist.api.getDataManager().createPlace(placeId, sender)


        sender.sendI18nRichMessage("place-created", placeId)

    }
}