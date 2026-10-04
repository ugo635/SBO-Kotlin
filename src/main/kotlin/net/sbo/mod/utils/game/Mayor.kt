package net.sbo.mod.utils.game

import net.sbo.mod.SBOKotlin
import net.sbo.mod.utils.data.MayorResponse
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.http.Http
import java.util.*
import kotlin.math.floor

object Mayor {
    private const val SKYBLOCK_EPOCH = 1560276000L
    private const val SECONDS_PER_MONTH = 37200.0
    private const val SECONDS_PER_DAY = 1200.0
    private const val SECONDS_PER_HOUR = 50.0
    private const val MONTHS_IN_YEAR = 12
    private const val DAYS_IN_MONTH = 31
    private const val ELECTION_DAY = 27
    private const val ELECTION_MONTH = 3
    private const val SECONDS_PER_YEAR = SECONDS_PER_MONTH * MONTHS_IN_YEAR
    private const val DAY_MS = 86_400_000L

    private const val FALLBACK_MAYOR = "Diana"
    private const val FALLBACK_PERK = "Mythological Ritual"

    private var dateMayorElected: Date? = null
    private var newMayorAtDate: Date? = null
    private var mayor: String? = null
    private var perks: MutableSet<String> = mutableSetOf()
    private var mayorApiError: Boolean = false
    private var apiLastUpdated: Long? = null
    private var minister: String? = null
    private var ministerPerk: String? = null
    private var skyblockDate: Date? = null
    private var skyblockDateString: String = ""
    private var refreshingMayor: Boolean = false
    private var newMayor: Boolean = false
    private var outDatedApi: Boolean = false
    var mayorElectedYear = 0

    /** True when the current mayor is Diana. Note: the API-error fallback also sets Diana. */
    val isDiana: Boolean get() = mayor == "Diana" || ministerPerk == FALLBACK_PERK

    fun init() {
        refreshMayorData()
        Register.onTick(20 * 60) { refreshMayorData() }
    }

    /**
     * Real time (ms) at which the current mayor term started: the last Late Spring 27th, 00:00.
     * Computed from the clock only, so it is correct even if the mayor API has not refreshed yet.
     */
    fun currentTermStartMillis(now: Long = System.currentTimeMillis()): Long {
        // SKYBLOCK_EPOCH is 6:00 on Early Spring 1st of year 1, so year 1 starts 6 SkyBlock hours earlier
        val year1Start = SKYBLOCK_EPOCH - 6 * SECONDS_PER_HOUR
        val electionOffset = ((ELECTION_MONTH - 1) * DAYS_IN_MONTH + (ELECTION_DAY - 1)) * SECONDS_PER_DAY
        val firstElectionEnd = year1Start + electionOffset
        val years = floor((now / 1000.0 - firstElectionEnd) / SECONDS_PER_YEAR)
        return ((firstElectionEnd + years * SECONDS_PER_YEAR) * 1000).toLong()
    }

    /** 0 = first 24 h of the current term, 1 = next 24 h, ... (a term lasts 5 days 4 h, so 0..5). */
    fun eventDay(now: Long = System.currentTimeMillis()): Int =
        ((now - currentTermStartMillis(now)) / DAY_MS).toInt()

    private fun refreshMayorData() {
        skyblockDateString = calcSkyblockDate(System.currentTimeMillis())
        if (skyblockDateString.isEmpty()) return

        skyblockDate = convertStringToDate(skyblockDateString)
        updateMayorElection()
        updateSbYear()
        fetchMayorIfNeeded()
    }

    private fun updateMayorElection() {
        val skyDate = skyblockDate ?: return

        if (newMayorAtDate == null || newMayorAtDate!!.time < skyDate.time) {
            newMayor = true

            val calendar = Calendar.getInstance()
            calendar.time = skyDate
            val currentYear = calendar.get(Calendar.YEAR)

            val electionThisYear = date(ELECTION_DAY, ELECTION_MONTH, currentYear)

            if (electionThisYear.time > skyDate.time) {
                mayorElectedYear = currentYear - 1
                dateMayorElected = date(ELECTION_DAY, ELECTION_MONTH, currentYear - 1)
                newMayorAtDate = electionThisYear
            } else {
                mayorElectedYear = currentYear
                dateMayorElected = electionThisYear
                newMayorAtDate = date(ELECTION_DAY, ELECTION_MONTH, currentYear + 1)
            }
        }
    }

    private fun updateSbYear() {
        Calendar.getInstance().apply {
            time = skyblockDate!!
        }
    }

    private fun fetchMayorIfNeeded() {
        if (skyblockDate == null) return
        if (mayor != null && !mayorApiError && !newMayor && !outDatedApi) return
        if (refreshingMayor) return

        getMayor()
        newMayor = false
    }

    private fun getMayor() {
        mayor = null
        perks.clear()
        refreshingMayor = true

        Http.sendGetRequest("https://api.hypixel.net/resources/skyblock/election")
            .toJson<MayorResponse>(true) { response ->
                refreshingMayor = false
                if (!response.success) {
                    handleApiError(response.error ?: "Unknown error")
                    return@toJson
                }
                processMayorResponse(response)
            }
            .error { error ->
                handleApiError(error.message ?: "Request failed")
            }
    }

    private fun processMayorResponse(response: MayorResponse) {
        apiLastUpdated = response.lastUpdated
        mayor = response.mayor.name
        perks = response.mayor.perks.map { it.name }.toMutableSet()
        minister = response.mayor.minister?.name
        ministerPerk = response.mayor.minister?.perk?.name
        mayorApiError = false

        checkApiFreshness()
    }

    private fun checkApiFreshness() {
        val apiTimeStamp = apiLastUpdated ?: return
        val apiDate = convertStringToDate(calcSkyblockDate(apiTimeStamp))
        val electedDate = dateMayorElected

        outDatedApi = electedDate != null && apiDate.time < electedDate.time
        if (outDatedApi) applyFallback()
    }

    private fun handleApiError(message: String) {
        refreshingMayor = false
        mayorApiError = true
        applyFallback()
        SBOKotlin.logger.error("API Mayor error: $message")
    }

    private fun applyFallback() {
        mayor = FALLBACK_MAYOR
        perks = mutableSetOf(FALLBACK_PERK)
    }

    private fun date(day: Int, month: Int, year: Int): Date =
        GregorianCalendar(year, month - 1, day).time

    private fun convertStringToDate(string: String): Date {
        val parts = string.split(".")
        return date(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private fun calcSkyblockDate(date: Long): String {
        val unix = floor(date.toDouble() / 1000).toLong()
        var secondsSinceEpoch = unix - SKYBLOCK_EPOCH

        var year = 1
        var month = 1
        var day = 1
        var hour = 6

        val secondsPerYear = SECONDS_PER_MONTH * MONTHS_IN_YEAR

        val yearDiff = floor(secondsSinceEpoch / secondsPerYear).toInt()
        secondsSinceEpoch -= yearDiff * secondsPerYear.toLong()
        year += yearDiff

        val monthDiff = floor(secondsSinceEpoch / SECONDS_PER_MONTH).toInt() % 13
        secondsSinceEpoch -= monthDiff * SECONDS_PER_MONTH.toLong()
        month = (month + monthDiff).let { if (it == 0) 13 else it }

        val dayDiff = floor(secondsSinceEpoch / SECONDS_PER_DAY).toInt() % 32
        secondsSinceEpoch -= dayDiff * SECONDS_PER_DAY.toLong()
        day = (day + dayDiff).let { if (it == 0) DAYS_IN_MONTH else it }

        val hourDiff = floor(secondsSinceEpoch / SECONDS_PER_HOUR).toInt() % 24
        hour = (hour + hourDiff) % 24

        if (hour < 6) {
            day = if (day < DAYS_IN_MONTH) day + 1 else 1
            if (day == 1) month++
        }

        return "$day.$month.$year"
    }
}