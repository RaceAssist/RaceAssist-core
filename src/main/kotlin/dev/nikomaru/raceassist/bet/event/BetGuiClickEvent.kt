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

import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil.getSheetsService
import dev.nikomaru.raceassist.bet.GuiComponent
import dev.nikomaru.raceassist.bet.gui.BetChestGui.Companion.AllPlayers
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.TempBetData
import dev.nikomaru.raceassist.files.Config.betUnit
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
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
                itemMeta.displayName(text("${betUnit}円単位 : ${selectedAfterBet * betUnit}円かけています", TextColor.fromHexString("#00ff7f")))
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
                itemMeta.displayName(text("${betUnit}円単位 : ${selectedAfterBet * betUnit}円かけています", TextColor.fromHexString("#00ff7f")))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                //anti chattering
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
                val uuid = player.uniqueId.toString()
                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.update({
                            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq uuid) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
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
                itemMeta.displayName(text("${betUnit}円単位 : ${selectedAfterBet * betUnit}円かけています", TextColor.fromHexString("#00ff7f")))
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
                val uuid = player.uniqueId.toString()
                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.update({
                            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq uuid) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
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
                itemMeta.displayName(text("${betUnit}円単位 : ${selectedAfterBet * betUnit}円かけています", TextColor.fromHexString("#00ff7f")))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                delay(50)
                clicked.remove(player.uniqueId)
            }
            17 -> {
                //clear
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                val uuid = player.uniqueId.toString()
                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.deleteWhere { (TempBetData.playerUUID eq uuid) and (TempBetData.raceID eq raceID) }
                        AllPlayers[raceID]?.forEach { jockey ->
                            TempBetData.insert {
                                it[TempBetData.raceID] = raceID
                                it[playerUUID] = uuid
                                it[TempBetData.jockey] = jockey.toString()
                                it[bet] = 0
                            }
                        }
                    }
                }
                for (i in 0 until limit + 1) {
                    val item = event.inventory.getItem(i + 18)!!
                    val itemMeta = item.itemMeta
                    itemMeta.displayName(text("${betUnit}円単位 : 0円かけています", TextColor.fromHexString("#00ff7f")))
                    item.itemMeta = itemMeta
                }
            }
            35 -> {
                //deny
                player.closeInventory()
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)

                withContext(Dispatchers.IO) {
                    transaction {
                        TempBetData.deleteWhere { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                    }
                }

            }
            44 -> {
                //accept

                if (VaultAPI.getEconomy()!!.getBalance(player) < (getAllBet(raceID, player) * betUnit)) {
                    noticeNoMoney(event, slot, player)
                    return
                }
                if (getAllBet(raceID, player) == 0) {
                    noticeNoBet(event, slot, player)
                    return
                }

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1f)
                player.closeInventory()

                val eco: Economy = VaultAPI.getEconomy()!!
                val owner = Bukkit.getOfflinePlayer(UUID.fromString(newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator]
                }))

                withContext(Dispatchers.Default) {
                    transaction {
                        var row = BetList.selectAll().count().toInt()
                        TempBetData.select { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                            .forEach { temp ->
                                if (temp[TempBetData.bet] != 0) {
                                    BetList.insert { bet ->
                                        bet[BetList.raceID] = raceID
                                        bet[playerName] = player.name
                                        bet[playerUUID] = player.uniqueId.toString()
                                        bet[jockey] = Bukkit.getOfflinePlayer(UUID.fromString(temp[TempBetData.jockey])).name.toString()
                                        bet[betting] = temp[TempBetData.bet] * betUnit
                                        bet[timeStamp] = LocalDateTime.now()
                                        bet[rowNum] = row + 1
                                    }
                                    betProcess(player, row, temp, eco, owner)
                                    row++
                                }
                            }
                        TempBetData.deleteWhere { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                    }
                }
                putSheetsData(raceID)
            }
        }

        val accept = event.inventory.getItem(44)!!
        val acceptMeta = accept.itemMeta
        val acceptLore: ArrayList<Component> = ArrayList<Component>()
        acceptLore.add(text("${getAllBet(raceID, player) * betUnit}円必要です"))
        acceptMeta.lore(acceptLore)
        accept.itemMeta = acceptMeta
        event.inventory.setItem(44, accept)
    }

    private fun betProcess(player: Player, row: Int, temp: ResultRow, eco: Economy, owner: OfflinePlayer) {
        player.sendMessage(
            "番号${row + 1} : ${Bukkit.getOfflinePlayer(UUID.fromString(temp[TempBetData.jockey])).name.toString()} に" +
                    " ${temp[TempBetData.bet] * betUnit}円"
        )
        eco.withdrawPlayer(player, temp[TempBetData.bet] * betUnit.toDouble())

        if (owner.isOnline) {
            (owner as Player).sendMessage("${player.name} が ${temp[TempBetData.bet] * betUnit}円 を賭けました")
        }
        eco.depositPlayer(owner, temp[TempBetData.bet] * betUnit.toDouble())
    }

    private suspend fun noticeNoBet(event: InventoryClickEvent, slot: Int, player: Player) {
        event.inventory.setItem(slot, GuiComponent.noBet())
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
        delay(1000)
        event.inventory.setItem(slot, GuiComponent.accept())
    }

    private suspend fun noticeNoMoney(event: InventoryClickEvent, slot: Int, player: Player) {
        event.inventory.setItem(slot, GuiComponent.noHaveMoney())
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
        delay(1000)
        event.inventory.setItem(slot, GuiComponent.accept())
    }

    private suspend fun getNowBet(raceID: String, player: Player, slot: Int) = newSuspendedTransaction {
        TempBetData.select {
            (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.jockey eq AllPlayers[raceID]?.get(
                slot
            ).toString())
        }.first()[TempBetData.bet]
    }

    private suspend fun getAllBet(raceID: String, player: Player) = newSuspendedTransaction {
        TempBetData.select { (TempBetData.raceID eq raceID) and (TempBetData.playerUUID eq player.uniqueId.toString()) }.sumOf { it[TempBetData.bet] }
    }

    private suspend fun putSheetsData(raceID: String) = withContext(Dispatchers.Default) {
        val spreadsheetId = getSheetID(raceID) ?: return@withContext
        val sheetsService = getSheetsService(spreadsheetId)

        var i = 1
        val data: ArrayList<ValueRange> = ArrayList()
        data.add(
            ValueRange().setRange("${raceID}_RaceAssist!A${i}").setValues(
                listOf(
                    listOf("タイムスタンプ", "マイクラネーム", "対象プレイヤー", "賭け金", "ベッド倍率 (%)->", getBetPercent(raceID))
                )
            )
        )

        newSuspendedTransaction(Dispatchers.Default) {
            BetList.select { BetList.raceID eq raceID }.forEach {

                val player = it[BetList.playerName]
                val jockey = it[BetList.jockey]
                val betting = it[BetList.betting]
                val timeStamp = it[BetList.timeStamp]
                data.add(
                    ValueRange().setRange("${raceID}_RaceAssist!A${i + 1}").setValues(
                        listOf(
                            listOf(
                                timeStamp.toString(), player, jockey,
                                betting
                            )
                        )
                    )
                )
                i++
            }
            val batchBody = BatchUpdateValuesRequest()
                .setValueInputOption("USER_ENTERED")
                .setData(data)

            sheetsService?.spreadsheets()?.values()?.batchUpdate(spreadsheetId, batchBody)?.execute()
        }

    }

    private suspend fun getSheetID(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetSetting.select { BetSetting.raceID eq raceID }.firstOrNull()?.get(BetSetting.spreadsheetId)
    }

    private suspend fun getBetPercent(raceID: String): Int = newSuspendedTransaction(Dispatchers.IO) {
        BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.returnPercent]
    }

    companion object {
        //Prevention of chattering-like phenomena
        private val clicked = HashMap<UUID, Boolean>()
    }
}