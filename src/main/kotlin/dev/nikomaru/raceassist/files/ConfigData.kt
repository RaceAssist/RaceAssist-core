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

package dev.nikomaru.raceassist.files

import kotlinx.serialization.Serializable

@Serializable
data class ConfigData(
    val version: String,
    val threshold: Int,
    val delay: Long,
    val discordWebHook: DiscordWebHook,
    val spreadSheet: SpreadSheet,
    val recordHorse: RecordHorse,
    val resultWebhook: ArrayList<ResultWebhook>,
    val webAPI: WebAPI?,
    val raceLimitMilliSecond: Long,
    val mySQL: MySQL?,
)

@Serializable
data class DiscordWebHook(val race: ArrayList<String>,
    val bet: ArrayList<String>,
    val place: ArrayList<String>,
    val horse: ArrayList<String>,
    val web: ArrayList<String>)

@Serializable
data class RecordHorse(val minSpeed: Double, val minJump: Double)

@Serializable
data class ResultWebhook(val url: String, val name: String, val password: String)

@Serializable
data class SpreadSheet(val port: Int, val sheetName: ArrayList<String>)

@Serializable
data class WebAPI(val port: Int, val sslPort: Int, val sslSetting: SslSetting, val jwtConfig: JWTConfig?, val recordUrl: ArrayList<RecordLog>)

@Serializable
data class RecordLog(val url: String, val name: String, val password: String)

@Serializable
data class SslSetting(val keyAlias: String, val keyStorePassword: String, val privateKeyPassword: String)

@Serializable
data class MySQL(val url: String, val username: String, val password: String)

@Serializable
data class JWTConfig(val privateKey: String, val keyId: String, val issuer: String, val audience: ArrayList<String>, val realm: String = "RaceAssist")