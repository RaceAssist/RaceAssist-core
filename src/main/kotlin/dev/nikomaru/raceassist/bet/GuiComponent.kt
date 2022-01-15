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

package dev.nikomaru.raceassist.bet

import dev.nikomaru.raceassist.files.Config.betUnit
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object GuiComponent {

    fun guiComponent(): TextComponent {
        return text("レース賭け自販機").color(TextColor.fromHexString("#228b22"))
    }

    fun onceUp(): ItemStack {
        val onceUp = ItemStack(Material.PINK_STAINED_GLASS_PANE)
        val onceUpMeta: ItemMeta = onceUp.itemMeta
        onceUpMeta.displayName(text("1単位 ${betUnit}円 賭ける").color(TextColor.fromHexString("#f08080")))
        onceUp.itemMeta = onceUpMeta
        return onceUp
    }

    fun onceDown(): ItemStack {
        val onceDown = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        val onceDownMeta: ItemMeta = onceDown.itemMeta
        onceDownMeta.displayName(text("1単位 ${betUnit}円 下げる").color(TextColor.fromHexString("#add8e6")))
        onceDown.itemMeta = onceDownMeta
        return onceDown
    }

    fun tenTimesUp(): ItemStack {
        val tenTimesUp = ItemStack(Material.RED_STAINED_GLASS_PANE)
        val tenTimesUpMeta: ItemMeta = tenTimesUp.itemMeta
        tenTimesUpMeta.displayName(text("10単位 ${betUnit * 10}円 賭ける").color(TextColor.fromHexString("#ff0000")))
        tenTimesUp.itemMeta = tenTimesUpMeta
        return tenTimesUp
    }

    fun tenTimesDown(): ItemStack {
        val tenTimesDown = ItemStack(Material.BLUE_STAINED_GLASS_PANE)
        val tenTimesDownMeta: ItemMeta = tenTimesDown.itemMeta
        tenTimesDownMeta.displayName(text("10単位 ${betUnit * 10}円 下げる").color(TextColor.fromHexString("#0000cd")))
        tenTimesDown.itemMeta = tenTimesDownMeta
        return tenTimesDown
    }

    fun accept(): ItemStack {
        val accept = ItemStack(Material.GREEN_WOOL)
        val acceptMeta: ItemMeta = accept.itemMeta
        acceptMeta.displayName(text("決定する").color(TextColor.fromHexString("#228b22")))
        accept.itemMeta = acceptMeta
        return accept
    }

    fun deny(): ItemStack {
        val deny = ItemStack(Material.RED_WOOL)
        val denyMeta: ItemMeta = deny.itemMeta
        denyMeta.displayName(text("キャンセルする").color(TextColor.fromHexString("#ff0000")))
        deny.itemMeta = denyMeta
        return deny
    }

    fun reset(): ItemStack {
        val reset = ItemStack(Material.WHITE_WOOL)
        val resetMeta: ItemMeta = reset.itemMeta
        resetMeta.displayName(text("リセットする").color(TextColor.fromHexString("#ffffff")))
        reset.itemMeta = resetMeta
        return reset
    }

    fun noBet(): ItemStack {
        val noBet = ItemStack(Material.BARRIER)
        val noBetMeta: ItemMeta = noBet.itemMeta
        noBetMeta.displayName(text("誰にも賭けられていません").color(TextColor.fromHexString("#ff0000")))
        noBet.itemMeta = noBetMeta
        return noBet
    }

    fun noUnderNotice(): ItemStack {
        val noUnderNotice = ItemStack(Material.BARRIER)
        val noUnderNoticeMeta: ItemMeta = noUnderNotice.itemMeta
        noUnderNoticeMeta.displayName(text("これ以上金額を減らすことはできません", TextColor.fromHexString("#ff0000")))
        noUnderNotice.itemMeta = noUnderNoticeMeta
        return noUnderNotice
    }

    fun noHaveMoney(): ItemStack {
        val noHaveMoney = ItemStack(Material.BARRIER)
        val noHaveMoneyMeta: ItemMeta = noHaveMoney.itemMeta
        noHaveMoneyMeta.displayName(text("あなたにはお金がありません", TextColor.fromHexString("#ff0000")))
        noHaveMoney.itemMeta = noHaveMoneyMeta
        return noHaveMoney
    }

}

