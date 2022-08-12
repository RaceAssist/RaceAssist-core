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
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.Utils.returnCanRaceSetting
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist audience")
class AudienceListCommand {
    @CommandPermission("raceassist.commands.audience.list")
    @CommandMethod("list <raceId>")
    suspend fun list(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        val locale = sender.locale()

        if (returnCanRaceSetting(raceId, sender)) return
        sender.sendMessage(Lang.getComponent("participants-list", locale))
        Utils.audience[raceId]?.forEach {
            sender.sendMessage(Bukkit.getOfflinePlayer(it).name.toString())
        }

    }
}