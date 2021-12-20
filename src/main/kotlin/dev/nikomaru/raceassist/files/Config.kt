/*
 * Copyright © 2021 Nikomaru
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

class Config {
    private var config: FileConfiguration? = null
    var host: String? = null
    var port = 0
    var database: String? = null
    var username: String? = null
    var password: String? = null

    fun load() {
        plugin!!.saveDefaultConfig()
        if (config != null) {
            plugin!!.reloadConfig()
        }
        config = plugin!!.config
        if (!config!!.isString("SQLSetting.host")) {
            config!!["SQLSetting.host"] = "localhost"
        }
        if (!config!!.isInt("SQLSetting.port")) {
            config!!["SQLSetting.port"] = 3306
        }
        if (!config!!.isString("SQLSetting.database")) {
            config!!["SQLSetting.database"] = "raceassist"
        }
        if (!config!!.isString("SQLSetting.username")) {
            config!!["SQLSetting.username"] = "root"
        }
        if (!config!!.isString("SQLSetting.password")) {
            config!!["SQLSetting.password"] = ""
        }
        host = config!!.getString("SQLSetting.host")
        port = config!!.getInt("SQLSetting.port")
        database = config!!.getString("SQLSetting.database")
        username = config!!.getString("SQLSetting.username")
        password = config!!.getString("SQLSetting.password")
    }

    init {
        load()
    }
}


