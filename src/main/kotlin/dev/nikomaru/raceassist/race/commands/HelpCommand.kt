/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
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

package dev.nikomaru.raceassist.race.commands

import cloud.commandframework.annotations.*
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender

@CommandMethod("ra|raceassist")
class HelpCommand {
    @CommandMethod("help")
    @CommandPermission("raceassist.command.help")
    fun help(sender: CommandSender) {
        val message: String
        if (sender.hasPermission("raceassist.admin")) {
            message = """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist audience <st>          </st></gradient></b>
            <click:suggest_command:'/ra audience join'><color:#92fe9d>/ra audience join <raceId></click> 観客に自分を追加します
            <click:suggest_command:'/ra audience leave'><color:#92fe9d>/ra audience leave <raceId></click> 観客から自分を削除します
            <click:suggest_command:'/ra audience list'><color:#92fe9d>/ra audience list <raceId></click> 観客の一覧を表示
            <color:yellow><click:run_command:'/ra help setting'>  ← setting</click>               <click:run_command:'/ra help bet'>bet  →</click> 
            """.trimIndent()
        } else {
            message = """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist <st>          </st></gradient></b>
            <click:suggest_command:'/ra audience join'><color:#92fe9d>/ra audience join <raceId></click> 観客に自分を追加
            <click:suggest_command:'/ra audience leave'><color:#92fe9d>/ra audience leave <raceId></click> 観客から自分を削除
            <color:yellow><click:suggest_command:'/ra bet open'><color:#92fe9d>/ra bet open <raceId></click> 賭け画面を開く
            """.trimIndent()
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message))
    }

    @CommandMethod("help <tag>")
    @CommandPermission("raceassist.command.help")
    @Hidden
    fun help(sender: CommandSender, @Argument("tag") tag: String) {
        val message: String = when (tag) {
            "audience" -> {
                """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist audience <st>          </st></gradient></b>
            <click:suggest_command:'/ra audience join'><color:#92fe9d>/ra audience join <raceId></click> 観客に自分を追加します
            <click:suggest_command:'/ra audience leave'><color:#92fe9d>/ra audience leave <raceId></click> 観客から自分を削除します
            <click:suggest_command:'/ra audience list'><color:#92fe9d>/ra audience list <raceId></click> 観客の一覧を表示
            <color:yellow><click:run_command:'/ra help setting'>  ← setting</click>               <click:run_command:'/ra help bet'>bet  →</click> 
            """.trimIndent()
            }
            "bet" -> {
                """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist bet <st>          </st></gradient></b>
            <click:suggest_command:'/ra bet can'><color:#92fe9d>/ra bet can <raceId> on/off</click> 対象のレースに対して賭けが可能か変更
            <click:suggest_command:'/ra bet delete'><color:#92fe9d>/ra bet delete <raceId></click> 賭けを削除します 
            <click:suggest_command:'/ra bet list'><color:#92fe9d>/ra bet list <raceId></click> 賭けの一覧を表示します 
            <click:suggest_command:'/ra bet open'><color:#92fe9d>/ra bet open <raceId></click> 賭けをすることのできる画面を開く
            <click:suggest_command:'/ra bet rate'><color:#92fe9d>/ra bet rate <raceId></click> 賭けのレートを変更します 
            <click:suggest_command:'/ra bet revert'><color:#92fe9d>/ra bet revert <raceId></click> すべての人に返金します 
            <click:suggest_command:'/ra bet remove'><color:#92fe9d>/ra bet remove <raceId> <betRow></click> 指定した番号の賭けを返金 
            <click:suggest_command:'/ra bet sheet'><color:#92fe9d>/ra bet sheet <raceId> <SheetID></click> spreadsheetを登録します
            <color:yellow><click:run_command:'/ra help audience'>  ← audience</click>               <click:run_command:'/ra help place'>place  →</click>
            """.trimIndent()
            }
            "place" -> {
                """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist place <st>          </st></gradient></b>
            <click:suggest_command:'/ra place reverse'><color:#92fe9d>/ra place reverse <raceId></click> レースの走行方向の向きを反転 
            <click:suggest_command:'/ra place central'><color:#92fe9d>/ra place central <raceId></click> レースの中心点を設定 
            <click:suggest_command:'/ra place degree'><color:#92fe9d>/ra place degree <raceId></click> レースのゴールの角度を設定
            <click:suggest_command:'/ra place lap'><color:#92fe9d>/ra place lap <raceId> <lap></click> レースのラップ数を指定 
            <click:suggest_command:'/ra place set'><color:#92fe9d>/ra place set <raceId> in|out</click> レース場の内周、外周を指定 
            <click:suggest_command:'/ra place finish'><color:#92fe9d>/ra place finish</click> 上記の設定の終了 
            <color:yellow><click:run_command:'/ra help bet'> ← bet </click>               <click:run_command:'/ra help player'>player  →</click>
            """.trimIndent()
            }
            "player" -> {
                """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist player <st>          </st></gradient></b>
            <click:suggest_command:'/ra player add'><color:#92fe9d>/ra player add <raceId> <Player></click> 騎手を追加 
            <click:suggest_command:'/ra player remove'><color:#92fe9d>/ra player remove <raceId></click> 騎手を削除 
            <click:suggest_command:'/ra player delete'><color:#92fe9d>/ra player delete <raceId></click> 騎手をすべて削除 
            <click:suggest_command:'/ra player list'><color:#92fe9d>/ra player list <raceId></click> 騎手の一覧を表示 
            <color:yellow><click:run_command:'/ra help place'>  ← place</click>               <click:run_command:'/ra help race'>race  →</click>
            """.trimIndent()
            }
            "race" -> {
                """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist race <st>          </st></gradient></b>
            <click:suggest_command:'/ra race start'><color:#92fe9d>/ra race start <raceId></click> レースを開始 
            <click:suggest_command:'/ra race debug'><color:#92fe9d>/ra race debug <raceId></click> レースのデバッグ 
            <click:suggest_command:'/ra race stop'><color:#92fe9d>/ra race stop <raceId></click> レースの停止 
            <color:yellow><click:run_command:'/ra help player'>  ← player</click>               <click:run_command:'/ra help setting'>setting  →</click>
            """.trimIndent()
            }
            "setting" -> {
                """
            <b><gradient:#92fe9d:#00c9ff><st>          </st> RaceAssist setting <st>          </st></gradient></b>
            <click:suggest_command:'/ra setting create'><color:#92fe9d>/ra setting create <raceId></click> レースの作成
            <click:suggest_command:'/ra setting delete'><color:#92fe9d>/ra setting delete <raceId></click> レースの削除
            <click:suggest_command:'/ra setting copy'><color:#92fe9d>/ra setting copy <raceId_1> <raceId_2></click> レースをコピーします
            <color:yellow><click:run_command:'/ra help race'>  ← race</click>               <click:run_command:'/ra help audience'>audience →</click>
            """.trimIndent()
            }
            else -> {
                ""
            }
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message))

    }
}