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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.CommandUtils.stop
import kotlinx.coroutines.delay
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist race")
class RaceStopCommand {
    @CommandPermission("RaceAssist.commands.race.stop")
    @CommandMethod("stop <raceId>")
    suspend fun stop(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (CommandUtils.returnRaceSetting(raceId, sender)) return
        stop[raceId] = true
        delay(1000)
        stop[raceId] = false

    }
}