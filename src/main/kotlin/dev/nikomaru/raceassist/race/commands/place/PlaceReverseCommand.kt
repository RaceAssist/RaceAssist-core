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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.manager.PlaceManager
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceReverseCommand {
    @CommandPermission("raceassist.commands.place.reverse")
    @CommandMethod("reverse <operatePlaceId>")
    fun reverse(
        sender: CommandSender,
        @Argument(value = "operatePlaceId", suggestions = SuggestionId.OPERATE_PLACE_ID) placeId: String
    ) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val placeManager = RaceAssist.api.getPlaceManager(placeId) as PlaceManager.PlainPlaceManager?
        if (placeManager?.senderHasControlPermission(sender) != true) return

        placeManager.setReverse(!placeManager.getReverse())

        sender.sendMessage(Lang.getComponent("to-change-race-orientation", sender.locale()))

    }

}