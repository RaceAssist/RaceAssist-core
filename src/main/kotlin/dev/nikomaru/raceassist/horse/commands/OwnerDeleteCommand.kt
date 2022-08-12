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

package dev.nikomaru.raceassist.horse.commands

import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import org.bukkit.command.CommandSender
import org.bukkit.entity.Horse
import org.bukkit.entity.Player

@CommandMethod("ra horse")
class OwnerDeleteCommand {
    @CommandMethod("ownerDelete")
    @CommandPermission("raceassist.command.ownerdelete")
    fun removeOwner(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます")
            return
        }
        val entity = sender.getTargetEntity(10) ?: return sender.sendMessage("対象が見つかりませんでした")
        if (entity !is Horse) {
            sender.sendMessage("対象が馬ではありません")
            return
        }
        if (entity.owner != sender) {
            sender.sendMessage("あなたはこの馬のオーナーではありません")
            return
        }
        entity.owner = null
        sender.sendMessage("オーナーを解除しました")
    }
}