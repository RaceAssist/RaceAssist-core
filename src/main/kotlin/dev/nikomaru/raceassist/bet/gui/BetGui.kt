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

package dev.nikomaru.raceassist.bet.gui

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.bet.gui.components.GuiItems.AllPlayers
import dev.nikomaru.raceassist.bet.gui.components.GuiItems.onceDown
import dev.nikomaru.raceassist.bet.gui.components.GuiItems.onceUp
import dev.nikomaru.raceassist.bet.gui.components.GuiItems.tenTimesDown
import dev.nikomaru.raceassist.bet.gui.components.GuiItems.tenTimesUp
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta


class BetGui {
    private var players: ArrayList<OfflinePlayer> = ArrayList()
    private var odds: HashMap<OfflinePlayer, Double> = HashMap()

    suspend fun openGui(player: Player, raceId: String) {
        val gui = ChestGui(6, "Shop")
        val pane = StaticPane(0, 0, 9, 5)

        AllPlayers[raceId] = arrayListOf()

        RaceAssist.api.getRaceManager(raceId)!!.getJockeys().forEach {
            players.add(it)
            AllPlayers[raceId]?.add(it)
        }

        BetUtils.initializePlayerTempBetData(raceId, player)

        players.forEach {
            odds[it] = BetUtils.getOdds(raceId, it)
        }


        for (x in 0..8) {
            for (y in 0..4) {
                val none =
                    GuiItem(ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)) { event -> event.isCancelled = true }
                pane.addItem(none, x, y)
            }
        }

        for (i in 0 until players.size) {
            val item = ItemStack(Material.PLAYER_HEAD, 1)
            val meta: SkullMeta = item.itemMeta as SkullMeta
            meta.owningPlayer = players[i]
            meta.displayName(
                Lang.getComponent(
                    "betting-zero-money",
                    player.locale(),
                    RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
                )
            )
            val lore: ArrayList<Component> = arrayListOf()
            lore.add(Lang.getComponent("gui-jockey-name", player.locale(), players[i].name))
            lore.add(Lang.getComponent("gui-jockey-odds", player.locale(), odds[players[i]]))
            meta.lore(lore)
            item.itemMeta = meta

            val guiItem = GuiItem(item) { event -> event.isCancelled = true }

            pane.addItem(tenTimesUp(player.locale(), raceId), i, 0)
            pane.addItem(onceUp(player.locale(), raceId), i, 1)
            pane.addItem(guiItem, i, 2)
            pane.addItem(onceDown(player.locale(), raceId), i, 3)
            pane.addItem(tenTimesDown(player.locale(), raceId), i, 4)

            //TODO Add reset button
            //TODO Add accept button
            //TODO Add stop button
        }

        gui.addPane(pane)

        gui.show(player)
    }
}