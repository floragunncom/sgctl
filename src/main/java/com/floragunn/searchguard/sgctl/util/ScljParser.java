package com.floragunn.searchguard.sgctl.util;

import com.google.common.base.Strings;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ScljParser {
    public static Map<String, Object> evaluateExpression(String ... expressions) throws ScljParseException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String expression : expressions) {
            CharacterIterator it = new StringCharacterIterator(expression);
            String key = getNextKeyString(it, expression);
            it.next();
            String value = getNextValueString(it, expression);
            Object object = evaluateValue(value);
            evaluateKey(key, object, result);
        }
        return result;
    }

    protected static Map<String, Object> evaluateKey(String key, Object value, final Map<String, Object> map) throws ScljParseException {
        if (isSimpleKey(key))
            return evaluateSimpleKey(key, value, map);
        if (isArrayKey(key))
            return evaluateArrayKey(key, value, map);
        if (isObjectKey(key))
            return evaluateObjectKey(key, value, map);
        throw new ScljParseException("Cannot evaluate key '" + key + "'");
    }

    public static Object evaluateValue(String value) throws ScljParseException {
        if (isLongValue(value))
            return getLongValue(value);
        if (isDoubleValue(value))
            return getDoubleValue(value);
        if (isStringValue(value))
            return getStringValue(value);
        if (isArrayValue(value))
            return getArrayValue(value);
        if (isObjectValue(value))
            return getObjectValue(value);
        throw new ScljParseException("Object type not known for '" + value + "'");
    }

    private static boolean isKey(String key) {
        return isSimpleKey(key) || isArrayKey(key) || isObjectKey(key);
    }

    private static String simplifyKey(String key) {
        if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\"")))
            return key.substring(1, key.length() - 1);
        return key;
    }

    private static boolean isSimpleKey(String key) {
        if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) return true;
        return !key.contains("[") && !key.contains("]") && !key.contains(" ") && !key.contains("=") && !key.contains(",");
    }

    private static Map<String, Object> evaluateSimpleKey(String key, Object value, Map<String, Object> map) throws ScljParseException {
        if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\"")))
            key = key.substring(1, key.length() - 1);
        if(map.containsKey(key)) {
            throw ScljParseException.buildOverrideException(key, map);
        }
        map.put(key, value);
        return map;
    }

    private static boolean isArrayKey(String key) {
        return key.endsWith("[]") && isSimpleKey(key.substring(0, key.length() - 2));
    }

    private static Map<String, Object> evaluateArrayKey(String key, Object value, Map<String, Object> map) throws ScljParseException {
        key = simplifyKey(key.substring(0, key.length() - 2));
        if (map.containsKey(key)) {
            if (map.get(key) instanceof List) {
                List array = (List) map.get(key);
                array.add(value);
            }
            else {
                throw ScljParseException.buildOverrideException(key, map);
            }
        }
        else {
            List<Object> list = new ArrayList<>();
            list.add(value);
            map.put(key, list);
        }
        return map;
    }

    private static boolean isObjectKey(String key) {
        return key.endsWith("]") && key.contains("[") && isSimpleKey(key.substring(0, key.indexOf('[')));
    }

    private static Map<String, Object> evaluateObjectKey(String key, Object value, Map<String, Object> map) throws ScljParseException {
        String expression = key.substring(key.indexOf('[') + 1, key.length() - 1);
        key = simplifyKey(key.substring(0, key.indexOf('[')));
        if (map.containsKey(key)) {
            if (map.get(key) instanceof Map) {
                Map<String, Object> object = (Map<String, Object>) map.get(key);
                evaluateKey(expression, value, object);
            }
            else {
                throw ScljParseException.buildOverrideException(key, map);
            }
        }
        else {
            Map<String, Object> subObject = evaluateKey(expression, value, new LinkedHashMap<>());
            map.put(key, subObject);
        }
        return map;
    }

    private static boolean isStringValue(String value) {
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) return true;
        return !value.contains("[") && !value.contains("]") && !value.contains(" ") && !value.contains("=") && !value.contains(",");
    }

    private static String getStringValue(String value) {
        if ((value.startsWith("'") && value.endsWith("'")) || value.startsWith("\"") && value.endsWith("\""))
            return value.substring(1, value.length() - 1);
        return value;
    }

    private static boolean isLongValue(String value) {
        try {
            Long.parseLong(value);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private static Long getLongValue(String value) {
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isDoubleValue(String value) {
        try {
            Double.parseDouble(value);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private static Double getDoubleValue(String value) {
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isArrayValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            return !(value.contains("=") && isKey(value.substring(1, value.indexOf('='))));
        }
        return false;
    }

    private static List<Object> getArrayValue(String arrayString) throws ScljParseException {
        String contentString = arrayString.substring(1, arrayString.length() - 1);
        List<String> content = new ArrayList<>();
        for (CharacterIterator it = new StringCharacterIterator(contentString); it.current() != CharacterIterator.DONE; it.next()) {
            content.add(getNextValueString(it, contentString));
        }
        List<Object> objects = new ArrayList<>();
        for (String objectString : content)
            objects.add(evaluateValue(objectString));
        return objects;
    }

    private static boolean isObjectValue(String value) {
        return value.startsWith("[") && value.endsWith("]") && value.contains("=") && isKey(value.substring(1, value.indexOf('=')));
    }

    private static Object getObjectValue(String values) throws ScljParseException {
        String contentString = values.substring(1, values.length() - 1);
        Map<String, String> content = new LinkedHashMap<>();
        for (CharacterIterator it = new StringCharacterIterator(contentString); it.current() != CharacterIterator.DONE; it.next()) {
            String key = getNextKeyString(it, contentString);
            it.next();
            String value = getNextValueString(it, contentString);
            content.put(key, value);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : content.entrySet()) {
            Object object = evaluateValue(entry.getValue());
            evaluateKey(entry.getKey(), object, result);
        }
        return result;
    }

    private static String getNextKeyString(final CharacterIterator it, String expression) throws ScljParseException {
        Stack<Character> parenthesis = new Stack<>();
        StringBuilder builder = new StringBuilder();
        while (it.current() != CharacterIterator.DONE) {
            builder.append(it.current());
            switch (it.current()) {
                case '\'':
                case '"':
                    if (!parenthesis.empty() && parenthesis.peek() == it.current()) parenthesis.pop();
                    else parenthesis.push(it.current());
                    break;
                case '[':
                    if (parenthesis.empty() || (parenthesis.peek() != '"' && parenthesis.peek() != '\'')) parenthesis.push(it.current());
                    break;
                case ']':
                    if (parenthesis.empty()) throw ScljParseException.buildParenthesisException("]", expression, it.getIndex());
                    if (parenthesis.peek() != '"' && parenthesis.peek() != '\'') {
                        if (parenthesis.peek() == '[') parenthesis.pop();
                        else throw ScljParseException.buildParenthesisException("]", expression, it.getIndex());
                    }
                    break;
            }
            it.next();
            if (parenthesis.empty() && it.current() == '=') {
                return builder.toString();
            }
        }
        throw new ScljParseException("Could not evaluate key from '" + expression + "'");
    }

    private static String getNextValueString(final CharacterIterator it, String expression) throws ScljParseException {
        Stack<Character> parenthesis = new Stack<>();
        StringBuilder builder = new StringBuilder();
        while (it.current() != CharacterIterator.DONE) {
            builder.append(it.current());
            switch (it.current()) {
                case '\'':
                case '"':
                    if (!parenthesis.empty() && parenthesis.peek() == it.current()) parenthesis.pop();
                    else parenthesis.push(it.current());
                    break;
                case '[':
                    if (parenthesis.empty() || (parenthesis.peek() != '"' && parenthesis.peek() != '\'')) parenthesis.push(it.current());
                    break;
                case ']':
                    if (parenthesis.empty()) throw ScljParseException.buildParenthesisException("]", expression, it.getIndex());
                    if (parenthesis.peek() != '"' && parenthesis.peek() != '\'') {
                        if (parenthesis.peek() == '[') parenthesis.pop();
                        else throw ScljParseException.buildParenthesisException("]", expression, it.getIndex());
                    }
                    break;
            }
            it.next();
            if (parenthesis.empty() && (it.current() == ',' || it.current() == CharacterIterator.DONE)) {
                return builder.toString();
            }
        }
        throw new ScljParseException("Could not evaluate a value from '" + expression + "'");
    }

    public static class ScljParseException extends Exception {
        private static final long serialVersionUID = -454544173645956128L;

        public static ScljParseException buildParenthesisException(String unexpected, String expression, int index) {
            return new ScljParseException("Unexpected '" + unexpected + "' in expression: \n" + expression + "\n" + Strings.repeat(" ", index) + "^");
        }

        public static ScljParseException buildOverrideException(String key, Map<String, Object> map) {
            return new ScljParseException("Cannot override value of field '" + key + "' in " + map);
        }

        public ScljParseException(String message) {
            super(message);
        }

        public ScljParseException(String message, Map<String, Object> map) {

        }
    }
}
