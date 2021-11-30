/*
 *  Copyright © 2021 Nikomaru
 *
 *  This program is free software: you can redistribute it and/or modify
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
package dev.nikomaru.keibaassist.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.WHITE
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender

@CommandAlias("ka|KeibaAssist")
class CommandManager : BaseCommand() {
    @Default
    @Subcommand("help")
    fun help(sender: CommandSender) {
        sender.sendMessage(text("KeibaAssistのコマンド一覧", TextColor.color(GREEN)))
        sender.sendMessage(
            text("/[ka|KeibaAssist] control create <グループ名> ", TextColor.color(GREEN))
                .append(text("グループを作成します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control help",
                TextColor.color(GREEN)
            ).append(text("ヘルプを表示します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control add <グループ名>",
                TextColor.color(GREEN)
            ).append(text("グループに馬を追加します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control remove <グループ名>",
                TextColor.color(GREEN)
            ).append(text("グループから馬を削除します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control display <グループ名>",
                TextColor.color(GREEN)
            ).append(text("グループに入っている馬を表示します", TextColor.color(WHITE)))
        )
    }
}