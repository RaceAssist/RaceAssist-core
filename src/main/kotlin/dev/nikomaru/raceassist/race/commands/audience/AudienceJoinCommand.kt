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
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.race.commands.CommandUtils.audience
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceExist
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist audience")
class AudienceJoinCommand {

    @CommandMethod("join <raceId>")
    private fun join(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (!getRaceExist(raceID)) {
                sender.sendMessage(Component.text(Lang.getText("not-found-this-race", sender.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            if (audience[raceID]?.contains(sender.uniqueId) == true) {
                sender.sendMessage(Component.text(Lang.getText("already-joined", sender.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            if (!audience.containsKey(raceID)) {
                audience[raceID] = ArrayList()
            }
            audience[raceID]?.add(sender.uniqueId)
            sender.sendMessage(Component.text(Lang.getText("joined-group", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
        }
    }
}