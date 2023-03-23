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
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.isMatchStatus
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.saveData
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Horse
import org.bukkit.entity.Player

@CommandMethod("ra horse")
class HorseDetectCommand {
    @CommandMethod("detect")
    suspend fun detectHorses(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます")
            return
        }
        lateinit var horses: List<Horse>
        lateinit var matchHorses: List<Horse>
        withContext(Dispatchers.minecraft) {
            horses = sender.getNearbyEntities(50.0, 50.0, 50.0).filterIsInstance<Horse>()
            matchHorses = horses.filter { it.isMatchStatus() }
        }

        sender.sendMessage("周囲の馬の数: ${horses.size}")
        sender.sendMessage("周囲の馬のうち、記録される馬の数: ${matchHorses.size}")

        matchHorses.forEach {
            it.saveData()
        }

        if (matchHorses.isEmpty()) {
            sender.sendMessage("周囲に記録される馬はいませんでした")
        } else {
            sender.sendMessage("記録が完了しました")
            sender.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.BLOCK, 1f, 1f))
        }
    }


}