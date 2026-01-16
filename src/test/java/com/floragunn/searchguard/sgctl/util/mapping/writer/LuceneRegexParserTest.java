/*
 * Copyright 2025-2026 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


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
