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


    val noDataGrass = GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE))

}

