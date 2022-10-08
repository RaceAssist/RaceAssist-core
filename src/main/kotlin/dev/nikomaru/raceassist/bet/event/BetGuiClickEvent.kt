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

package dev.nikomaru.raceassist.bet.event

import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil.getSheetsService
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.bet.GuiComponent
import dev.nikomaru.raceassist.bet.gui.BetChestGui.Companion.AllPlayers
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.data.database.BetList.rowNum
import dev.nikomaru.raceassist.data.files.BetSettingData
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.milkbowl.vault.economy.Economy
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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
        val raceId: String = PlainTextComponentSerializer.plainText().serialize(event.inventory.getItem(8)?.itemMeta?.displayName()!!)


        if (event.slot == 35) {
            player.closeInventory()
        }

        val betUnit = BetSettingData.getBetUnit(raceId)
        when (val slot = event.slot) {

            in 0..7 -> {
                //10 ↑
                if (slot > limit) {
                    return
                }

                val selectedNowBet: Int = getNowBet(raceId, player, slot)
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.0f)

                val iterator = BetUtils.TempBetDatas.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot)) {
                        it.betPerUnit = selectedNowBet + 10
                    }
                }

                val selectedAfterBet: Int = getNowBet(raceId, player, (slot))
                val item = event.inventory.getItem(slot + 18)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(Lang.getComponent("now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit))
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
                val selectedNowBet: Int = getNowBet(raceId, player, (slot - 9))
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)

                val iterator = BetUtils.TempBetDatas.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 9)) {
                        it.betPerUnit = selectedNowBet + 1
                    }
                }

                val selectedAfterBet: Int = getNowBet(raceId, player, (slot - 9))
                val item = event.inventory.getItem(slot + 9)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(Lang.getComponent("now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit))
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
                val selectedNowBet: Int = getNowBet(raceId, player, (slot - 27))

                if (selectedNowBet <= 0) {
                    event.inventory.setItem(slot, GuiComponent.noUnderNotice(player.locale()))
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
                    delay(1000)
                    event.inventory.setItem(slot, GuiComponent.onceDown(player.locale(), raceId))
                    return
                }
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.7f)
                val iterator = BetUtils.TempBetDatas.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 27)) {
                        it.betPerUnit = selectedNowBet - 1
                    }
                }

                val selectedAfterBet: Int = getNowBet(raceId, player, (slot - 27))
                val item = event.inventory.getItem(slot - 9)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(Lang.getComponent("now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit))
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
                val selectedNowBet: Int = getNowBet(raceId, player, (slot - 36))


                if (selectedNowBet <= 9) {
                    event.inventory.setItem(slot, GuiComponent.noUnderNotice(player.locale()))
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
                    delay(1000)
                    event.inventory.setItem(slot, GuiComponent.tenTimesDown(player.locale(), raceId))
                    return
                }
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.7f)

                val iterator = BetUtils.TempBetDatas.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 36)) {
                        it.betPerUnit = selectedNowBet - 10
                    }
                }

                val selectedAfterBet: Int = getNowBet(raceId, player, (slot - 36))
                val item = event.inventory.getItem(slot - 18)!!
                val itemMeta = item.itemMeta
                itemMeta.displayName(Lang.getComponent("now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit))
                item.itemMeta = itemMeta

                clicked[player.uniqueId] = true
                delay(50)
                clicked.remove(player.uniqueId)
            }
            17 -> {
                //clear
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                val iterator = BetUtils.TempBetDatas.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it.raceId == raceId && it.player == player) {
                        it.betPerUnit = 0
                    }
                }

                for (i in 0 until limit + 1) {
                    val item = event.inventory.getItem(i + 18)!!
                    val itemMeta = item.itemMeta
                    itemMeta.displayName(Lang.getComponent("betting-zero-money", player.locale(), betUnit))
                    item.itemMeta = itemMeta
                }
            }
            35 -> {
                //deny
                player.closeInventory()
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)

                val iterator = BetUtils.TempBetDatas.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    if (it.raceId == raceId && it.player == player) {
                        iterator.remove()
                    }
                }

            }
            44 -> {
                //accept

                if (VaultAPI.getEconomy().getBalance(player) < (getAllBet(raceId, player) * betUnit)) {
                    noticeNoMoney(event, slot, player)
                    return
                }
                if (getAllBet(raceId, player) == 0) {
                    noticeNoBet(event, slot, player)
                    return
                }

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1f)
                player.closeInventory()

                val eco: Economy = VaultAPI.getEconomy()
                val owner = RaceSettingData.getOwner(raceId)

                newSuspendedTransaction(Dispatchers.Default) {
                    var row = getMaxRow(raceId)
                    val iterator = BetUtils.TempBetDatas.iterator()
                    while (iterator.hasNext()) {
                        val temp = iterator.next()
                        if (temp.raceId == raceId && temp.player == player && temp.betPerUnit != 0) {
                            BetList.insert { bet ->
                                bet[BetList.raceId] = raceId
                                bet[playerName] = player.name
                                bet[playerUUID] = player.uniqueId.toString()
                                bet[jockey] = temp.jockey.name.toString()
                                bet[jockeyUUID] = temp.jockey.uniqueId.toString()
                                bet[betting] = temp.betPerUnit * betUnit
                                bet[timeStamp] = LocalDateTime.now()
                                bet[rowNum] = row + 1
                            }
                            betProcess(player, row, temp.betPerUnit, temp.jockey, eco, owner, raceId)
                            row++
                        }
                    }
                    while (iterator.hasNext()) {
                        val it = iterator.next()
                        if (it.raceId == raceId && it.player == player) {
                            iterator.remove()
                        }
                    }
                }
                putSheetsData(raceId)
            }
        }

        val accept = event.inventory.getItem(44)!!
        val acceptMeta = accept.itemMeta
        val acceptLore: ArrayList<Component> = ArrayList<Component>()
        acceptLore.add(Lang.getComponent("gui-need-money", player.locale(), getAllBet(raceId, player) * betUnit))
        acceptMeta.lore(acceptLore)
        accept.itemMeta = acceptMeta
        event.inventory.setItem(44, accept)
    }

    private suspend fun getMaxRow(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        var maxRow = 0
        BetList.select { BetList.raceId eq raceId }.forEach {
            if (it[rowNum] > maxRow) {
                maxRow = it[rowNum]
            }
        }
        maxRow
    }

    private suspend fun betProcess(player: Player, row: Int, bet: Int, jockey: OfflinePlayer, eco: Economy, owner: OfflinePlayer, raceId: String) {
        player.sendMessage(Lang.getComponent("bet-complete-message-player",
            player.locale(),
            row + 1,
            jockey.name.toString(),
            bet * BetSettingData.getBetUnit(raceId)))
        eco.withdrawPlayer(player, bet * BetSettingData.getBetUnit(raceId).toDouble())

        if (owner.isOnline) {
            (owner as Player).sendMessage(Lang.getComponent("bet-complete-message-owner",
                player.locale(),
                player.name,
                bet * BetSettingData.getBetUnit(raceId)))
        }
        eco.depositPlayer(owner, bet * BetSettingData.getBetUnit(raceId).toDouble())
    }

    private suspend fun noticeNoBet(event: InventoryClickEvent, slot: Int, player: Player) {
        event.inventory.setItem(slot, GuiComponent.noBet(player.locale()))
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
        delay(1000)
        event.inventory.setItem(slot, GuiComponent.accept(player.locale()))
    }

    private suspend fun noticeNoMoney(event: InventoryClickEvent, slot: Int, player: Player) {
        event.inventory.setItem(slot, GuiComponent.noHaveMoney(player.locale()))
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)
        delay(1000)
        event.inventory.setItem(slot, GuiComponent.accept(player.locale()))
    }

    private fun getNowBet(raceId: String, player: Player, slot: Int): Int {
        var bet = 0
        val iterator = BetUtils.TempBetDatas.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot)) {
                bet = it.betPerUnit
            }
        }
        return bet
    }

    private fun getAllBet(raceId: String, player: Player): Int {
        var sum = 0
        BetUtils.TempBetDatas.forEach {
            if (it.raceId == raceId && it.player == player) {
                sum += it.betPerUnit
            }
        }
        return sum
    }

    private suspend fun putSheetsData(raceId: String) = withContext(Dispatchers.Default) {
        val spreadSheetIdList = BetSettingData.getSpreadSheetId(raceId)
        spreadSheetIdList?.let { sheetId ->

            val sheetsService = getSheetsService(sheetId) ?: return@withContext

            var i = 1
            val data: ArrayList<ValueRange> = ArrayList()
            data.add(ValueRange().setRange("${raceId}_RaceAssist_Bet!A${i}")
                .setValues(listOf(listOf(Lang.getText("sheet-timestamp", Locale.getDefault()),
                    Lang.getText("sheet-minecraft-name", Locale.getDefault()),
                    Lang.getText("sheet-jockey", Locale.getDefault()),
                    Lang.getText("sheet-bet-price", Locale.getDefault()),
                    Lang.getText("sheet-bet-multiplier", Locale.getDefault()),
                    getBetPercent(raceId)))))

            newSuspendedTransaction(Dispatchers.Default) {
                BetList.select { BetList.raceId eq raceId }.forEach {
                    i++
                    val player = it[BetList.playerName]
                    val jockey = it[BetList.jockey]
                    val betting = it[BetList.betting]
                    val timeStamp = it[BetList.timeStamp]
                    data.add(ValueRange().setRange("${raceId}_RaceAssist_Bet!A${i}")
                        .setValues(listOf(listOf(timeStamp.toString(), player, jockey, betting))))
                }
                val batchBody = BatchUpdateValuesRequest().setValueInputOption("USER_ENTERED").setData(data)

                sheetsService.spreadsheets()?.values()?.batchUpdate(sheetId, batchBody)?.execute()
            }
        }

    }

    private suspend fun getBetPercent(raceId: String): Int = newSuspendedTransaction(Dispatchers.IO) {
        BetSettingData.getReturnPercent(raceId)
    }

    companion object {
        //Prevention of chattering-like phenomena
        private val clicked = HashMap<UUID, Boolean>()
    }
}