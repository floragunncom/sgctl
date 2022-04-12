package com.floragunn.searchguard.sgctl.util;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

class ClonParserTest {
    private static Stream<ExpressionExpectedPair> expressionStream() {
        return ImmutableList.ofArray(
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", "value"),
                        "key=value"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", "value/var"),
                        "'key'='value/var'"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", Boolean.TRUE),
                        "key=true"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", Boolean.FALSE),
                        "key=false"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", Double.valueOf(4.2e-3)),
                        "key=4.2e-3"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", null),
                        "key=null"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", ImmutableList.ofArray("one", "two", Long.valueOf(3))),
                        "key=[one,two,3]"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key", "value")),
                        "key=[key=value]"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", ImmutableMap.of("inner_key", ImmutableMap.of("long", Long.valueOf(3)))),
                        "key[inner_key[long]]=3"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", ImmutableMap.of("inner_key", ImmutableMap.of("arr", ImmutableList.ofArray(Double.valueOf(3.5))))),
                        "key[inner_key[arr[]]]=3.5"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key1", Long.valueOf(56256), "key2", "valid_string", "key3", ImmutableList.ofArray("content"))),
                        "key=[key1=56256,key2=valid_string,key3=[content]]"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", "value",
                                "obj", ImmutableMap.of(
                                        "inner_obj", ImmutableMap.of(
                                                "arr", ImmutableList.ofArray("one", Long.valueOf(-2), Long.valueOf(3)))),
                                "key2", "value2"),
                        "key=value", "obj[inner_obj[arr]]=[one,-2,3]", "key2=value2"),
                ExpressionExpectedPair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key1", "value1", "key2", "value2")),
                        "key=[key1=value1]", "key[key2]=value2")
        ).stream();
    }

    @ParameterizedTest
    @MethodSource("expressionStream")
    public void testEvaluation(ExpressionExpectedPair pair) throws Exception {
        Map<String, Object> actual = ClonParser.parse(pair.expressions);
        Assertions.assertEquals(pair.expected, actual);
    }

    @Test
    public void testErrors() throws Exception {
        Assertions.assertThrows(ClonParser.ClonException.class, () -> ClonParser.parse(""));
    }

    private static class ExpressionExpectedPair {
        public static ExpressionExpectedPair of(Map<String, Object> expected, String ... expressions) {
            return new ExpressionExpectedPair(expected, expressions);
        }
        private final String[] expressions;
        private final Map<String, Object> expected;
        protected ExpressionExpectedPair(Map<String, Object> expected, String ... expressions) {
            this.expressions = expressions;
            this.expected = expected;
        }
    }
}