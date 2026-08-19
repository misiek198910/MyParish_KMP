package com.example.mojaparafia.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

fun parseHtmlToAnnotatedString(htmlText: String): AnnotatedString {
    if (htmlText.isBlank()) return AnnotatedString("")

    // 1. Podstawowe czyszczenie encji HTML
    var cleaned = htmlText
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    // 2. Zamiana łamania linii / akapitów na przejścia do nowej linii
    cleaned = cleaned
        .replace(Regex("(?i)<br\\s*/?>"), "\n")
        .replace(Regex("(?i)</p>"), "\n\n")
        .replace(Regex("(?i)</div>"), "\n")
        .replace(Regex("(?i)<li>"), "• ")
        .replace(Regex("(?i)</li>"), "\n")

    // 3. Usuwanie pozostałych tagów HTML (np. <div>, <span>, <table> itp.) z zachowaniem pogrubień/kursywy
    val regex = Regex("<[^>]*>")

    return buildAnnotatedString {
        var currentIndex = 0
        // Znajdujemy tagi <b>, <strong>, <i>, <em>
        val tagRegex = Regex("(?i)<(b|strong|i|em)>(.*?)</\\1>")
        val matches = tagRegex.findAll(cleaned)

        var lastIndex = 0
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            val tagName = match.groupValues[1].lowercase()
            val innerText = match.groupValues[2]

            // Dodajemy tekst przed tagiem (usuwając z niego ewentualne inne tagi)
            append(cleaned.substring(lastIndex, start).replace(regex, ""))

            // Dodajemy sformatowany tekst wewnątrz tagu
            val styleStart = length
            append(innerText.replace(regex, ""))
            val styleEnd = length

            if (tagName == "b" || tagName == "strong") {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), styleStart, styleEnd)
            } else if (tagName == "i" || tagName == "em") {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), styleStart, styleEnd)
            }

            lastIndex = end
        }

        // Dodajemy pozostałą część tekstu po ostatnim tagu
        if (lastIndex < cleaned.length) {
            append(cleaned.substring(lastIndex).replace(regex, ""))
        }
    }
}