/*
 * Copyright © 2022 Nikomaru <nikomaru@nikomaru.dev>
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
import dev.nikomaru.raceassist.race.commands.CommandUtils.audience
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist audience")
class AudienceLeaveCommand {

    @CommandMethod("leave <raceId>")
    private fun leave(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        if (audience[raceID]?.contains(sender.uniqueId) == false) {
            sender.sendMessage(Component.text(Lang.getText("now-not-belong", sender.locale()), TextColor.color(NamedTextColor.RED)))
            return
        }
        audience[raceID]?.remove(sender.uniqueId)
        sender.sendMessage(Component.text(Lang.getText("to-exit-the-group", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
    }
}