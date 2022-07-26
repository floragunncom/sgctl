package com.floragunn.searchguard.sgctl.util;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class ClonParserTest {
    private static Stream<Pair<Map<String, ?>, List<String>>> expressionStream() {
        List<Pair<Map<String, ?>, List<String>>> list = ImmutableList.ofArray(
                Pair.of(
                        ImmutableMap.of("key", "value"),
                        ImmutableList.ofArray("key=value")),
                Pair.of(
                        ImmutableMap.of("key", "value/var"),
                        ImmutableList.ofArray("'key'='value/var'")),
                Pair.of(
                        ImmutableMap.of("key", Boolean.TRUE),
                        ImmutableList.ofArray("key=true")),
                Pair.of(
                        ImmutableMap.of("key", Boolean.FALSE),
                        ImmutableList.ofArray("key=false")),
                Pair.of(
                        ImmutableMap.of("key", Double.valueOf(4.2e-3)),
                        ImmutableList.ofArray("key=4.2e-3")),
                Pair.of(
                        ImmutableMap.of("key", null),
                        ImmutableList.ofArray("key=null")),
                Pair.of(
                        ImmutableMap.of("key", ImmutableList.ofArray("one", "two", Long.valueOf(3))),
                        ImmutableList.ofArray("key=[one,two,3]")),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key", "value")),
                        ImmutableList.ofArray("key=[key=value]")),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("inner_key", ImmutableMap.of("long", Long.valueOf(3)))),
                        ImmutableList.ofArray("key[inner_key[long]]=3")),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("inner_key", ImmutableMap.of("arr", ImmutableList.ofArray(Double.valueOf(3.5))))),
                        ImmutableList.ofArray("key[inner_key[arr[]]]=3.5")),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key1", Long.valueOf(56256), "key2", "valid_string", "key3", ImmutableList.ofArray("content"))),
                        ImmutableList.ofArray("key=[key1=56256,key2=valid_string,key3=[content]]")),
                Pair.of(
                        ImmutableMap.of("key", "value",
                                "obj", ImmutableMap.of(
                                        "inner_obj", ImmutableMap.of(
                                                "arr", ImmutableList.ofArray("one", Long.valueOf(-2), Long.valueOf(3)))),
                                "key2", "value2"),
                        ImmutableList.ofArray("key=value", "obj[inner_obj[arr]]=[one,-2,3]", "key2=value2")),
                Pair.of(
                        ImmutableMap.of("key", ImmutableMap.of("key1", "value1", "key2", "value2")),
                        ImmutableList.ofArray("key=[key1=value1]", "key[key2]=value2")),
                Pair.of(
                        ImmutableMap.of("key", ""),
                        ImmutableList.ofArray("key=''")),
                Pair.of(ImmutableMap.of("key", "val\"ue"),
                        ImmutableList.ofArray("key='val\"ue'")),
                Pair.of(ImmutableMap.of("key'", ImmutableMap.of("inner\"", ImmutableList.ofArray(ImmutableMap.of("bla", "value")))),
                        ImmutableList.ofArray("\"key'\"['inner\"'[]]=[bla=value]"))
        );
        return list.stream();
    }

    @ParameterizedTest
    @MethodSource("expressionStream")
    public void testEvaluation(Pair<Map<String, ?>, List<String>> pair) throws Exception {
        Map<String, ?> actual = ClonParser.parse(pair.getRight());
        Assertions.assertEquals(pair.getLeft(), actual);
    }

    private static Stream<ClonParser.ClonException.Builder> errorExpressionStream() {
        return ImmutableList.ofArray(
                ClonParser.ClonException.Builder.getNotEndExceptionBuilder().setExpression("key=[val]k").setErrorIndex(9).setPart("key=[val]k").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getCharacterNotFoundExceptionBuilder('=').setExpression("hello_world").setErrorIndex(11).setPart("hello_world").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getParenthesisOpenExceptionBuilder().setExpression("key=[value1,value2").setErrorIndex(18).setPart("[value1,value2").setPartStartIndex(4),
                ClonParser.ClonException.Builder.getParenthesisCloseExceptionBuilder().setExpression("key=val]").setErrorIndex(7).setPart("key=val]").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getNoStringEndExceptionBuilder().setExpression("'key=value").setErrorIndex(4).setPart("'key").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.EXPRESSION).setExpression("").setErrorIndex(0).setPart("").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.KEY).setExpression("=54").setErrorIndex(0).setPart("").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.VALUE).setExpression("key=").setErrorIndex(4).setPart("").setPartStartIndex(4),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.VALUE).setExpression("key=[value1,value2,]").setErrorIndex(19).setPart("[value1,value2,]").setPartStartIndex(4),
                ClonParser.ClonException.Builder.getUnsupportedSymbolExceptionBuilder('!').setExpression("bl!a=6").setErrorIndex(2).setPart("bl!a").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getNameEmptyExceptionBuilder(ClonParser.PartType.KEY).setExpression("''='moin'").setErrorIndex(2).setPart("''").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getParenthesisOpenExceptionBuilder().setExpression("transient[logger.com.floragunn=TRACE]").setErrorIndex(30).setPart("transient[logger.com.floragunn").setPartStartIndex(0)
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
        ClonParser.ClonException expected = ClonParser.ClonException.Builder.getOverrideExceptionBuilder("key").setExpression("key=value2").setErrorIndex(3).setPart("key").setPartStartIndex(0).build();
        ClonParser.ClonException actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("key=value", "key=value2"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());

        expected = ClonParser.ClonException.Builder.getOverrideExceptionBuilder("key").setExpression("obj[inner[key]]=[value]").setErrorIndex(13).setPart("key").setPartStartIndex(10).build();
        actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("obj=[inner=[key=3]]", "obj[inner[key]]=[value]"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());
    }
}