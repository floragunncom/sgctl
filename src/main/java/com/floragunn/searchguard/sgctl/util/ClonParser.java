package com.floragunn.searchguard.sgctl.util;

import com.google.common.base.Strings;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ClonParser {
    private static final List<Character> STRING_INDICATORS = Arrays.asList('"', '\'');
    private static final char ASSIGN_OPERATOR = '=';
    private static final Character EXPRESSION_OPEN = '[';
    private static final Character EXPRESSION_CLOSE = ']';
    private static final Character SEPARATOR = ',';

    public static Map<String, Object> parse(String ... expressions) throws ClonException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String expression : expressions) {
            Iterator iterator = new Iterator(expression);
            new ExpressionReader(iterator).read(result);
        }
        return result;
    }

    private static boolean isOperationSymbol(char c) {
        return c == SEPARATOR || c == ASSIGN_OPERATOR || c == EXPRESSION_OPEN || c == EXPRESSION_CLOSE;
    }

    private static boolean isNameSymbol(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private static class ExpressionReader {
        private final Iterator it;

        private ExpressionReader(Iterator it) {
            this.it = it;
        }

        public Map<String, Object> read(Map<String, Object> map) throws ClonException {
            it.validateNotEmpty("Expression");
            it.peekParenthesisCorrect();
            KeyReader.ValueApplier applier = new KeyReader(it).read(map);
            it.readCharacter(ASSIGN_OPERATOR);
            Object value = new ValueReader(it).read();
            it.validateEndAndParenthesis();
            applier.apply(value);
            return map;
        }
    }

    private static class KeyReader {
        private final Iterator it;

        public KeyReader(Iterator it) {
            this.it = it;
        }

        private ValueApplier read(Map<String, Object> map) throws ClonException {
            it.validateNotEmpty("Key");
            String key = it.readString();
            if (it.current() == ASSIGN_OPERATOR || it.current() == EXPRESSION_CLOSE) {
                if (map.containsKey(key)) throw ClonException.buildOverrideException(it, key);
                return new ValueApplier(key, map);
            }

            it.readCharacter(EXPRESSION_OPEN);
            if (it.current() == EXPRESSION_CLOSE) {
                ValueApplier applier;
                if (!map.containsKey(key)) {
                    List<Object> array = new ArrayList<>();
                    map.put(key, array);
                    applier = new ValueApplier(key, array);
                }
                else if (map.get(key) instanceof List)
                    applier = new ValueApplier(key, (List) map.get(key));
                else throw ClonException.buildOverrideException(it, key);
                it.readCharacter(EXPRESSION_CLOSE);
                return applier;
            }
            ValueApplier applier;
            if (!map.containsKey(key)) {
                Map<String, Object> innerMap = new LinkedHashMap<>();
                map.put(key, innerMap);
                applier = read(innerMap);
            }
            else if(map.get(key) instanceof Map) {
                applier = read((Map<String, Object>) map.get(key));
            }
            else throw ClonException.buildOverrideException(it, key);
            it.readCharacter(EXPRESSION_CLOSE);
            return applier;
        }

        private static class ValueApplier {
            private final String key;
            private final List<Object> list;
            private final Map<String, Object> map;

            public ValueApplier(String key, Map<String, Object> map) {
                this.key = key;
                this.list = null;
                this.map = map;
            }

            public ValueApplier(String key, List<Object> list) {
                this.key = key;
                this.list = list;
                this.map = null;
            }

            public void apply(Object object) throws ClonException {
                if (list != null) list.add(object);
                else if (map != null) map.put(key, object);
                else throw new ClonException("Could not resolve object tree");
            }
        }
    }

    private static class ValueReader {
        private final Iterator it;

        public ValueReader(Iterator it) {
            this.it = it;
        }

        public Object read() throws ClonException {
            it.validateNotEmpty("Value");
            Object result;
            String expression = it.getCurrentExpression();
            if (expression.charAt(0) == EXPRESSION_OPEN) {
                if (expression.charAt(1) == EXPRESSION_CLOSE) throw ClonException.buildEmptyException(it, "Array or Object");
                if (Iterator.peekContainsOnDepth(it, 1, ASSIGN_OPERATOR))
                    result = readObject();
                else
                    result = readArray();
            }
            else if (isLong()) result = readLong();
            else if (isDouble()) result = readDouble();
            else if ("true".equals(expression) || "false".equals(expression)) result = readBoolean();
            else if ("null".equals(expression)) result = readNull();
            else result = readString();
            it.validateEndAndParenthesis();
            return result;
        }

        private boolean isLong() {
            try {
                Long.valueOf(it.getCurrentExpression());
                return true;
            }
            catch (NumberFormatException ignored) {
                return false;
            }
        }

        private boolean isDouble() {
            try {
                Double.valueOf(it.getCurrentExpression());
                return true;
            }
            catch (NumberFormatException ignored) {
                return false;
            }
        }

        private boolean isString() {
            String expression = it.getCurrentExpression();
            if (STRING_INDICATORS.contains(expression.charAt(0))) return true;
            for (Character c : expression.toCharArray()) {
                if (!isNameSymbol(c)) return false;
            }
            return true;
        }

        private Object readLong() throws ClonException {
            return Long.valueOf(it.readNotEncapsulated());
        }

        private Object readDouble() throws ClonException {
            return Double.valueOf(it.readNotEncapsulated());
        }

        private Object readBoolean() throws ClonException {
            return "true".equals(it.readNotEncapsulated()) ? Boolean.TRUE : Boolean.FALSE;
        }

        private Object readNull() throws ClonException {
            it.readNotEncapsulated();
            return null;
        }

        private Object readString() throws ClonException {
            return it.readString();
        }

        private Object readArray() throws ClonException {
            it.readCharacter(EXPRESSION_OPEN);
            List<Object> values = new ArrayList<>();
            while (it.current() != EXPRESSION_CLOSE) {
                int valueStart = it.getIndex();
                it.readUntilMatchOnSameDepth(SEPARATOR, EXPRESSION_CLOSE);
                int valueEnd = it.getIndex();
                ValueReader reader = new ValueReader(new Iterator(it, valueStart, valueEnd));
                values.add(reader.read());
                if (it.current() != EXPRESSION_CLOSE) it.readCharacter(SEPARATOR);
            }
            it.readCharacter(EXPRESSION_CLOSE);
            return values;
        }

        private Object readObject() throws ClonException {
            it.readCharacter(EXPRESSION_OPEN);
            Map<String, Object> objectMap = new LinkedHashMap<>();
            while (it.current() != EXPRESSION_CLOSE) {
                int expressionStart = it.getIndex();
                it.readUntilMatchOnSameDepth(SEPARATOR, EXPRESSION_CLOSE);
                int expressionEnd = it.getIndex();
                ExpressionReader reader = new ExpressionReader(new Iterator(it, expressionStart, expressionEnd));
                reader.read(objectMap);
                if (it.current() != EXPRESSION_CLOSE) it.readCharacter(SEPARATOR);
            }
            it.readCharacter(EXPRESSION_CLOSE);
            return objectMap;
        }
    }

    private static class Iterator {
        private final String expression;
        private final StringCharacterIterator it;
        private final Stack<Character> parenthesis = new Stack<>();
        private final int start;
        private final int end;

        public Iterator(String expression, int start, int end) {
            this.expression = expression;
            it = new StringCharacterIterator(expression, start, end, start);
            this.start = start;
            this.end = end;
        }

        public Iterator(Iterator it, int start, int end) {
            this(it.expression, start, end);
        }

        public Iterator(Iterator other) {
            this(other, other.start, other.end);
        }

        public Iterator(String expression) {
            this(expression, 0, expression.length());
        }

        public static boolean peekContainsOnDepth(Iterator iterator, int depth, Character ... characters) throws ClonException {
            Iterator it = new Iterator(iterator);
            while (!it.isEnd()) {
                if (Arrays.asList(characters).contains(it.current()) && it.parenthesis.size() == depth) return true;
                it.next();
            }
            return false;
        }

        public String getCurrentExpression() {
            return expression.substring(getIndex(), end);
        }

        public String getCompleteExpression() {
            return expression;
        }

        public void peekParenthesisCorrect() throws ClonException {
            Iterator it = new Iterator(this);
            while (!it.isEnd()) {
                it.next();
            }
            it.validateParenthesis();
        }

        public String readUntilMatchOnSameDepth(Character ... chars) throws ClonException {
            StringBuilder builder = new StringBuilder();
            int depth = parenthesis.size();
            while (!Arrays.asList(chars).contains(it.current()) || parenthesis.size() != depth) {
                if (it.current() == CharacterIterator.DONE) throw ClonException.buildExpectedCharNotFoundException(this, chars);
                builder.append(it.current());
                next();
            }
            return builder.toString();
        }

        protected String readString() throws ClonException {
            if (STRING_INDICATORS.contains(it.current())) return readEncapsulatedString(it.current());
            StringBuilder name = new StringBuilder();
            while (!isOperationSymbol(it.current()) && !isEnd()) {
                if (!isNameSymbol(it.current())) throw ClonException.buildUnsupportedSymbolException(this);
                name.append(it.current());
                next();
            }
            if (name.toString().isEmpty()) throw ClonException.buildNameEmptyException(this);
            return name.toString();
        }

        private String readEncapsulatedString(Character parenthesis) throws ClonException {
            readCharacter(parenthesis);
            StringBuilder name = new StringBuilder();
            while (it.current() != parenthesis) {
                if (isEnd()) throw ClonException.buildParenthesisStillOpenException(this);
                name.append(it.current());
                next();
            }
            readCharacter(parenthesis);
            if (name.toString().isEmpty()) throw ClonException.buildNameEmptyException(this);
            return name.toString();
        }

        protected String readNotEncapsulated() throws ClonException {
            StringBuilder name = new StringBuilder();
            while (!isOperationSymbol(it.current()) && !isEnd()) {
                name.append(it.current());
                next();
            }
            return name.toString();
        }

        public boolean inString() {
            return !parenthesis.empty() && STRING_INDICATORS.contains(parenthesis.peek());
        }

        public char current() {
            return it.current();
        }

        public Character readCharacter(Character ... expectedCurrent) throws ClonException {
            if (!Arrays.asList(expectedCurrent).contains(it.current())) throw ClonException.buildUnexpectedCharacterException(this, it.current());
            Character current = it.current();
            next();
            return current;
        }

        public char next() throws ClonException {
            if (STRING_INDICATORS.contains(it.current())) {
                if (inString() && it.current() == parenthesis.peek()) parenthesis.pop();
                else parenthesis.push(it.current());
            }
            if (!inString()) {
                if (it.current() == EXPRESSION_OPEN) parenthesis.push(it.current());
                if (it.current() == EXPRESSION_CLOSE) {
                    if (!parenthesis.empty()) parenthesis.pop();
                    else throw ClonException.buildParenthesisClosedException(this);
                }
            }
            return it.next();
        }

        public int getIndex() {
            return it.getIndex();
        }

        public boolean isEnd() {
            return it.current() == CharacterIterator.DONE;
        }

        public void validateNotEmpty(String context) throws ClonException {
            if (Strings.isNullOrEmpty(expression)) throw ClonException.buildEmptyException(this, context);
        }

        public void validateEnd() throws ClonException {
            if (!isEnd()) throw ClonException.buildExpectedEndException(this);
        }

        public void validateParenthesis() throws ClonException {
            if (!parenthesis.empty()) throw ClonException.buildParenthesisStillOpenException(this);
        }

        public void validateEndAndParenthesis() throws ClonException {
            validateEnd();
            validateParenthesis();
        }
    }

    public static class ClonException extends Exception {
        public static ClonException buildUnexpectedCharacterException(Iterator iterator, char c) {
            return new ClonException(buildExceptionMessage("Unexpected character '" + c + "'", iterator));
        }

        public static ClonException buildExpectedEndException(Iterator iterator) {
            return new ClonException(buildExceptionMessage("Expected expression to end but found '" + iterator.getCompleteExpression().substring(iterator.getIndex(), iterator.end) + "' instead", iterator));
        }

        public static ClonException buildExpectedCharNotFoundException(Iterator iterator, Character ... chars) {
            return new ClonException(buildExceptionMessage("Expected " + Arrays.toString(chars) + " in expression '" + iterator.getCurrentExpression() + "'", iterator));
        }

        public static ClonException buildParenthesisStillOpenException(Iterator iterator) {
            char missing = STRING_INDICATORS.contains(iterator.parenthesis.peek()) ? iterator.parenthesis.peek() : EXPRESSION_CLOSE;
            return new ClonException(buildExceptionMessage("Expected '" + missing + "'", iterator));
        }

        public static ClonException buildParenthesisClosedException(Iterator iterator) {
            return new ClonException(buildExceptionMessage("Unexpected '" + EXPRESSION_CLOSE + "'", iterator));
        }

        public static ClonException buildOverrideException(Iterator iterator, String key) {
            return new ClonException(buildExceptionMessage("Can not override field '" + key + "'", iterator));
        }

        public static ClonException buildEmptyException(Iterator iterator, String context) {
            return new ClonException(buildExceptionMessage(context + " can not be empty", iterator));
        }

        public static ClonException buildUnsupportedSymbolException(Iterator iterator) {
            return new ClonException(buildExceptionMessage("Unsupported symbol. Consider using quotes", iterator));
        }

        public static ClonException buildNameEmptyException(Iterator iterator) {
            return new ClonException(buildExceptionMessage("Key names and strings can not be empty", iterator));
        }

        private static String buildExceptionMessage(String message, Iterator iterator) {
            StringBuilder builder = new StringBuilder();
            builder.append(message).append("\n")
                    .append(iterator.getCompleteExpression()).append("\n");
            for (int i = 0; i < iterator.getIndex(); i++) builder.append(" ");
            return builder.append("^").toString();
        }

        private ClonException(String message) {
            super(message);
        }
    }
}
