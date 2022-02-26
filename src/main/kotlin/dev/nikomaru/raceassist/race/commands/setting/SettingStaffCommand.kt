/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
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

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.CommandUtils.getOwner
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.RaceStaffUtils
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist setting")
@CommandPermission("RaceAssist.commands.setting.staff")
class SettingStaffCommand {

    @CommandMethod("staff add <raceId> <playerName>")
    fun addStaff(sender: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch
            val target = Bukkit.getOfflinePlayer(playerName)
            if (returnCanSetPlayer(target, sender, playerName)) return@launch
            if (RaceStaffUtils.addStaff(raceId, target.uniqueId)) {
                sender.sendMessage(Lang.getComponent("add-staff", sender.locale()))
            } else {
                sender.sendMessage(Lang.getComponent("already-added-staff", sender.locale()))
            }
        }
    }

    @CommandMethod("staff remove <raceId> <playerName>")
    fun removeStaff(sender: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch
            val target = Bukkit.getOfflinePlayer(playerName)
            if (returnCanSetPlayer(target, sender, playerName)) return@launch
            if (getOwner(raceId) == target.uniqueId) {
                return@launch sender.sendMessage(Lang.getComponent("cant-remove-yourself-staff", sender.locale()))
            }
            if (RaceStaffUtils.removeStaff(raceId, target.uniqueId)) {
                sender.sendMessage(Lang.getComponent("delete-staff", sender.locale()))
            } else {
                sender.sendMessage(Lang.getComponent("not-find-staff", sender.locale()))
            }
        }
    }

    @CommandMethod("staff list <raceId>")
    fun listStaff(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        plugin.launch {
            if (CommandUtils.returnRaceSetting(raceId, sender)) return@launch
            RaceStaffUtils.getStaff(raceId).forEach {
                sender.sendMessage(Bukkit.getOfflinePlayer(it).name.toString())
            }
        }
    }

    private fun returnCanSetPlayer(target: OfflinePlayer, sender: Player, playerName: String): Boolean {
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(Lang.getComponent("not-exsist-staff-this-player", sender.locale(), playerName, sender.locale()))
            return true
        }
        return false
    }
}