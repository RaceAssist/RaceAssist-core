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

package dev.nikomaru.raceassist.web

import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import dev.nikomaru.raceassist.data.database.UserAuthData
import dev.nikomaru.raceassist.utils.Utils.passwordHash
import kotlinx.coroutines.Dispatchers
import org.apache.commons.lang.RandomStringUtils
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist web")
@CommandPermission("raceassist.commands.web")
class WebCommand {

    @CommandMethod("register")
    @CommandDescription("アカウントを登録します")
    suspend fun register(sender: CommandSender) {
        if (sender !is Player) return
        val uuid = sender.uniqueId

        val exist = newSuspendedTransaction(Dispatchers.IO) {
            UserAuthData.select(UserAuthData.uuid eq uuid.toString()).count() > 0
        }
        if (exist) {
            sender.sendMessage("すでに登録されています /ra web resetを実行して削除した後もう一度実行してください")
            return
        }
        val password = RandomStringUtils.randomAlphanumeric(20)
        val hashedPassword = passwordHash(password)
        newSuspendedTransaction(Dispatchers.IO) {
            UserAuthData.insert {
                it[UserAuthData.uuid] = uuid.toString()
                it[UserAuthData.hashedPassword] = hashedPassword
            }
        }

        sender.sendRichMessage("パスワードは $password です <yellow><click:copy_to_clipboard:'$password'>クリックでコピー</click></yellow>")
    }

    @CommandMethod("reset")
    @CommandDescription("アカウントをリセットします")
    suspend fun reset(sender: CommandSender) {
        if (sender !is Player) return
        val uuid = sender.uniqueId

        val exist = newSuspendedTransaction(Dispatchers.IO) {
            UserAuthData.select(UserAuthData.uuid eq uuid.toString()).count() > 0
        }
        if (!exist) {
            sender.sendRichMessage("登録されていません まずは /ra web registerを実行してください")
            return
        }
        newSuspendedTransaction(Dispatchers.IO) {
            UserAuthData.deleteWhere { UserAuthData.uuid eq uuid.toString() }
        }
        sender.sendRichMessage("ログインパスワードの削除が完了しました。")
    }

}