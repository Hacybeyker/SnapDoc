package com.hacybeyker.snapdoc.feature.library.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildFtsQueryUseCaseTest {

    private val useCase = BuildFtsQueryUseCase()

    @Test
    fun `every word becomes a prefix so a partial word still finds the document`() {
        assertEquals("hardware* store*", useCase("hardware store"))
    }

    @Test
    fun `full-text operators are stripped instead of being obeyed`() {
        assertEquals("hardware* store*", useCase("hardware AND -store"))
        assertEquals("obrien*", useCase("O'Brien"))
        assertEquals("total*", useCase("\"total\""))
    }

    @Test
    fun `punctuation between words does not glue them together`() {
        assertEquals("total* 1630*", useCase("total, 16.30"))
    }

    @Test
    fun `digits are searchable, so an amount can be looked up`() {
        assertEquals("1630*", useCase("16.30"))
    }

    @Test
    fun `an empty box means the whole archive, not an empty result`() {
        assertNull(useCase(""))
        assertNull(useCase("   "))
    }

    @Test
    fun `a query with nothing searchable in it also means the whole archive`() {
        assertNull(useCase("--- \"\" ***"))
    }

    @Test
    fun `a pasted paragraph is capped instead of building a huge query`() {
        val pasted = (1..20).joinToString(" ") { "word$it" }

        assertEquals(8, useCase(pasted)?.split(" ")?.size)
    }
}
