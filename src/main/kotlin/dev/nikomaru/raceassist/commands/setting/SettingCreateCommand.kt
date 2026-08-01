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

package dev.nikomaru.raceassist.commands.setting

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.Regex
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.lang.Lang
import dev.nikomaru.raceassist.commands.utils.SuggestionId
import dev.nikomaru.raceassist.data.value.CommonId
import dev.nikomaru.raceassist.data.value.IdentifiablePlaceId
import dev.nikomaru.raceassist.data.value.IdentifiableRaceId
import dev.nikomaru.raceassist.data.value.PlaceId
import dev.nikomaru.raceassist.data.value.RaceId
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist setting")
class SettingCreateCommand {
    @CommandPermission("raceassist.commands.setting.create")
    @CommandMethod("create <raceId> <placeId>")
    suspend fun create(
        sender: CommandSender,
        @Argument(value = "raceId") @Regex(value = CommonId.ID_REGEX) raceId: RaceId,
        @Argument(value = "placeId", suggestions = SuggestionId.PLACE_ID) placeId: PlaceId
    ) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        if (RaceUtils.existsRace(raceId)) {
            sender.sendMessage(Lang.getComponent("already-used-the-name-race", sender.locale()))
            return
        }
        if (!RaceUtils.existsPlace(placeId)) {
            sender.sendMessage(Lang.getComponent("not-exists-place", sender.locale()))
            return
        }
        RaceAssist.api.getDataManager().createRace(raceId, placeId, sender)
        sender.sendMessage(Lang.getComponent("to-create-race", sender.locale()))

    }
}