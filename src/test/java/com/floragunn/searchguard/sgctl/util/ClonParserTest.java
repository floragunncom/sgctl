package com.floragunn.searchguard.sgctl.util;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

class ClonParserTest {
    private static Stream<Pair<? extends ImmutableMap<String, ?>, String[]>> expressionStream() {
        return ImmutableList.ofArray(
                Pair.of(
                        ImmutableMap.of("key", "value"),
                        new String[]{"key=value"}),
                Pair.of(
                        ImmutableMap.of("key", "value/var"),
                        new String[]{"'key'='value/var'"}),
                Pair.of(
                        ImmutableMap.of("key", Boolean.TRUE),
                        new String[]{"key=true"}),
                Pair.of(
                        ImmutableMap.of("key", Boolean.FALSE),
                        new String[]{"key=false"}),
                Pair.of(
                        ImmutableMap.of("key", Double.valueOf(4.2e-3)),
                        new String[]{"key=4.2e-3"}),
                Pair.of(
                        ImmutableMap.of("key", null),
                        new String[]{"key=null"}),
                Pair.of(
                        ImmutableMap.of("key", ImmutableList.ofArray("one", "two", Long.valueOf(3))),
                        new String[]{"key=[one,two,3]"}),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key", "value")),
                        new String[]{"key=[key=value]"}),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("inner_key", ImmutableMap.of("long", Long.valueOf(3)))),
                        new String[]{"key[inner_key[long]]=3"}),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("inner_key", ImmutableMap.of("arr", ImmutableList.ofArray(Double.valueOf(3.5))))),
                        new String[]{"key[inner_key[arr[]]]=3.5"}),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key1", Long.valueOf(56256), "key2", "valid_string", "key3", ImmutableList.ofArray("content"))),
                        new String[]{"key=[key1=56256,key2=valid_string,key3=[content]]"}),
                Pair.of(
                        ImmutableMap.of("key", "value",
                                "obj", ImmutableMap.of(
                                        "inner_obj", ImmutableMap.of(
                                                "arr", ImmutableList.ofArray("one", Long.valueOf(-2), Long.valueOf(3)))),
                                "key2", "value2"),
                        new String[]{"key=value", "obj[inner_obj[arr]]=[one,-2,3]", "key2=value2"}),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key1", "value1", "key2", "value2")),
                        new String[]{"key=[key1=value1]", "key[key2]=value2"})
        ).stream();
    }

    @ParameterizedTest
    @MethodSource("expressionStream")
    public void testEvaluation(Pair<Map<String, Object>, String[]> pair) throws Exception {
        Map<String, Object> actual = ClonParser.parse(pair.getRight());
        Assertions.assertEquals(pair.getLeft(), actual);
    }

    private static Stream<ClonParser.ClonException.Builder> errorExpressionStream() {
        return ImmutableList.ofArray(
                ClonParser.ClonException.Builder.getNotEndExceptionBuilder().setExpression("key=[val]k").setIndex(9),
                ClonParser.ClonException.Builder.getCharacterNotFoundExceptionBuilder('=').setExpression("hello_world").setIndex(11),
                ClonParser.ClonException.Builder.getParenthesisOpenExceptionBuilder().setExpression("key=[value1,value2").setIndex(18),
                ClonParser.ClonException.Builder.getParenthesisCloseExceptionBuilder().setExpression("key=val]").setIndex(7),
                ClonParser.ClonException.Builder.getNoStringEndExceptionBuilder().setExpression("'key=value").setIndex(4),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder("Expression").setExpression("").setIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder("Key").setExpression("=54").setIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder("Value").setExpression("key=").setIndex(4),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder("Value").setExpression("key=[value1,value2,]").setIndex(19),
                ClonParser.ClonException.Builder.getUnsupportedSymbolExceptionBuilder('!').setExpression("bl!a=6").setIndex(2),
                ClonParser.ClonException.Builder.getNameEmptyExceptionBuilder().setExpression("''='moin'").setIndex(2)
        ).stream();
    }

    @ParameterizedTest
    @MethodSource("errorExpressionStream")
    public void testErrors(ClonParser.ClonException.Builder builder) throws Exception {
        ClonParser.ClonException expected = builder.build();
        ClonParser.ClonException actual = Assertions.assertThrowsExactly(expected.getClass(), () -> ClonParser.parse(builder.expression));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());
    }

    @Test
    public void testOverrideException() throws Exception {
        ClonParser.ClonException expected = ClonParser.ClonException.Builder.getOverrideExceptionBuilder("key").setExpression("key=value2").setIndex(3).build();
        ClonParser.ClonException actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("key=value", "key=value2"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());

        expected = ClonParser.ClonException.Builder.getOverrideExceptionBuilder("key").setExpression("obj[inner[key]]=[value]").setIndex(13).build();
        actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("obj=[inner=[key=3]]", "obj[inner[key]]=[value]"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());
    }
}