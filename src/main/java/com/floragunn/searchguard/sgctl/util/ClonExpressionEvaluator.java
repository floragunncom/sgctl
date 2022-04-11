package com.floragunn.searchguard.sgctl.util;

import javax.sound.midi.SysexMessage;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ClonExpressionEvaluator {
    private static final List<Character> STRING_INDICATORS = Arrays.asList('"', '\'');
    private static final char ASSIGN_OPERATOR = '=';
    private static final Character EXPRESSION_OPEN = '[';
    private static final Character EXPRESSION_CLOSE = ']';
    private static final Character SEPARATOR = ',';

    public static Map<String, Object> evaluate(String ... expressions) throws ClonException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String expression : expressions) {
            Iterator iterator = new Iterator(expression);
            Map.Entry<String, Object> evaluatedExpression = new ExpressionReader(iterator).read();
            result.put(evaluatedExpression.getKey(), evaluatedExpression.getValue());
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

        public Map.Entry<String, Object> read() throws ClonException {
            System.out.println("expression: " + it.expression);
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            KeyReader.ValueApplier applier = new KeyReader(it).read(map);
            it.readCharacter(ASSIGN_OPERATOR);
            Object value = new ValueReader(it).read();
            it.validateEndAndParenthesis();
            applier.apply(value);
            return map.entrySet().iterator().next();
        }
    }

    private static class KeyReader {
        private final Iterator it;

        public KeyReader(Iterator it) {
            this.it = it;
        }

        private ValueApplier read(Map<String, Object> map) throws ClonException {
            System.out.println("key");
            String key = it.readString();
            if (it.current() == ASSIGN_OPERATOR || it.current() == EXPRESSION_CLOSE) {
                if (map.containsKey(key)) throw new RuntimeException(); //TODO cannot override
                //it.validateEndAndParenthesis();
                return new ValueApplier(key, map);
            }

            it.readCharacter(EXPRESSION_OPEN);
            if (it.current() == EXPRESSION_CLOSE) {
                ValueApplier applier;
                if (!map.containsKey(key)) {
                    List<Object> array = new ArrayList<>();
                    applier = new ValueApplier(key, array);
                    map.put(key, array);
                }
                else if (map.get(key) instanceof List)
                    applier = new ValueApplier(key, (List) map.get(key));
                else throw new RuntimeException(); //TODO cannot override
                it.readCharacter(EXPRESSION_CLOSE);
                //it.validateEndAndParenthesis();
                return applier;
            }
            ValueApplier applier;
            if (!map.containsKey(key)) {
                Map<String, Object> innerMap = new LinkedHashMap<>();
                applier = read(innerMap);
                map.put(key, innerMap);
            }
            else if(map.get(key) instanceof Map)
                applier = read((Map<String, Object>) map.get(key));
            else throw new RuntimeException(); //TODO cannot override
            it.readCharacter(EXPRESSION_CLOSE);
            //it.validateEndAndParenthesis();
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

            public void apply(Object object) {
                if (list != null) list.add(object);
                else if (map != null) map.put(key, object);
                else throw new RuntimeException();
            }
        }
    }

    private static class ValueReader {
        private final Iterator it;

        public ValueReader(Iterator it) {
            this.it = it;
        }

        public Object read() throws ClonException {
            Object result;
            String expression = it.getCurrentExpression();
            System.out.println("value: " + expression);

            if (expression.charAt(0) == EXPRESSION_OPEN) {
                if (expression.charAt(1) == EXPRESSION_CLOSE) throw new RuntimeException(); //Array or Object body can not be empty
                if (Iterator.peekContainsOnDepth(it, 1, ASSIGN_OPERATOR))
                    result = readObject();
                else
                    result = readArray();
            }
            else if (isLong()) result = readLong();
            else if (isDouble()) result = readDouble();
            else if ("true".equals(expression) || "false".equals(expression)) result = readBoolean();
            else if ("null".equals(expression)) result = readNull();
            else if (isString()) result = readString();
            else {
                System.out.println("x " + expression);
                throw new RuntimeException(); //TODO Could not evaluate type
            }
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

        private Object readLong() {
            return Long.valueOf(it.readNotEncapsulatedString());
        }

        private Object readDouble() {
            return Double.valueOf(it.readNotEncapsulatedString());
        }

        private Object readBoolean() {
            return "true".equals(it.readNotEncapsulatedString()) ? Boolean.TRUE : Boolean.FALSE;
        }

        private Object readNull() {
            return it.readNotEncapsulatedString();
        }

        private Object readString() {
            return it.readString();
        }

        private Object readArray() throws ClonException {
            it.readCharacter(EXPRESSION_OPEN);
            List<Object> values = new ArrayList<>();
            while (it.current() != EXPRESSION_CLOSE) {
                int valueStart = it.getIndex();
                System.out.println("start :" + valueStart);
                it.readUntilMatchOnSameDepth(SEPARATOR, EXPRESSION_CLOSE);
                int valueEnd = it.getIndex();
                System.out.println("end :" + valueEnd);
                ValueReader reader = new ValueReader(new Iterator(it, valueStart, valueEnd));
                values.add(reader.read());
                if (it.current() != EXPRESSION_CLOSE) it.readCharacter(SEPARATOR);
                System.out.println("done");
            }
            it.readCharacter(EXPRESSION_CLOSE);
            System.out.println(values);
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
                Map.Entry<String, Object> expressionResult = reader.read();
                objectMap.put(expressionResult.getKey(), expressionResult.getValue());
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

        public static boolean peekContainsOnDepth(Iterator iterator, int depth, Character ... characters) {
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

        public int peekNextOccurrenceIndex(char c) throws ClonException {
            int index = getCurrentExpression().indexOf(c);
            if (index < 0) throw ClonException.buildUnexpectedCharacterException(this, c);
            return index;
        }

        public String readUntilMatchOnSameDepth(Character ... chars) {
            StringBuilder builder = new StringBuilder();
            int depth = parenthesis.size();
            while (!Arrays.asList(chars).contains(it.current()) || parenthesis.size() != depth) {
                if (it.current() == CharacterIterator.DONE) throw new RuntimeException();
                builder.append(it.current());
                next();
            }
            return builder.toString();
        }

        public String readString() {
            if (STRING_INDICATORS.contains(it.current())) return readEncapsulatedString();
            StringBuilder name = new StringBuilder();
            while (!isOperationSymbol(it.current()) && !isEnd()) {
                if (!isNameSymbol(it.current())) throw new RuntimeException(); //TODO names must be letter or encapsulated
                name.append(it.current());
                next();
            }
            return name.toString();
        }

        private String readEncapsulatedString() {
            next();
            StringBuilder name = new StringBuilder();
            while (it.current() != parenthesis.peek()) {
                if (isEnd()) throw new RuntimeException();
                name.append(it.current());
                next();
            }
            next();
            return name.toString();
        }

        private String readNotEncapsulatedString() {
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
            if (!Arrays.asList(expectedCurrent).contains(it.current())) throw ClonException.buildUnexpectedCharacterException(this, it.current()); //TODO unexpected character
            Character current = it.current();
            next();
            return current;
        }

        public char next() {
            if (STRING_INDICATORS.contains(it.current())) {
                if (inString() && it.current() == parenthesis.peek()) parenthesis.pop();
                if (!inString()) parenthesis.push(it.current());
            }
            if (!inString()) {
                if (it.current() == EXPRESSION_OPEN) parenthesis.push(it.current());
                if (it.current() == EXPRESSION_CLOSE) {
                    if (!parenthesis.empty()) parenthesis.pop();
                    else throw new RuntimeException(); //TODO Close not expected
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

        public void validateEndAndParenthesis() {
            if (isEnd() && parenthesis.size() == 0) return;
            throw new RuntimeException(); //Unexpected parenthesis or char
        }
    }

    public static class ClonException extends Exception {
        public static ClonException buildUnexpectedCharacterException(Iterator iterator, char c) {
            StringBuilder builder = new StringBuilder();
            builder.append("Unexpected character '" + c + "'\n");
            builder.append(iterator.getCompleteExpression() + "\n");
            for (int i = 0; i < iterator.getIndex(); i++) builder.append(" ");
            builder.append("^");
            return new ClonException(builder.toString());
        }

        private ClonException(String message) {
            super(message);
        }
    }
}
