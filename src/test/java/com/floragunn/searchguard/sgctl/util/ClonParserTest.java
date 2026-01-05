package com.floragunn.searchguard.sgctl.util;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Coverage for {@link ClonParser} parsing of CLON expressions and error reporting.
 */
class pClonParserTest {
    /**
     * Provides input/output pairs for valid CLON expressions.
     *
     * @return stream of parse input paired with the expected object
     */
    private static Stream<Pair<Object, List<String>>> inputStream() {
        List<Pair<Object, List<String>>> list = ImmutableList.ofArray(
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
                Pair.of(ImmutableMap.of("key", "value \"inner\""),
                        ImmutableList.ofArray("key='value \"inner\"'")),
                Pair.of(ImmutableMap.of("key", "[brackets]"),
                        ImmutableList.ofArray("key='[brackets]'")),
                Pair.of(ImmutableMap.of("key'", ImmutableMap.of("inner\"", ImmutableList.ofArray(ImmutableMap.of("bla", "value")))),
                        ImmutableList.ofArray("\"key'\"['inner\"'[]]=[bla=value]")),

                Pair.of("daniel", ImmutableList.of("daniel")),
                Pair.of(Boolean.TRUE, ImmutableList.of("true")),
                Pair.of(Long.valueOf(5), ImmutableList.of("5")),
                Pair.of(Double.valueOf(.5), ImmutableList.of("0.5")),
                Pair.of(ImmutableList.ofArray("moin", Long.valueOf(2), "pippo"), ImmutableList.of("[moin,2,pippo]")),
                Pair.of(ImmutableMap.of("key", "value", "num", Long.valueOf(22)), ImmutableList.of("[key=value,num=22]")),
                Pair.of(ImmutableList.of("str1", "str2", Long.valueOf(5)), ImmutableList.of("str1", "str2", "5")),
                Pair.of(null, ImmutableList.of("null")),
                Pair.of("", ImmutableList.of("''")),
                Pair.of(ImmutableList.of(ImmutableMap.of("key1", "value1"), ImmutableMap.of("key2", "value2")), ImmutableList.of("[key1=value1]", "[key2=value2]"))
        );
        return list.stream();
    }

    /**
     * Verifies that valid CLON expressions parse to the expected object structure.
     *
     * @param pair expected object paired with the CLON expression list
     */
    @ParameterizedTest
    @MethodSource("inputStream")
    public void testInputEvaluation(Pair<Object, List<String>> pair) throws Exception {
        Object actual = ClonParser.parse(pair.getRight());
        Assertions.assertEquals(pair.getLeft(), actual);
    }

    /**
     * Provides builders for expected exception details.
     *
     * @return stream of exception builders representing invalid inputs
     */
    private static Stream<ClonParser.ClonException.Builder> errorExpressionStream() {
        return ImmutableList.ofArray(
                ClonParser.ClonException.Builder.getNotEndExceptionBuilder().setExpression("key=[val]k").setErrorIndex(9).setPart("key=[val]k").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getParenthesisOpenExceptionBuilder().setExpression("key=[value1,value2").setErrorIndex(18).setPart("[value1,value2").setPartStartIndex(4),
                ClonParser.ClonException.Builder.getParenthesisCloseExceptionBuilder().setExpression("key=val]").setErrorIndex(7).setPart("key=val]").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getNoStringEndExceptionBuilder().setExpression("'key=value").setErrorIndex(10).setPart("'key=value").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.KEY).setExpression("=54").setErrorIndex(0).setPart("").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.VALUE).setExpression("key=").setErrorIndex(4).setPart("").setPartStartIndex(4),
                ClonParser.ClonException.Builder.getEmptyExceptionBuilder(ClonParser.PartType.VALUE).setExpression("key=[value1,value2,]").setErrorIndex(19).setPart("[value1,value2,]").setPartStartIndex(4),
                ClonParser.ClonException.Builder.getUnsupportedSymbolExceptionBuilder('!').setExpression("bl!a=6").setErrorIndex(2).setPart("bl!a").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getNameEmptyExceptionBuilder(ClonParser.PartType.KEY).setExpression("''='moin'").setErrorIndex(2).setPart("''").setPartStartIndex(0),
                ClonParser.ClonException.Builder.getNotEndExceptionBuilder().setExpression("transient[logger.com.floragunn=TRACE]").setErrorIndex(9).setPart("transient[logger.com.floragunn=TRACE]").setPartStartIndex(0)
        ).stream();
    }

    /**
     * Verifies that malformed expressions raise the expected exception type and message.
     *
     * @param builder expected exception builder
     */
    @ParameterizedTest
    @MethodSource("errorExpressionStream")
    public void testExpressionEvaluationErrors(ClonParser.ClonException.Builder builder) {
        ClonParser.ClonException expected = builder.build();
        ClonParser.ClonException actual = Assertions.assertThrowsExactly(expected.getClass(), () -> ClonParser.parse(builder.expression));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());
    }

    /**
     * Verifies override detection for duplicate keys across inputs.
     */
    @Test
    public void testOverrideException() {
        ClonParser.ClonException expected = ClonParser.ClonException.Builder.getOverrideExceptionBuilder("key").setExpression("key=value2").setErrorIndex(3).setPart("key").setPartStartIndex(0).build();
        ClonParser.ClonException actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("key=value", "key=value2"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());

        expected = ClonParser.ClonException.Builder.getOverrideExceptionBuilder("key").setExpression("obj[inner[key]]=[value]").setErrorIndex(13).setPart("key").setPartStartIndex(10).build();
        actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("obj=[inner=[key=3]]", "obj[inner[key]]=[value]"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());
    }

    /**
     * Verifies detection of mixed value and expression inputs.
     */
    @Test
    public void testValueExpressionMixException() {
        ClonParser.ClonException expected = ClonParser.ClonException.Builder.getExpressionValueMixException(ClonParser.PartType.VALUE).setExpression("key=value").setErrorIndex(0).setPart("key=value").setPartStartIndex(0).build();
        ClonParser.ClonException actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("str", "key=value"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());

        expected = ClonParser.ClonException.Builder.getExpressionValueMixException(ClonParser.PartType.EXPRESSION).setExpression("str").setErrorIndex(0).setPart("str").setPartStartIndex(0).build();
        actual = Assertions.assertThrows(expected.getClass(), () -> ClonParser.parse("key=value", "str"));
        Assertions.assertEquals(expected.getMessage(), actual.getMessage());
    }

    /**
     * Verifies mixed object and value entries inside an array raise a parsing error.
     */
    @Test
    void testMixedObjectAndValueInArray() {
        ClonParser.ClonException exception = Assertions.assertThrows(
                ClonParser.ClonException.class,
                () -> ClonParser.parse("key=[a=1,b]")
        );
        Assertions.assertTrue(exception.getMessage().contains("Expected"));
    }

    /**
     * Verifies unsupported symbols (such as escaping) are rejected unless quoted.
     */
    @Test
    void testEscapedBracketIsRejectedWhenNotQuoted() {
        ClonParser.ClonException exception = Assertions.assertThrows(
                ClonParser.ClonException.class,
                () -> ClonParser.parse("key=val\\]")
        );
        Assertions.assertTrue(exception.getMessage().contains("Unsupported symbol"));
    }

    /**
     * Verifies randomly generated key/value expressions parse into the expected map.
     */
    @Test
    void shouldParseRandomSimpleExpressions() throws Exception {
        Random random = new Random(42);

        for (int i = 0; i < 100; i++) {
            int entries = 1 + random.nextInt(5);
            Map<String, Object> expected = new LinkedHashMap<>();
            List<String> expressions = new ArrayList<>();

            for (int j = 0; j < entries; j++) {
                String key = randomToken(random, 3, 8);
                String value = randomToken(random, 1, 10);
                expected.put(key, value);
                expressions.add(key + "=" + value);
            }

            Object actual = ClonParser.parse(expressions);
            Assertions.assertEquals(expected, actual);
        }
    }

    /**
     * Verifies random invalid symbols are rejected in unquoted values.
     */
    @Test
    void shouldRejectRandomInvalidSymbols() {
        Random random = new Random(7);

        for (int i = 0; i < 25; i++) {
            String key = randomToken(random, 3, 6);
            String value = randomToken(random, 2, 5) + "!" + randomToken(random, 2, 5);
            Assertions.assertThrows(ClonParser.ClonException.class, () -> ClonParser.parse(key + "=" + value));
        }
    }

    /**
     * Generates an alphanumeric token using the provided random source.
     *
     * @param random random source
     * @param minLength minimum length
     * @param maxLength maximum length
     * @return random token
     */
    private String randomToken(Random random, int minLength, int maxLength) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int selector = random.nextInt(3);
            char c;
            if (selector == 0) {
                c = (char) ('a' + random.nextInt(26));
            } else if (selector == 1) {
                c = (char) ('A' + random.nextInt(26));
            } else {
                c = (char) ('0' + random.nextInt(10));
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
