package com.hacybeyker.snapdoc.feature.library.domain

import javax.inject.Inject

/**
 * Turns what the user typed into something SQLite's full-text engine will accept.
 *
 * This is not politeness, it is the difference between a search box and a crash: `MATCH` takes a
 * query *language*, so a stray quote, a lone `-`, or the word `AND` are syntax, and a bare
 * apostrophe from "O'Brien" is enough to throw. Every token is therefore reduced to letters and
 * digits and the operators never survive.
 *
 * Each token also gets a `*`, because someone typing "hard" while looking for a hardware receipt
 * expects a hit — full-text matching is by whole word unless prefix matching is asked for.
 */
class BuildFtsQueryUseCase @Inject constructor() {

    /** Returns null when nothing searchable is left, which means "show everything" and not "no results". */
    operator fun invoke(rawQuery: String): String? = rawQuery
        .split(*SEPARATORS)
        .map { token -> token.filter(Char::isLetterOrDigit).lowercase() }
        .filter { it.isNotEmpty() && it.uppercase() !in OPERATOR_WORDS }
        .take(MAX_TOKENS)
        .joinToString(separator = " ") { "$it*" }
        .ifEmpty { null }

    private companion object {
        val SEPARATORS = charArrayOf(' ', '\t', '\n', ',', ';')

        /**
         * Dropped rather than escaped. Stripping punctuation alone leaves these as ordinary words,
         * and a prefix search for `and*` matches almost every document in the archive — the search
         * would look like it worked while quietly returning everything.
         */
        val OPERATOR_WORDS = setOf("AND", "OR", "NOT", "NEAR")

        /** A pasted paragraph would build a query slower than the scan it is meant to find. */
        const val MAX_TOKENS = 8
    }
}
