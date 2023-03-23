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

package dev.nikomaru.raceassist.race.commands

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.utils.Lang.mm
import org.bukkit.command.CommandSender

@CommandMethod("ra|raceassist")
class HelpCommand {
    @CommandMethod("help")
    @CommandPermission("raceassist.commands.help")
    @CommandDescription("help command")
    fun help(sender: CommandSender) {
        val message =
            "<click:open_url:'https://github.com/Nlkomaru/RaceAssist-core/wiki/Command'><green>コマンドリスト クリックで開く</green></click>"
        sender.sendMessage(mm.deserialize(message))
    }
//
//    @CommandMethod("image <x1> <x2> <y1> <y2>")
//    suspend fun createImage(sender: CommandSender,
//        @Argument(value = "x1") x1: Int,
//        @Argument(value = "x2") x2: Int,
//        @Argument(value = "y1") y1: Int,
//        @Argument(value = "y2") y2: Int) {
//        val base64 = Utils.createImage(x1, x2, y1, y2)
//
//        File("D:\\download\\racajkghfds.txt").writeText(base64)
//        val serializedObject: ByteArray = Base64.getDecoder().decode(base64)
//        File("D:\\download\\racajkghfds.png").writeBytes(serializedObject)
//    }

}