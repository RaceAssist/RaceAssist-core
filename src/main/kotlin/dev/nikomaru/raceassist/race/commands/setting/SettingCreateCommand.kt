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

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.RaceData
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist setting")
class SettingCreateCommand {
    @CommandPermission("RaceAssist.commands.setting.create")
    @CommandMethod("create <raceId>")
    suspend fun create(sender: CommandSender, @Argument(value = "raceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        if (RaceData.existsRace(raceId)) {
            sender.sendMessage(Lang.getComponent("already-used-the-name-race", sender.locale()))
            return
        }
        RaceData.createRace(raceId, sender)
        sender.sendMessage(Lang.getComponent("to-create-race", sender.locale()))

    }
}