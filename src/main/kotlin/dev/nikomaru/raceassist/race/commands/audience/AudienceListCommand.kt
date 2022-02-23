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

package dev.nikomaru.raceassist.race.commands.audience

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist audience")
class AudienceListCommand {
    @CommandPermission("RaceAssist.commands.audience.list")
    @CommandMethod("list <raceId>")
    private fun list(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        RaceAssist.plugin.launch {
            if (returnRaceSetting(raceId, sender)) return@launch
            sender.sendMessage(Lang.getComponent("participants-list", sender.locale()))
            CommandUtils.audience[raceId]?.forEach {
                sender.sendMessage(Bukkit.getOfflinePlayer(it).name.toString())
            }
        }
    }
}