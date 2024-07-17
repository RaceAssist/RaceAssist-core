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

package dev.nikomaru.raceassist.bet.gui.components

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

object GuiItems {

    private fun getNowBet(raceId: String, player: Player, slot: Int): Int {
        var bet = 0
        val iterator = BetUtils.tempBetDataList.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot)) {
                bet = it.betPerUnit
            }
        }
        return bet
    }

    fun onceUp(locale: Locale, raceId: String): GuiItem {
        // once up slot: 9-16
        val onceUp = ItemStack(Material.PINK_STAINED_GLASS_PANE)
        val onceUpMeta: ItemMeta = onceUp.itemMeta
        onceUpMeta.displayName(
            Lang.getComponent(
                "to-bet-one-unit", locale, RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
            )
        )
        onceUp.itemMeta = onceUpMeta

        val guiItem = GuiItem(onceUp) { event ->
            val betManager = RaceAssist.api.getBetManager(raceId)!!
            val betUnit = betManager.getBetUnit()
            val player = event.whoClicked as Player
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(raceId, player, (slot - 9))
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 9)) {
                    it.betPerUnit = selectedNowBet + 1
                }
            }

            val selectedAfterBet: Int = getNowBet(raceId, player, (slot - 9))
            val item = event.inventory.getItem(slot + 9)!!
            val itemMeta = item.itemMeta
            itemMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            item.itemMeta = itemMeta

            event.isCancelled = true
        }
        return guiItem
    }

    fun onceDown(locale: Locale, raceId: String): GuiItem {
        // once down slot: 27-34
        val onceDown = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        val onceDownMeta: ItemMeta = onceDown.itemMeta
        onceDownMeta.displayName(
            Lang.getComponent(
                "to-cancel-bet-one-unit", locale, RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
            )
        )
        onceDown.itemMeta = onceDownMeta

        val guiItem = GuiItem(onceDown) { event ->
            val betManager = RaceAssist.api.getBetManager(raceId)!!
            val betUnit = betManager.getBetUnit()
            val player = event.whoClicked as Player
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(raceId, player, (slot - 27))
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 27)) {
                    it.betPerUnit = if (selectedNowBet - 1 < 0) 0 else (selectedNowBet - 1)
                }
            }

            val selectedAfterBet: Int = getNowBet(raceId, player, (slot - 27))
            val item = event.inventory.getItem(slot - 9)!!
            val itemMeta = item.itemMeta
            itemMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            item.itemMeta = itemMeta

            event.isCancelled = true
        }
        return guiItem
    }

    fun tenTimesUp(locale: Locale, raceId: String): GuiItem {
        //ten times up slot: 0-8
        val tenTimesUp = ItemStack(Material.RED_STAINED_GLASS_PANE)
        val tenTimesUpMeta: ItemMeta = tenTimesUp.itemMeta
        tenTimesUpMeta.displayName(
            Lang.getComponent(
                "to-bet-ten-unit", locale, RaceAssist.api.getBetManager(raceId)!!.getBetUnit() * 10
            )
        )
        tenTimesUp.itemMeta = tenTimesUpMeta

        val guiItem = GuiItem(tenTimesUp) { event ->
            val betManager = RaceAssist.api.getBetManager(raceId)!!
            val betUnit = betManager.getBetUnit()
            val player = event.whoClicked as Player
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(raceId, player, slot)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot)) {
                    it.betPerUnit = selectedNowBet + 10
                }
            }

            val selectedAfterBet: Int = getNowBet(raceId, player, slot)
            val item = event.inventory.getItem(slot + 18)!!
            val itemMeta = item.itemMeta
            itemMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            item.itemMeta = itemMeta

            event.isCancelled = true
        }
        return guiItem
    }

    fun tenTimesDown(locale: Locale, raceId: String): GuiItem {
        // ten times down slot: 36-44
        val tenTimesDown = ItemStack(Material.BLUE_STAINED_GLASS_PANE)
        val tenTimesDownMeta: ItemMeta = tenTimesDown.itemMeta
        tenTimesDownMeta.displayName(
            Lang.getComponent(
                "to-cancel-bet-ten-unit", locale, RaceAssist.api.getBetManager(raceId)!!.getBetUnit() * 10
            )
        )
        tenTimesDown.itemMeta = tenTimesDownMeta

        val guiItem = GuiItem(tenTimesDown) { event ->
            val betManager = RaceAssist.api.getBetManager(raceId)!!
            val betUnit = betManager.getBetUnit()
            val player = event.whoClicked as Player
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(raceId, player, (slot - 36))

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 36)) {
                    it.betPerUnit = if (selectedNowBet - 10 < 0) 0 else (selectedNowBet - 10)
                }
            }

            val selectedAfterBet: Int = getNowBet(raceId, player, (slot - 36))
            val item = event.inventory.getItem(slot - 18)!!
            val itemMeta = item.itemMeta
            itemMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            item.itemMeta = itemMeta

            event.isCancelled = true
        }
        return guiItem
    }

    val AllPlayers: HashMap<String, ArrayList<OfflinePlayer>> = HashMap()
}