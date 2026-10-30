package fr.bourges.meteo

/** Traduction des codes météo WMO renvoyés par Open-Meteo. */
object Weather {

    fun label(code: Int): String = when (code) {
        0 -> "Ciel dégagé"
        1 -> "Plutôt dégagé"
        2 -> "Partiellement nuageux"
        3 -> "Couvert"
        45 -> "Brouillard"
        48 -> "Brouillard givrant"
        51 -> "Bruine légère"
        53 -> "Bruine"
        55 -> "Bruine dense"
        56, 57 -> "Bruine verglaçante"
        61 -> "Pluie faible"
        63 -> "Pluie"
        65 -> "Pluie forte"
        66, 67 -> "Pluie verglaçante"
        71 -> "Neige faible"
        73 -> "Neige"
        75 -> "Neige forte"
        77 -> "Grains de neige"
        80 -> "Averses faibles"
        81 -> "Averses"
        82 -> "Averses violentes"
        85, 86 -> "Averses de neige"
        95 -> "Orage"
        96, 99 -> "Orage avec grêle"
        else -> "Inconnu"
    }

    fun emoji(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1 -> if (isDay) "🌤️" else "🌙"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "❔"
    }

    fun windDirection(degrees: Double): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
        return dirs[(((degrees % 360) + 360) % 360 / 45.0 + 0.5).toInt() % 8]
    }
}
