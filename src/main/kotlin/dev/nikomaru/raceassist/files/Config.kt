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
package dev.nikomaru.raceassist.files

import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import org.bukkit.configuration.file.FileConfiguration

object Config {
    var config: FileConfiguration? = null

    var host: String? = null
    var port = 0
    var database: String? = null
    var username: String? = null
    var password: String? = null
    var threshold: Int? = null
    var discordWebHook: String? = null
    var betUnit: Int = 0
    var applicationName: String? = null
    var spreadsheetId: String? = null

    fun load() {
        if (config != null) {
            plugin!!.reloadConfig()
        }

        host = config!!.getString("SQLSettings.host")
        port = config!!.getInt("SQLSetting.port")
        database = config!!.getString("SQLSetting.database")
        username = config!!.getString("SQLSetting.username")
        password = config!!.getString("SQLSetting.password")
        threshold = config!!.getInt("RaceSetting.threshold")
        betUnit = config!!.getInt("RaceSetting.bet")
        discordWebHook = config!!.getString("NetworkSettings.discord")
        applicationName = config!!.getString("Sheets.applicationName")
        spreadsheetId = config!!.getString("Sheets.spreadsheetId")
    }

}


