package com.hacybeyker.snapdoc.feature.ocr.domain

import javax.inject.Inject

/**
 * The fallback that runs when there is no generative model — and, per the project's premise, it is
 * what keeps the app useful on the devices that cannot run one, which is most of them.
 *
 * It reads only what regular text actually spells out: a labelled total, a date in a common format,
 * and a document kind implied by the words used. It deliberately never guesses the merchant. The
 * tempting heuristic — "the first line is the shop" — is wrong often enough (it is just as likely to
 * be an address, a slogan or a stray logo caption) that filling the field would trade an honest gap
 * for a confident mistake, and a wrong merchant is worse than a missing one.
 */
class ExtractDocumentFieldsUseCase @Inject constructor() {

    operator fun invoke(text: String): DocumentInsight {
        if (text.isBlank()) return DocumentInsight.empty(InsightSource.Rules)
        val total = detectTotal(text)
        return DocumentInsight(
            kind = detectKind(text, hasTotal = total != null),
            merchant = null,
            date = DATE.find(text)?.value,
            total = total,
            source = InsightSource.Rules
        )
    }

    /**
     * [hasTotal] carries most of the weight. Plenty of real receipts never print the word "receipt"
     * — they are a shop name, a list of prices and a total — so a labelled total is the strongest
     * signal the rules get. It is checked after the invoice keywords because invoices have totals too.
     */
    private fun detectKind(text: String, hasTotal: Boolean): DocumentKind {
        val upper = text.uppercase()
        return when {
            KIND_KEYWORDS.getValue(DocumentKind.Invoice).any { it in upper } -> DocumentKind.Invoice
            KIND_KEYWORDS.getValue(DocumentKind.IdDocument).any { it in upper } -> DocumentKind.IdDocument
            KIND_KEYWORDS.getValue(DocumentKind.Receipt).any { it in upper } -> DocumentKind.Receipt
            hasTotal -> DocumentKind.Receipt
            else -> DocumentKind.Note
        }
    }

    /**
     * A labelled total beats a bigger number elsewhere: on a receipt the largest amount on the page
     * is often the cash tendered or a subtotal before discount, so the label is the reliable signal.
     */
    private fun detectTotal(text: String): String? {
        val labelled = text.lineSequence()
            .firstOrNull { line -> TOTAL_LABELS.any { it in line.uppercase() } }
            ?.let { AMOUNT.findAll(it).lastOrNull()?.value }
        return labelled?.trim()
    }

    private companion object {
        val DATE = Regex("""\b(\d{4}-\d{2}-\d{2}|\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""")

        /** An optional symbol, then digits with either separator, then exactly two decimals. */
        val AMOUNT = Regex("""[€$£]?\s?\d{1,3}(?:[.,]\d{3})*[.,]\d{2}""")

        val TOTAL_LABELS = listOf("TOTAL", "IMPORTE", "AMOUNT DUE", "BALANCE DUE")

        val KIND_KEYWORDS = mapOf(
            DocumentKind.Invoice to listOf("INVOICE", "FACTURA", "TAX INVOICE"),
            DocumentKind.Receipt to listOf("RECEIPT", "RECIBO", "TICKET", "SUBTOTAL", "CHANGE DUE"),
            DocumentKind.IdDocument to listOf("PASSPORT", "IDENTITY CARD", "DNI", "DRIVING LICENCE", "DRIVER LICENSE")
        )
    }
}
