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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.i18n.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist bet")
class BetDeleteCommand {
    @CommandPermission("raceassist.commands.bet.delete")
    @CommandMethod("delete <operateRaceId>")
    @Confirmation
    @CommandDescription("レースに対して駆けられているものを削除します")
    suspend fun delete(sender: CommandSender, @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        BetUtils.deleteBetData(raceId)
        sender.sendMessage(Lang.getComponent("bet-remove-race", sender.locale(), raceId))

    }

}