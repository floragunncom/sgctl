package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.searchguard.sgctl.testsupport.QuietTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link LuceneRegexParser}.
 */
class LuceneRegexParserTest extends QuietTestBase {

    /**
     * Ensures non-regex input passes through unchanged.
     */
    @Test
    void shouldReturnInputWhenRegexIsNotEnabled() throws Exception {
        assertEquals("plain", LuceneRegexParser.toJavaRegex("plain"));
    }

    /**
     * Verifies basic operator conversions for Lucene-style regex inputs.
     */
    @Test
    void shouldConvertLuceneOperators() throws Exception {
        assertEquals("/a.*b/", LuceneRegexParser.toJavaRegex("/a@b/"));
        assertEquals("/a(?!)b/", LuceneRegexParser.toJavaRegex("/a#b/"));
        assertEquals("/a&b/", LuceneRegexParser.toJavaRegex("/a&b/"));
        assertEquals("/([1-3])/", LuceneRegexParser.toJavaRegex("/x<1-3>/"));
    }

    /**
     * Ensures complement operator is rejected for enabled regex input.
     */
    @Test
    void shouldRejectComplementOperator() {
        Exception exception = assertThrows(Exception.class, () -> LuceneRegexParser.toJavaRegex("/a~b/"));
        assertEquals("Encountered a complement operator '~'. This can not be perfectly represented in Java regex.", exception.getMessage());
    }

    /**
     * Ensures complement operator is ignored when regex markers are absent.
     */
    @Test
    void shouldIgnoreComplementWhenRegexIsNotEnabled() throws Exception {
        assertEquals("a~b", LuceneRegexParser.toJavaRegex("a~b"));
    }

    /**
     * Ensures escaped operator characters are not replaced.
     */
    @Test
    void shouldLeaveEscapedOperatorsUntouched() throws Exception {
        assertEquals("/a\\@b/", LuceneRegexParser.toJavaRegex("/a\\@b/"));
    }
}
