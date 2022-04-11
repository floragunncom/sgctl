package com.floragunn.searchguard.sgctl.util;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ScljParserTest {

    @Test
    public void evaluateKeyTest() throws Exception {
        String expression = "key=value";
        Map<String, Object> expected = new HashMap<>();
        expected.put("key", "value");
        Map<String, Object> actual = ScljParser.evaluateKey("key", "value", new HashMap<>());
        Assertions.assertEquals(expected, actual);

        expression = "obj[key]=value";
        expected = new HashMap<>();
        expected.put("obj", ImmutableMap.of("key", "value"));
        actual = ScljParser.evaluateKey("obj[key]", "value", new HashMap<>());
        Assertions.assertEquals(expected, actual);

        expression = "obj[arr[]]=3";
        expected = ImmutableMap.of("obj",
                ImmutableMap.of("arr",
                        ImmutableList.of(3)));
        actual = ScljParser.evaluateKey("obj[arr[]]", 3, new HashMap<>());
        Assertions.assertEquals(expected, actual);

        expected = ImmutableMap.of("Bla \"blub\" :=%", "'value\"with shit %&=='");
        actual = ScljParser.evaluateKey("'Bla \"blub\" :=%'", "'value\"with shit %&=='", new HashMap<>());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void evaluateObjectTest() throws Exception {
        long expectedLong = 5624562;
        Object actualLong = ScljParser.evaluateValue("5624562");
        Assertions.assertEquals(expectedLong, actualLong);

        double expectedDouble = 25642.13243;
        Object actualDouble = ScljParser.evaluateValue("25642.13243");
        Assertions.assertEquals(expectedDouble, actualDouble);

        String expectedString = "Hello World\n this is an awkward == string with many strange Symbols &= \"";
        Object actualString = ScljParser.evaluateValue("'Hello World\n this is an awkward == string with many strange Symbols &= \"'");
        Assertions.assertEquals(expectedString, actualString);

        List<Object> expectedArray = ImmutableList.ofArray(Long.valueOf(5), Long.valueOf(624), Double.valueOf(3.2));
        Object actualList = ScljParser.evaluateValue("[5,624,3.2]");
        Assertions.assertEquals(expectedArray, actualList);

        expectedArray = ImmutableList.ofArray(Long.valueOf(452), ImmutableList.ofArray("string", Double.valueOf(4.6), ImmutableList.ofArray(Long.valueOf(6))), "value");
        actualList = ScljParser.evaluateValue("[452,[string,4.6,[6]],value]");
        Assertions.assertEquals(expectedArray, actualList);

        Map<String, Object> expectedObject = ImmutableMap.of("key", "value", "obj", ImmutableMap.of("in", "side"));
        Object actualObject = ScljParser.evaluateValue("[key=value,obj=[in=side]]");
        Assertions.assertEquals(expectedObject, actualObject);
    }

    @Test
    public void evaluateExpressionTest() throws Exception {
        Map<String, Object> expectedObject = ImmutableMap.of("key", "value",
                "obj", ImmutableMap.of(
                        "inner_obj", ImmutableMap.of(
                                "arr", ImmutableList.ofArray("one", Long.valueOf(-2), Long.valueOf(3)),
                                "key", "test")),
                "key2", "value2");
        Object actualObject = ScljParser.evaluateExpression("key=value", "obj[inner_obj[arr]]=[one,-2,3]", "obj[inner_obj[key]]=test", "key2=value2");
        Assertions.assertEquals(expectedObject, actualObject);
    }

    @Test
    public void evaluateExpressionTest2() throws Exception {
        Map<String, Object> expectedObject = ImmutableMap.of("key", "value",
                "obj", ImmutableMap.of(
                        "inner_obj", ImmutableMap.of(
                                "arr", ImmutableList.ofArray("one", Long.valueOf(-2), Long.valueOf(3)),
                                "key", "test")),
                "key2", "value2");
        Object actualObject = ClonExpressionEvaluator.evaluate("key=value", "obj[inner_obj[arr]]=[one,-2,3]", "obj[inner_obj[key]]=test", "key2=value2");
        Assertions.assertEquals(expectedObject, actualObject);
    }
}