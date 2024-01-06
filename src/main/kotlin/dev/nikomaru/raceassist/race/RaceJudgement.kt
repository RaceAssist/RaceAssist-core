package dev.nikomaru.raceassist.race

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.manager.PlaceManager
import dev.nikomaru.raceassist.api.core.manager.RaceManager
import dev.nikomaru.raceassist.race.error.InitSettingError
import dev.nikomaru.raceassist.race.error.PlaceSettingError
import dev.nikomaru.raceassist.race.error.RaceSettingError
import dev.nikomaru.raceassist.utils.RaceAudience
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

abstract class RaceJudgement(open val raceId: String, open val executor: CommandSender) {

    lateinit var placeId: String

    lateinit var raceManager: RaceManager
    lateinit var plainPlaceManager: PlaceManager.PlainPlaceManager

    lateinit var locale: Locale
    lateinit var replacement: HashMap<UUID, String>

    var finished = false

    val jockeys: ArrayList<Player> = ArrayList()
    var jockeyCount = 0

    val finishJockeys = arrayListOf<UUID>()

    val currentLap = hashMapOf<UUID, Int>()

    val currentRanking = hashMapOf<UUID, Int>() // without finish

    val time = hashMapOf<UUID, Long>()

    val audiences = RaceAudience()

    var suspend = false

    var limit = 0L

    var beforeTime = 0L

    fun initSetting(): Result<Unit, InitSettingError> {
        raceManager = RaceAssist.api.getRaceManager(raceId) ?: return Err(InitSettingError.RACE_MANAER_NOT_FOUND)
        placeId = raceManager.getPlaceId()
        plainPlaceManager = (RaceAssist.api.getPlaceManager(placeId) as PlaceManager.PlainPlaceManager?) ?: return Err(
            InitSettingError.PLACE_MANAER_NOT_FOUND
        )
        locale = executor.locale()
        return Ok(Unit)
    }

    abstract suspend fun raceSetting(): Result<Unit, RaceSettingError>

    abstract suspend fun placeSetting(): Result<Unit, PlaceSettingError>

    fun audienceSetting() {
        Utils.audience[raceId]?.forEach {
            audiences.add(Bukkit.getOfflinePlayer(it))
        }
        jockeys.forEach {
            audiences.add(it)
        }
        if (executor is Player) {
            audiences.add(executor as Player)
        }
    }

    abstract suspend fun start() // count

    abstract suspend fun calculate() // ここで演算を行い、currentRankingに格納する

    abstract suspend fun display() // calculateで書き込まれたcurrentRankingをもとに、表示する

    abstract suspend fun finish() // finish

    abstract suspend fun payDividend() // payDividend

}