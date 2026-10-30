package fr.bourges.meteo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity() {

    companion object {
        private const val LATITUDE = 47.0810
        private const val LONGITUDE = 2.3988
        private const val PREFS = "meteo"
        private const val KEY_JSON = "last_json"
        private const val KEY_FETCHED_AT = "fetched_at"

        private val API_URL = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$LATITUDE&longitude=$LONGITUDE" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
            "precipitation,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
            "precipitation_probability_max,sunrise,sunset" +
            "&timezone=Europe%2FParis&forecast_days=7"

        private val FRENCH = Locale.FRANCE
    }

    private lateinit var progress: ProgressBar
    private lateinit var error: TextView
    private lateinit var updated: TextView
    private lateinit var icon: TextView
    private lateinit var temperature: TextView
    private lateinit var description: TextView
    private lateinit var details: TextView
    private lateinit var hourly: LinearLayout
    private lateinit var daily: LinearLayout
    private lateinit var refresh: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress)
        error = findViewById(R.id.error)
        updated = findViewById(R.id.updated)
        icon = findViewById(R.id.icon)
        temperature = findViewById(R.id.temperature)
        description = findViewById(R.id.description)
        details = findViewById(R.id.details)
        hourly = findViewById(R.id.hourly)
        daily = findViewById(R.id.daily)
        refresh = findViewById(R.id.refresh)

        refresh.setOnClickListener { load() }

        // Affiche immédiatement les dernières données connues (utile hors ligne)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_JSON, null)?.let { cached ->
            runCatching { render(JSONObject(cached), prefs.getString(KEY_FETCHED_AT, null)) }
        }

        load()
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        error.visibility = View.GONE
        refresh.isEnabled = false

        Thread {
            val result = runCatching { fetch() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                refresh.isEnabled = true
                result.onSuccess { body ->
                    val fetchedAt = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd/MM 'à' HH:mm", FRENCH))
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_JSON, body)
                        .putString(KEY_FETCHED_AT, fetchedAt)
                        .apply()
                    render(JSONObject(body), fetchedAt)
                }.onFailure { e ->
                    error.text = "Impossible de récupérer la météo.\n" +
                        "Vérifiez votre connexion internet.\n(${e.message ?: e.javaClass.simpleName})"
                    error.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun fetch(): String {
        val conn = URL(API_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun render(json: JSONObject, fetchedAt: String?) {
        updated.text = if (fetchedAt != null) "Mis à jour le $fetchedAt" else ""

        val current = json.getJSONObject("current")
        val code = current.getInt("weather_code")
        val isDay = current.optInt("is_day", 1) == 1
        icon.text = Weather.emoji(code, isDay)
        temperature.text = "${current.getDouble("temperature_2m").roundToInt()}°"
        description.text = Weather.label(code)

        val dailyJson = json.getJSONObject("daily")
        val sunrise = dailyJson.getJSONArray("sunrise").getString(0).substringAfter('T')
        val sunset = dailyJson.getJSONArray("sunset").getString(0).substringAfter('T')

        details.text = buildString {
            append("Ressenti ${current.getDouble("apparent_temperature").roundToInt()}°")
            append("  •  Humidité ${current.getInt("relative_humidity_2m")} %\n")
            append("Vent ${current.getDouble("wind_speed_10m").roundToInt()} km/h ")
            append(Weather.windDirection(current.getDouble("wind_direction_10m")))
            append("  •  Rafales ${current.getDouble("wind_gusts_10m").roundToInt()} km/h\n")
            append("Précipitations ${current.getDouble("precipitation")} mm\n")
            append("Lever $sunrise  •  Coucher $sunset")
        }

        renderHourly(json.getJSONObject("hourly"), current.getString("time"))
        renderDaily(dailyJson)
    }

    private fun renderHourly(h: JSONObject, currentTime: String) {
        hourly.removeAllViews()
        val times = h.getJSONArray("time")
        val temps = h.getJSONArray("temperature_2m")
        val codes = h.getJSONArray("weather_code")
        val rain = h.getJSONArray("precipitation_probability")
        val days = h.getJSONArray("is_day")

        // Index de l'heure courante (format "2026-10-30T14:00")
        val currentHour = currentTime.take(13)
        var start = 0
        for (i in 0 until times.length()) {
            if (times.getString(i).take(13) >= currentHour) { start = i; break }
        }

        val end = minOf(start + 24, times.length())
        for (i in start until end) {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(10), dp(6), dp(10), dp(6))
            }
            val hour = if (i == start) "Maint." else times.getString(i).substring(11, 13) + "h"
            col.addView(text(hour, 13f))
            col.addView(text(Weather.emoji(codes.getInt(i), days.optInt(i, 1) == 1), 26f))
            col.addView(text("${temps.getDouble(i).roundToInt()}°", 16f, bold = true))
            val p = rain.optInt(i, 0)
            col.addView(text(if (p > 0) "💧$p%" else " ", 11f))
            hourly.addView(col)
        }
    }

    private fun renderDaily(d: JSONObject) {
        daily.removeAllViews()
        val dates = d.getJSONArray("time")
        val codes = d.getJSONArray("weather_code")
        val maxs = d.getJSONArray("temperature_2m_max")
        val mins = d.getJSONArray("temperature_2m_min")
        val rain = d.getJSONArray("precipitation_probability_max")

        for (i in 0 until dates.length()) {
            val date = LocalDate.parse(dates.getString(i))
            val dayName = when (i) {
                0 -> "Aujourd'hui"
                1 -> "Demain"
                else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, FRENCH)
                    .replaceFirstChar { it.titlecase(FRENCH) } +
                    " " + date.dayOfMonth
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }
            row.addView(text(dayName, 16f).apply {
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            val p = rain.optInt(i, 0)
            row.addView(text(if (p > 0) "💧$p%" else "", 13f).apply {
                layoutParams = LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            row.addView(text(Weather.emoji(codes.getInt(i), true), 24f).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            row.addView(
                text(
                    "${mins.getDouble(i).roundToInt()}° / ${maxs.getDouble(i).roundToInt()}°",
                    16f, bold = true
                ).apply {
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(dp(84), LinearLayout.LayoutParams.WRAP_CONTENT)
                }
            )
            daily.addView(row)
        }
    }

    private fun text(value: String, sizeSp: Float, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(0xFFFFFFFF.toInt())
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
