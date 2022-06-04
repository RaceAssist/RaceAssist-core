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
import dev.nikomaru.raceassist.utils.CommandUtils.audience
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist audience")
class AudienceLeaveCommand {
    @CommandPermission("RaceAssist.commands.audience.leave")
    @CommandMethod("leave <raceId>")
    private fun leave(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        if (audience[raceId]?.contains(sender.uniqueId) == false) {
            sender.sendMessage(Lang.getComponent("now-not-belong", sender.locale()))
            return
        }
        audience[raceId]?.remove(sender.uniqueId)
        sender.sendMessage(Lang.getComponent("to-exit-the-group", sender.locale()))
    }
}