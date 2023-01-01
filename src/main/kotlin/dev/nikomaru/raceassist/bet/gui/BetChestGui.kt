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

package dev.nikomaru.raceassist.bet.gui

import dev.nikomaru.raceassist.bet.BetUtils.getOdds
import dev.nikomaru.raceassist.bet.GuiComponent
import dev.nikomaru.raceassist.data.files.BetSettingData
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.utils.i18n.Lang
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class BetChestGui {

    suspend fun getGUI(player: Player, raceId: String): Inventory {
        val gui = Bukkit.createInventory(player, 45, GuiComponent.guiComponent())
        val players: ArrayList<OfflinePlayer> = ArrayList()
        val odds: HashMap<OfflinePlayer, Double> = HashMap()

        AllPlayers[raceId] = arrayListOf()
        RaceSettingData.getJockeys(raceId).forEach {
            players.add(it)
            AllPlayers[raceId]?.add(it)
        }

        players.forEach {
            odds[it] = getOdds(raceId, it)
        }


        for (i in 0 until 45) {
            gui.setItem(i, ItemStack(Material.GRAY_STAINED_GLASS_PANE))
        }
        // IFに置き換えるよてい https://github.com/stefvanschie/IF
        for (i in 0 until players.size) {
            val item = ItemStack(Material.PLAYER_HEAD, 1)
            val meta: SkullMeta = item.itemMeta as SkullMeta
            meta.owningPlayer = players[i]
            meta.displayName(Lang.getComponent("betting-zero-money", player.locale(), BetSettingData.getBetUnit(raceId)))
            val lore: ArrayList<Component> = arrayListOf()
            lore.add(Lang.getComponent("gui-jockey-name", player.locale(), players[i].name))
            lore.add(Lang.getComponent("gui-jockey-odds", player.locale(), odds[players[i]]))
            meta.lore(lore)
            item.itemMeta = meta

            gui.setItem(i, GuiComponent.tenTimesUp(player.locale(), raceId))
            gui.setItem(i + 9, GuiComponent.onceUp(player.locale(), raceId))
            gui.setItem(i + 18, item)
            gui.setItem(i + 27, GuiComponent.onceDown(player.locale(), raceId))
            gui.setItem(i + 36, GuiComponent.tenTimesDown(player.locale(), raceId))
        }

        val raceIdItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val raceIdMeta = raceIdItem.itemMeta
        raceIdMeta.displayName(text(raceId, TextColor.fromHexString("#00ff7f")))
        raceIdItem.itemMeta = raceIdMeta

        gui.setItem(8, raceIdItem)
        gui.setItem(17, GuiComponent.reset(player.locale()))
        gui.setItem(35, GuiComponent.deny(player.locale()))
        gui.setItem(44, GuiComponent.accept(player.locale()))

        return gui

    }

    companion object {
        val AllPlayers: HashMap<String, ArrayList<OfflinePlayer>> = HashMap()
    }
}