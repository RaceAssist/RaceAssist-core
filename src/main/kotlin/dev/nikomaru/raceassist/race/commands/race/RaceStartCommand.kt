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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.race.PlainRaceJudgement
import dev.nikomaru.raceassist.race.PlaneVectorRaceJudgement
import dev.nikomaru.raceassist.race.RaceJudgement
import dev.nikomaru.raceassist.utils.SuggestionId
import kotlinx.coroutines.delay
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist race")
class RaceStartCommand {

    companion object {
        val stopPayment = hashSetOf<String>()
    }

    @CommandMethod("stopPayment <operateRaceId>")
    @CommandDescription("払い戻しを停止するコマンド")
    suspend fun stopPayment(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return

        stopPayment.add(raceId)
        sender.sendRichMessage("<color:green> 払い戻しを停止しました </color>")

    }

    @CommandPermission("raceassist.commands.race.start")
    @CommandMethod("start <operateRaceId>")
    @CommandDescription("レースを開始するコマンド")
    suspend fun start(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return

        val raceJudgement: RaceJudgement = when (RaceUtils.getPlaceType(raceManager.getPlaceId())) {
            PlaceType.PLAIN -> PlainRaceJudgement(raceId, sender)
            PlaceType.PLANE_VECTOR -> PlaneVectorRaceJudgement(raceId, sender)
            else -> {
                sender.sendRichMessage("<color:red> このコースはサポートされていません </color>")
                return
            }
        }

        raceJudgement.initSetting().onSuccess {
            sender.sendRichMessage("<color:green> 初期設定が完了しました </color>")
        }.onFailure {
            sender.sendRichMessage("<color:red> 初期設定中にエラーが発生しました </color>")
            return
        }
        raceJudgement.raceSetting().onSuccess {
            sender.sendRichMessage("<color:green> レース設定が完了しました </color>")
        }.onFailure {
            sender.sendRichMessage("<color:red> レース設定中にエラーが発生しました </color>")
            return
        }
        raceJudgement.placeSetting().onSuccess {
            sender.sendRichMessage("<color:green> コース設定が完了しました </color>")
        }.onFailure {
            sender.sendRichMessage("<color:red> コース設定中にエラーが発生しました </color>")
            return
        }
        raceJudgement.audienceSetting()
        sender.sendRichMessage("<color:green> 観客設定が完了しました </color>")

        raceJudgement.start()
        while (!raceJudgement.finished) {
            raceJudgement.calculate()
            raceJudgement.display()
        }

        raceJudgement.finish()
        if (!raceJudgement.suspend) {
            sender.sendRichMessage("<color:green> レースが終了しました </color>")
            repeat(3) {
                sender.sendRichMessage("<color:green> ${(4 - it) * 10}秒後に払い戻しが実行されます </color>")
                sender.sendRichMessage("<color:green> 支払いを停止するには/ra race stopPayment $raceId を実行してください </color>")
                delay(10000)
            }
            if (stopPayment.contains(raceId)) {
                sender.sendRichMessage("<color:green> 払い戻しを停止しました </color>")
                sender.sendRichMessage("<color:green> 手動で実行する場合は /ra ber pay $raceId <指定した騎手> を実行してください </color>")
                stopPayment.remove(raceId)
                return
            }
            raceJudgement.payDividend()
        }
    }
}

