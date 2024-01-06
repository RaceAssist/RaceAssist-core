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

package dev.nikomaru.raceassist.bet

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*

object GuiComponent {

    fun guiComponent(): Component {
        return Component.text("レース賭け自販機")
    }

    fun onceUp(locale: Locale, raceId: String): ItemStack {
        val onceUp = ItemStack(Material.PINK_STAINED_GLASS_PANE)
        val onceUpMeta: ItemMeta = onceUp.itemMeta
        onceUpMeta.displayName(
            Lang.getComponent(
                "to-bet-one-unit",
                locale,
                RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
            )
        )
        onceUp.itemMeta = onceUpMeta
        return onceUp
    }

    fun onceDown(locale: Locale, raceId: String): ItemStack {
        val onceDown = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        val onceDownMeta: ItemMeta = onceDown.itemMeta
        onceDownMeta.displayName(
            Lang.getComponent(
                "to-cancel-bet-one-unit",
                locale,
                RaceAssist.api.getBetManager(raceId)!!.getBetUnit()
            )
        )
        onceDown.itemMeta = onceDownMeta
        return onceDown
    }

    fun tenTimesUp(locale: Locale, raceId: String): ItemStack {
        val tenTimesUp = ItemStack(Material.RED_STAINED_GLASS_PANE)
        val tenTimesUpMeta: ItemMeta = tenTimesUp.itemMeta
        tenTimesUpMeta.displayName(
            Lang.getComponent(
                "to-bet-ten-unit",
                locale,
                RaceAssist.api.getBetManager(raceId)!!.getBetUnit() * 10
            )
        )
        tenTimesUp.itemMeta = tenTimesUpMeta
        return tenTimesUp
    }

    fun tenTimesDown(locale: Locale, raceId: String): ItemStack {
        val tenTimesDown = ItemStack(Material.BLUE_STAINED_GLASS_PANE)
        val tenTimesDownMeta: ItemMeta = tenTimesDown.itemMeta
        tenTimesDownMeta.displayName(
            Lang.getComponent(
                "to-cancel-bet-ten-unit",
                locale,
                RaceAssist.api.getBetManager(raceId)!!.getBetUnit() * 10
            )
        )
        tenTimesDown.itemMeta = tenTimesDownMeta
        return tenTimesDown
    }

    fun accept(locale: Locale): ItemStack {
        val accept = ItemStack(Material.GREEN_WOOL)
        val acceptMeta: ItemMeta = accept.itemMeta
        acceptMeta.displayName(Lang.getComponent("gui-decide", locale))
        accept.itemMeta = acceptMeta
        return accept
    }

    fun deny(locale: Locale): ItemStack {
        val deny = ItemStack(Material.RED_WOOL)
        val denyMeta: ItemMeta = deny.itemMeta
        denyMeta.displayName(Lang.getComponent("gui-cancel", locale))
        deny.itemMeta = denyMeta
        return deny
    }

    fun reset(locale: Locale): ItemStack {
        val reset = ItemStack(Material.WHITE_WOOL)
        val resetMeta: ItemMeta = reset.itemMeta
        resetMeta.displayName(Lang.getComponent("gui-reset", locale))
        reset.itemMeta = resetMeta
        return reset
    }

    fun noBet(locale: Locale): ItemStack {
        val noBet = ItemStack(Material.BARRIER)
        val noBetMeta: ItemMeta = noBet.itemMeta
        noBetMeta.displayName(Lang.getComponent("no-one-betting", locale))
        noBet.itemMeta = noBetMeta
        return noBet
    }

    fun noUnderNotice(locale: Locale): ItemStack {
        val noUnderNotice = ItemStack(Material.BARRIER)
        val noUnderNoticeMeta: ItemMeta = noUnderNotice.itemMeta
        noUnderNoticeMeta.displayName(Lang.getComponent("cannot-decrease-more-money", locale))
        noUnderNotice.itemMeta = noUnderNoticeMeta
        return noUnderNotice
    }

    fun noHaveMoney(locale: Locale): ItemStack {
        val noHaveMoney = ItemStack(Material.BARRIER)
        val noHaveMoneyMeta: ItemMeta = noHaveMoney.itemMeta
        noHaveMoneyMeta.displayName(Lang.getComponent("no-have-money", locale))
        noHaveMoney.itemMeta = noHaveMoneyMeta
        return noHaveMoney
    }

    val noDataGrass = GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE))

}

