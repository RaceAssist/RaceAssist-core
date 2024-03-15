/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.race.commands

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import org.bukkit.command.CommandSender

@CommandMethod("ra")
class HelpCommand {
    @CommandMethod("help")
    @CommandPermission("raceassist.commands.help")
    @CommandDescription("help command")
    fun help(sender: CommandSender) {
        val message =
            "<click:open_url:'https://github.com/Nlkomaru/RaceAssist-core/wiki/Command'><green>コマンドリスト クリックで開く</green></click>"
        sender.sendRichMessage(message)
        return
    }
}