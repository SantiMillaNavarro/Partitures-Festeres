package com.partituresfesteres.app.ui

import java.text.Normalizer
import kotlin.math.max

/**
 * Búsqueda tolerante pero predecible.
 *
 * Prioridad:
 * 1) coincidencia exacta;
 * 2) empieza por la consulta;
 * 3) contiene la consulta;
 * 4) palabras/prefijos;
 * 5) pequeña tolerancia a erratas SOLO en términos de >= 4 caracteres.
 *
 * Esto evita el comportamiento demasiado permisivo de la v1.3 con consultas
 * cortas, donde una sola letra distinta podía considerarse una coincidencia.
 */
internal fun searchScore(query: String, vararg candidates: String): Int? {
    val q = normalizeSearch(query)
    if (q.isBlank()) return 0

    val fields = candidates.map(::normalizeSearch).filter { it.isNotBlank() }
    if (fields.isEmpty()) return null

    // Las coincidencias de frase completa dominan claramente el ranking.
    fields.forEachIndexed { index, field ->
        val fieldPenalty = index * 35
        when {
            field == q -> return 10_000 - fieldPenalty
            field.startsWith(q) -> return 9_200 - fieldPenalty - field.length.coerceAtMost(200)
            field.contains(q) -> return 8_400 - fieldPenalty - field.indexOf(q).coerceAtMost(200)
        }
    }

    val queryTokens = q.split(' ').filter { it.isNotBlank() }
    val candidateTokens = fields.flatMap { it.split(' ').filter { it.isNotBlank() } }
    if (candidateTokens.isEmpty()) return null

    var total = 0
    for (qToken in queryTokens) {
        var best: Int? = null
        for (token in candidateTokens) {
            val score = when {
                token == qToken -> 1_000
                token.startsWith(qToken) -> 920
                qToken.length >= 2 && token.contains(qToken) -> 820
                qToken.length >= 4 -> {
                    val maxDistance = when {
                        qToken.length >= 9 -> 2
                        else -> 1
                    }
                    val distance = damerauLevenshtein(qToken, token)
                    if (distance <= maxDistance) 700 - (distance * 90) else null
                }
                else -> null
            }
            if (score != null && (best == null || score > best)) best = score
        }
        if (best == null) return null
        total += best
    }

    // Pequeño bonus si todos los términos aparecen en el nombre principal.
    val title = fields.firstOrNull().orEmpty()
    if (queryTokens.all { it in title }) total += 180
    return total
}

internal fun smartMatches(query: String, vararg candidates: String): Boolean =
    searchScore(query, *candidates) != null

internal fun <T> smartSearch(
    query: String,
    items: List<T>,
    candidates: (T) -> List<String>,
): List<T> {
    if (query.isBlank()) return items
    return items.mapIndexedNotNull { index, item ->
        val score = searchScore(query, *candidates(item).toTypedArray()) ?: return@mapIndexedNotNull null
        RankedSearchItem(item, score, index)
    }
        .sortedWith(compareByDescending<RankedSearchItem<T>> { it.score }.thenBy { it.originalIndex })
        .map { it.item }
}

private data class RankedSearchItem<T>(
    val item: T,
    val score: Int,
    val originalIndex: Int,
)

private fun normalizeSearch(value: String): String {
    val normalized = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
    return normalized
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
        .replace("\\s+".toRegex(), " ")
}

private fun damerauLevenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    if (kotlin.math.abs(a.length - b.length) > max(2, a.length / 3)) return max(a.length, b.length)

    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j

    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            var value = minOf(
                d[i - 1][j] + 1,
                d[i][j - 1] + 1,
                d[i - 1][j - 1] + cost,
            )
            // Transposición adyacente (p. ej. "kablia" -> "kabila") cuenta como una errata.
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                value = minOf(value, d[i - 2][j - 2] + 1)
            }
            d[i][j] = value
        }
    }
    return d[a.length][b.length]
}
