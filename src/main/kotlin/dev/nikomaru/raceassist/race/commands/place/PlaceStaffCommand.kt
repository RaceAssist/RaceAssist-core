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
import dev.nikomaru.raceassist.data.files.*
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandMethod("ra|raceassist place")
@CommandPermission("raceassist.commands.place.staff")

class PlaceStaffCommand {
    @CommandMethod("staff add <operatePlaceId> <playerName>")
    suspend fun addStaff(sender: CommandSender,
        @Argument(value = "operatePlaceId", suggestions = "operatePlaceId") PlaceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {

        if (!RaceUtils.hasPlaceControlPermission(PlaceId, sender)) return

        val locale = sender.locale()
        val target = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))

        if (PlaceSettingData.addStaff(PlaceId, target)) {
            sender.sendMessage(Lang.getComponent("add-staff", locale))
        } else {
            sender.sendMessage(Lang.getComponent("already-added-staff", locale))
        }

    }

    @CommandMethod("staff remove <operatePlaceId> <playerName>")
    suspend fun removeStaff(sender: CommandSender,
        @Argument(value = "operatePlaceId", suggestions = "operatePlaceId") PlaceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        if (!RaceUtils.hasPlaceControlPermission(PlaceId, sender)) return

        val locale = sender.locale()
        val target = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
        if (PlaceSettingData.getOwner(PlaceId) == target) {
            return sender.sendMessage(Lang.getComponent("cant-remove-yourself-staff", locale))
        }

        if (PlaceSettingData.removeStaff(PlaceId, target)) {
            sender.sendMessage(Lang.getComponent("delete-staff", locale))
        } else {
            sender.sendMessage(Lang.getComponent("not-find-staff", locale))
        }

    }

    @CommandMethod("staff list <operatePlaceId>")
    suspend fun listStaff(sender: CommandSender, @Argument(value = "operatePlaceId", suggestions = "operatePlaceId") PlaceId: String) {
        if (!RaceUtils.hasPlaceControlPermission(PlaceId, sender)) return
        PlaceSettingData.getStaffs(PlaceId).forEach {
            sender.sendMessage(it.name.toString())
        }

    }
}