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
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class BetGui : KoinComponent {
    private var players: ArrayList<OfflinePlayer> = ArrayList()
    private var odds: HashMap<OfflinePlayer, Double> = HashMap()

    private val plugin: RaceAssist by inject()
    private val economy = VaultAPI.getEconomy()

    private lateinit var gui: ChestGui
    private lateinit var pane: StaticPane
    private lateinit var player: Player
    private lateinit var raceId: String

    private val betManager by lazy { RaceAssist.api.getBetManager(raceId)!! }

    suspend fun openGui(player: Player, raceId: String) {
        this.gui = ChestGui(5, "Shop")
        this.pane = StaticPane(0, 0, 9, 5)
        this.player = player
        this.raceId = raceId

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
            pane.setOnClick { event ->
                event.isCancelled = true
            }

            val head = initHead(AllPlayers[raceId]?.get(i)!!)

            //0-8 ten times up
            pane.addItem(tenTimesUp(), i, 0)

            //9-17 once up
            pane.addItem(oneTimesUp(), i, 1)

            //18-26 head
            pane.addItem(head, i, 2)

            //18-26 once down
            pane.addItem(oneTimesDown(), i, 3)

            //27-35 ten times down
            pane.addItem(tenTimesDown(), i, 4)

            pane.addItem(reset(), 8, 1)
            pane.addItem(accept(), 8, 3)
            pane.addItem(close(), 8, 4)
        }

        gui.addPane(pane)

        gui.show(player)
    }

    private fun tenTimesUp(): GuiItem {
        val tenTimesUp = ItemStack(Material.RED_STAINED_GLASS_PANE)
        val tenTimesUpMeta: ItemMeta = tenTimesUp.itemMeta
        tenTimesUpMeta.displayName(
            Lang.getComponent(
                "to-bet-ten-unit", player.locale(), RaceAssist.api.getBetManager(raceId)!!.getBetUnit() * 10
            )
        )
        tenTimesUp.itemMeta = tenTimesUpMeta
        return GuiItem(tenTimesUp) { event ->
            val betUnit = betManager.getBetUnit()
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(slot)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot)) {
                    it.betPerUnit = selectedNowBet + 10
                }
            }
            val selectedAfterBet: Int = getNowBet(slot)
            val head = event.inventory.getItem(slot + 18)!!
            val itemMeta = head.itemMeta
            itemMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            head.itemMeta = itemMeta
            pane.addItem(GuiItem(head), slot, 2)
            gui.update()
            event.isCancelled = true
        }
    }

    private fun oneTimesUp(): GuiItem {
        val onceUp = ItemStack(Material.PINK_STAINED_GLASS_PANE)
        val onceUpMeta: ItemMeta = onceUp.itemMeta
        onceUpMeta.displayName(
            Lang.getComponent(
                "to-bet-one-unit", player.locale(), RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
            )
        )
        onceUp.itemMeta = onceUpMeta

        return GuiItem(onceUp) { event ->
            val betUnit = betManager.getBetUnit()
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(slot - 9)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 9)) {
                    it.betPerUnit = selectedNowBet + 1
                }
            }
            val selectedAfterBet: Int = getNowBet(slot - 9)
            val head = event.inventory.getItem(slot + 9)!!
            val headMeta = head.itemMeta
            headMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            head.itemMeta = headMeta

            event.isCancelled = true
        }
    }

    private fun oneTimesDown(): GuiItem {
        val onceDown = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        val onceDownMeta: ItemMeta = onceDown.itemMeta
        onceDownMeta.displayName(
            Lang.getComponent(
                "to-cancel-bet-one-unit", player.locale(), RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
            )
        )
        onceDown.itemMeta = onceDownMeta

        return GuiItem(onceDown) { event ->
            val betUnit = betManager.getBetUnit()
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(slot - 27)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 27)) {
                    it.betPerUnit = if (selectedNowBet - 1 < 0) 0 else (selectedNowBet - 1)
                }
            }

            val selectedAfterBet: Int = getNowBet(slot - 27)
            val head = event.inventory.getItem(slot - 9)!!
            val headMeta = head.itemMeta
            headMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            head.itemMeta = headMeta

            event.isCancelled = true
        }
    }

    private fun tenTimesDown(): GuiItem {
        val tenTimesDown = ItemStack(Material.BLUE_STAINED_GLASS_PANE)
        val tenTimesDownMeta: ItemMeta = tenTimesDown.itemMeta
        tenTimesDownMeta.displayName(
            Lang.getComponent(
                "to-cancel-bet-ten-unit", player.locale(), RaceAssist.api.getBetManager(raceId)!!.getBetUnit() * 10
            )
        )
        tenTimesDown.itemMeta = tenTimesDownMeta

        return GuiItem(tenTimesDown) { event ->
            val betUnit = betManager.getBetUnit()
            val slot = event.slot
            val selectedNowBet: Int = getNowBet(slot - 36)

            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.0f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player && it.jockey == AllPlayers[raceId]?.get(slot - 36)) {
                    it.betPerUnit = if (selectedNowBet - 10 < 0) 0 else (selectedNowBet - 10)
                }
            }

            val selectedAfterBet: Int = getNowBet(slot - 36)
            val head = event.inventory.getItem(slot - 18)!!
            val headMeta = head.itemMeta
            headMeta.displayName(
                Lang.getComponent(
                    "now-betting-price", player.locale(), betUnit, selectedAfterBet * betUnit
                )
            )
            head.itemMeta = headMeta

            event.isCancelled = true
        }
    }

    private fun getNowBet(slot: Int): Int {
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

    private fun initHead(jockey: OfflinePlayer): GuiItem {
        val head = ItemStack(Material.PLAYER_HEAD, 1)
        val headMeta: SkullMeta = head.itemMeta as SkullMeta
        headMeta.owningPlayer = jockey
        headMeta.displayName(
            Lang.getComponent(
                "betting-zero-money", player.locale(), betManager.getBetUnit()
            )
        )

        val lore: ArrayList<Component> = arrayListOf()
        lore.add(Lang.getComponent("gui-jockey-name", player.locale(), jockey.name))
        lore.add(Lang.getComponent("gui-jockey-odds", player.locale(), odds[jockey]))
        headMeta.lore(lore)
        head.itemMeta = headMeta



        return GuiItem(head) { event ->
            event.isCancelled = true
        }
    }

    private fun getAllBet(): Int {
        var sum = 0
        BetUtils.tempBetDataList.forEach {
            if (it.raceId == raceId && it.player == player) {
                sum += it.betPerUnit
            }
        }
        return sum
    }

    private fun accept(): GuiItem {
        val accept = ItemStack(Material.GREEN_WOOL)
        val acceptMeta: ItemMeta = accept.itemMeta
        acceptMeta.displayName(Lang.getComponent("gui-decide", player.locale()))
        accept.itemMeta = acceptMeta
        val item = GuiItem(accept) {
            val betManager = RaceAssist.api.getBetManager(raceId)!!
            val betUnit = betManager.getBetUnit()
            if (economy.getBalance(player) < (getAllBet() * betUnit)) {
                noticeNoMoney()
                return@GuiItem
            }
            if (getAllBet() == 0) {
                noticeNoBet()
                return@GuiItem
            }
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1f)
            player.closeInventory()
            transaction {
                BetUtils.tempBetDataList.stream().filter {
                    it.raceId == raceId && it.player == player && it.betPerUnit != 0
                }.forEach {
                    betManager.pushBet(it.player, it.jockey, it.betPerUnit)
                }
                BetUtils.tempBetDataList.removeIf { it.raceId == raceId && it.player == player }
            }
        }
        return item
    }

    private fun reset(): GuiItem {
        val reset = ItemStack(Material.WHITE_WOOL)
        val resetMeta: ItemMeta = reset.itemMeta
        resetMeta.displayName(Lang.getComponent("gui-reset", player.locale()))
        reset.itemMeta = resetMeta

        return GuiItem(reset) { _ ->
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player) {
                    it.betPerUnit = 0
                }
            }

            for (i in 0 until players.size) {
                val head = initHead(AllPlayers[raceId]?.get(i)!!)
                pane.addItem(head, i, 2)
                gui.update()
            }
        }
    }

    private fun close(): GuiItem {
        val close = ItemStack(Material.RED_WOOL)
        val closeMeta: ItemMeta = close.itemMeta
        closeMeta.displayName(Lang.getComponent("gui-cancel", player.locale()))
        close.itemMeta = closeMeta

        return GuiItem(close) { event ->
            player.closeInventory()
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.5f, 1f)

            val iterator = BetUtils.tempBetDataList.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.raceId == raceId && it.player == player) {
                    iterator.remove()
                }
            }
        }
    }

    private fun noticeNoBet() {
        val noBet = ItemStack(Material.BARRIER)
        val noBetMeta: ItemMeta = noBet.itemMeta
        noBetMeta.displayName(Lang.getComponent("no-one-betting", player.locale()))
        noBet.itemMeta = noBetMeta
        pane.addItem(GuiItem(noBet) { event ->
            event.isCancelled = true
        }, 8, 4)
        gui.update()
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            pane.addItem(accept(), 8, 4)
            gui.update()
        }, 20)


    }

    private fun noticeNoMoney() {
        val noMoney = ItemStack(Material.BARRIER)
        val noMoneyMeta: ItemMeta = noMoney.itemMeta
        noMoneyMeta.displayName(Lang.getComponent("no-have-money", player.locale()))
        noMoney.itemMeta = noMoneyMeta
        pane.addItem(GuiItem(noMoney) { event ->
            event.isCancelled = true
        }, 8, 4)
        gui.update()
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 0.7f)

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            pane.addItem(accept(), 8, 4)
            gui.update()
        }, 20)

    }

    companion object {
        val AllPlayers: java.util.HashMap<String, java.util.ArrayList<OfflinePlayer>> = java.util.HashMap()
    }
}