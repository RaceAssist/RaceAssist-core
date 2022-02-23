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
import dev.nikomaru.raceassist.utils.CommandUtils.audience
import dev.nikomaru.raceassist.utils.CommandUtils.getRaceExist
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist audience")
class AudienceJoinCommand {
    @CommandPermission("RaceAssist.commands.audience.join")
    @CommandMethod("join <raceId>")
    private fun join(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        RaceAssist.plugin.launch {
            if (!getRaceExist(raceId)) {
                sender.sendMessage(Lang.getComponent("not-found-this-race", sender.locale()))
                return@launch
            }
            if (audience[raceId]?.contains(sender.uniqueId) == true) {
                sender.sendMessage(Lang.getComponent("already-joined", sender.locale()))
                return@launch
            }
            if (!audience.containsKey(raceId)) {
                audience[raceId] = ArrayList()
            }
            audience[raceId]?.add(sender.uniqueId)
            sender.sendMessage(Lang.getComponent("joined-group", sender.locale()))
        }
    }
}