/*
 * Copyright © 2022 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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

import com.google.common.collect.ImmutableList
import dev.nikomaru.raceassist.bet.GuiComponent
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.files.Config.betUnit
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.text.MessageFormat
import java.util.*
import kotlin.math.floor

class BetChestGui {

    suspend fun getGUI(player: Player, raceID: String): Inventory {
        val gui = Bukkit.createInventory(player, 45, GuiComponent.guiComponent())
        val playerWools = ImmutableList.of(
            Material.RED_WOOL,
            Material.BLUE_WOOL,
            Material.YELLOW_WOOL,
            Material.GREEN_WOOL,
            Material.BROWN_WOOL,
            Material.PINK_WOOL,
            Material.WHITE_WOOL
        )
        val players: ArrayList<UUID> = ArrayList()
        val odds: HashMap<UUID, Double> = HashMap()
        var sum = 0
        println("1")
        val rate: Int = newSuspendedTransaction(Dispatchers.IO) {
            BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.returnPercent]
        }

        AllPlayers[raceID] = ArrayList<UUID>()
        newSuspendedTransaction(Dispatchers.IO) {
            println("2")
            PlayerList.select { PlayerList.raceID eq raceID }.forEach {
                players.add(UUID.fromString(it[PlayerList.playerUUID]))
                AllPlayers[raceID]!!.add(UUID.fromString(it[PlayerList.playerUUID]))
            }
        }

        newSuspendedTransaction(Dispatchers.IO) {
            BetList.select { BetList.raceID eq raceID }.forEach {
                sum += it[BetList.betting]
            }
        }
        players.forEach { jockey ->
            newSuspendedTransaction(Dispatchers.IO) {
                var jockeySum = 0
                BetList.select { (BetList.raceID eq raceID) and (BetList.jockey eq Bukkit.getOfflinePlayer(jockey).name!!) }.forEach {
                    jockeySum += it[BetList.betting]
                }
                odds[jockey] = floor(((sum * (rate.toDouble() / 100)) / jockeySum) * 100) / 100
            }
        }


        for (i in 0 until 45) {
            gui.setItem(i, ItemStack(Material.GRAY_STAINED_GLASS_PANE))
        }
        for (i in 0 until players.size) {
            val item = ItemStack(playerWools[i])
            val prevMeta = item.itemMeta
            prevMeta.displayName(
                text(
                    MessageFormat.format(Lang.getText("betting-zero-money", player.locale()), betUnit), TextColor.fromHexString
                        ("#00ff7f")
                )
            )
            val lore: ArrayList<Component> = ArrayList<Component>()
            lore.add(
                text(
                    MessageFormat.format(Lang.getText("gui-jockey-name", player.locale()), Bukkit.getOfflinePlayer(players[i]).name),
                    TextColor.fromHexString
                        ("#00a497")
                )
            )
            lore.add(
                text(
                    MessageFormat.format(Lang.getText("gui-jockey-odds", player.locale()), odds[players[i]]),
                    TextColor.fromHexString("#e6b422")
                )
            )
            prevMeta.lore(lore)
            item.itemMeta = prevMeta

            gui.setItem(i, GuiComponent.tenTimesUp(player.locale()))
            gui.setItem(i + 9, GuiComponent.onceUp(player.locale()))
            gui.setItem(i + 18, item)
            gui.setItem(i + 27, GuiComponent.onceDown(player.locale()))
            gui.setItem(i + 36, GuiComponent.tenTimesDown(player.locale()))
        }

        println("3")
        val raceIDItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val raceIDMeta = raceIDItem.itemMeta
        raceIDMeta.displayName(text(raceID, TextColor.fromHexString("#00ff7f")))
        raceIDItem.itemMeta = raceIDMeta

        gui.setItem(8, raceIDItem)
        gui.setItem(17, GuiComponent.reset(player.locale()))
        gui.setItem(35, GuiComponent.deny(player.locale()))
        gui.setItem(44, GuiComponent.accept(player.locale()))



        return gui

    }

    companion object {
        val AllPlayers: HashMap<String, ArrayList<UUID>> = HashMap()
    }
}