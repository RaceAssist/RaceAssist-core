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

package dev.nikomaru.raceassist.bet.event

import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.GuiComponent
import dev.nikomaru.raceassist.bet.gui.BetChestGui.Companion.AllPlayers
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.TempBetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

class BetGuiClickEvent : Listener {
    @EventHandler
    suspend fun vendingClickEvent(event: InventoryClickEvent) {

        if (event.view.title() != GuiComponent.guiComponent()) {
            return
        }

        var limit = 0
        for (i in 0 until 8) {
            if (event.inventory.getItem(18 + i) != ItemStack(Material.GRAY_STAINED_GLASS_PANE)) {
                limit = i
            }
        }

        val player = event.whoClicked as Player
        event.isCancelled = true
        if (clicked[player.uniqueId] == true) {
            return
        }
        val raceID: String = PlainTextComponentSerializer.plainText().serialize(event.inventory.getItem(8)?.itemMeta?.displayName()!!)


        if (event.slot == 35) {
            player.closeInventory()
        }

        when (val slot = event.slot) {
            in 0..7 -> {
                //10 ↑
                if (slot > limit) {
                    return
                }
                val selectedNowBet: Int = getNowBet(raceID, player, slot)
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.0f)

                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.update({
                            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
                                slot
                            ).toString())
                        }) {
                            it[bet] = selectedNowBet + 10
                        }

                    }
                }

                val selectedAfterBet: Int = getNowBet(raceID, player, (slot))
                val item = event.inventory.getItem(slot + 18)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(text("1000円単位 : ${selectedAfterBet * 1000}円かけています", TextColor.fromHexString("#00ff7f")))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                delay(50)
                clicked.remove(player.uniqueId)
            }
            in 9..16 -> {
                // 1 ↑
                if (slot > (limit + 9)) {
                    return
                }
                val selectedNowBet: Int = getNowBet(raceID, player, (slot - 9))
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)



                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.update({
                            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
                                slot - 9
                            ).toString())
                        }) {
                            it[bet] = selectedNowBet + 1
                        }
                    }
                }
                val selectedAfterBet: Int = getNowBet(raceID, player, (slot - 9))
                val item = event.inventory.getItem(slot + 9)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(text("1000円単位 : ${selectedAfterBet * 1000}円かけています", TextColor.fromHexString("#00ff7f")))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                delay(50)
                clicked.remove(player.uniqueId)
            }
            in 27..34 -> {
                //1 ↓
                if (slot > (limit + 27)) {
                    return
                }
                val selectedNowBet: Int = getNowBet(raceID, player, (slot - 27))

                if (selectedNowBet <= 0) {
                    event.inventory.setItem(slot, GuiComponent.noUnderNotice())
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
                    delay(1000)
                    event.inventory.setItem(slot, GuiComponent.onceDown())
                    return
                }
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.7f)

                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.update({
                            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
                                slot - 27
                            ).toString())
                        }) {
                            it[bet] = selectedNowBet - 1
                        }
                    }
                }
                val selectedAfterBet: Int = getNowBet(raceID, player, (slot - 27))
                val item = event.inventory.getItem(slot - 9)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(text("1000円単位 : ${selectedAfterBet * 1000}円かけています", TextColor.fromHexString("#00ff7f")))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                delay(50)
                clicked.remove(player.uniqueId)
            }
            in 36..43 -> {
                // 10 ↓
                if (slot > (limit + 36)) {
                    return
                }
                val selectedNowBet: Int = getNowBet(raceID, player, (slot - 36))


                if (selectedNowBet <= 9) {
                    event.inventory.setItem(slot, GuiComponent.noUnderNotice())
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
                    delay(1000)
                    event.inventory.setItem(slot, GuiComponent.tenTimesDown())
                    return
                }
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f)
                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.update({
                            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
                                slot - 36
                            ).toString())
                        }) {
                            it[bet] = selectedNowBet - 10
                        }
                    }
                }

                val selectedAfterBet: Int = getNowBet(raceID, player, (slot - 36))
                val item = event.inventory.getItem(slot - 18)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(text("1000円単位 : ${selectedAfterBet * 1000}円かけています", TextColor.fromHexString("#00ff7f")))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                delay(50)
                clicked.remove(player.uniqueId)
            }
            17 -> {
                //clear
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.deleteWhere { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                        AllPlayers[raceID]?.forEach { jockey ->
                            TempBetData.insert {
                                it[TempBetData.raceID] = raceID
                                it[playerUUID] = player.uniqueId.toString()
                                it[TempBetData.jockey] = jockey.toString()
                                it[bet] = 0
                            }
                        }
                    }
                }
                for (i in 0 until limit + 1) {
                    val item = event.inventory.getItem(i + 18)!!
                    val itemMeta = item.itemMeta
                    itemMeta.displayName(text("1000円単位 : 0円かけています", TextColor.fromHexString("#00ff7f")))
                    item.itemMeta = itemMeta
                }
            }
            35 -> {
                //deny
                player.closeInventory()
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)
                RaceAssist.plugin!!.launch {
                    withContext(Dispatchers.IO) {
                        transaction {
                            TempBetData.deleteWhere { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                        }
                    }
                }
            }
            44 -> {
                //accept
                if (VaultAPI.getEconomy()!!.getBalance(player) < (getAllBet(raceID, player) * 1000)) {
                    event.inventory.setItem(slot, GuiComponent.noHaveMoney())
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)

                    delay(1000)
                    event.inventory.setItem(slot, GuiComponent.accept())
                    return
                }
                if (getAllBet(raceID, player) == 0) {
                    event.inventory.setItem(slot, GuiComponent.noBet())
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)

                    delay(1000)
                    event.inventory.setItem(slot, GuiComponent.accept())
                    return
                }

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1f)
                player.closeInventory()

                val eco: Economy = VaultAPI.getEconomy()!!
                val owner = Bukkit.getOfflinePlayer(UUID.fromString(transaction {
                    BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator]
                }))


                RaceAssist.plugin!!.launch {
                    withContext(Dispatchers.IO) {
                        transaction {
                            var row = BetList.selectAll().count().toInt()
                            TempBetData.select { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                                .forEach { temp ->
                                    if (temp[TempBetData.bet] != 0) {
                                        BetList.insert { bet ->
                                            bet[BetList.raceID] = raceID
                                            bet[playerName] = player.name
                                            bet[jockey] = Bukkit.getOfflinePlayer(UUID.fromString(temp[TempBetData.jockey])).name.toString()
                                            bet[betting] = temp[TempBetData.bet] * 1000
                                            bet[timeStamp] = LocalDateTime.now()
                                            bet[rowNum] = row + 1
                                        }

                                        player.sendMessage("番号${row + 1} : ${Bukkit.getOfflinePlayer(UUID.fromString(temp[TempBetData.jockey])).name.toString()} に" + " ${temp[TempBetData.bet] * 1000}円")
                                        eco.withdrawPlayer(player, temp[TempBetData.bet] * 1000.0)
                                        if (owner.isOnline) {
                                            (owner as Player).sendMessage("${player.name} が ${temp[TempBetData.bet] * 1000}円 を賭けました")
                                        }
                                        eco.depositPlayer(owner, temp[TempBetData.bet] * 1000.0)
                                        row++
                                    }
                                }
                            TempBetData.deleteWhere { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                        }
                    }
                }
            }

        }

        val accept = event.inventory.getItem(44)!!
        val acceptMeta = accept.itemMeta
        val acceptLore: ArrayList<Component> = ArrayList<Component>()
        acceptLore.add(text("${getAllBet(raceID, player) * 1000}円必要です"))
        acceptMeta.lore(acceptLore)
        accept.itemMeta = acceptMeta
        event.inventory.setItem(44, accept)
    }

    private fun getNowBet(raceID: String, player: Player, slot: Int) = transaction {
        TempBetData.select {
            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
                slot
            ).toString())
        }.first()[TempBetData.bet]
    }

    private fun getAllBet(raceID: String, player: Player) = transaction {
        TempBetData.select { (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) }.sumOf { it[TempBetData.bet] }
    }

    companion object {
        //チャタリングのような現象防止
        private val clicked = HashMap<UUID, Boolean>()
    }
}