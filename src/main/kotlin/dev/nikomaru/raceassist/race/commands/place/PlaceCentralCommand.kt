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

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.Utils.canSetCentral
import dev.nikomaru.raceassist.utils.Utils.centralPlaceId
import dev.nikomaru.raceassist.utils.i18n.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceCentralCommand {
    @CommandPermission("raceassist.commands.place.central")
    @CommandMethod("central <operatePlaceId>")
    suspend fun central(sender: CommandSender, @Argument(value = "operatePlaceId", suggestions = "operatePlaceId") placeId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }

        if (!RaceUtils.hasPlaceControlPermission(placeId, sender)) return
        canSetCentral[sender.uniqueId] = true
        centralPlaceId[sender.uniqueId] = placeId
        sender.sendMessage(Lang.getComponent("to-set-central-point", sender.locale()))

    }
}