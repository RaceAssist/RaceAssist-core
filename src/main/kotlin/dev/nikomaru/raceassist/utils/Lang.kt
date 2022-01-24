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

package dev.nikomaru.raceassist.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

object Lang {
    private val properties = Properties()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            val input: InputStream = this.javaClass.classLoader.getResourceAsStream("lang/${Locale.getDefault()}.properties")
                ?: this.javaClass.classLoader.getResourceAsStream("lang/ja_JP.properties")!!
            val isr = InputStreamReader(input, "UTF-8")

            properties.load(isr)
        }
    }

    fun getText(key: String): String {
        return properties.getProperty(key) ?: "サーバー管理者にこのメッセージが出たことを伝えてください"
    }

}