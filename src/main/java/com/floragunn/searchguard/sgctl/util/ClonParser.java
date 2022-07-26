/*
 * Copyright 2022 floragunn GmbH
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
    private static final Character PARENTHESIS_OPEN = '[';
    private static final Character PARENTHESIS_CLOSE = ']';
    private static final Character SEPARATOR = ',';

    protected enum PartType {KEY, VALUE, EXPRESSION}

    public static Map<String, Object> parse(List<String> expressions) throws ClonException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String expression : expressions) {
            TokenIterator iterator = new TokenIterator(expression, 0, expression.length());
            new ExpressionReader(iterator).read(result);
        }
        return result;
    }

    public static Map<String, Object> parse(String ... expressions) throws ClonException {
        return parse(Arrays.asList(expressions));
    }

    private static boolean isOperationSymbol(char c) {
        return c == SEPARATOR || c == ASSIGN_OPERATOR || c == PARENTHESIS_OPEN || c == PARENTHESIS_CLOSE;
    }

    private static boolean isNameSymbol(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
    }

    private static class ExpressionReader {
        private final TokenIterator it;

        private ExpressionReader(TokenIterator it) {
            this.it = it;
        }

        public Map<String, Object> read(Map<String, Object> map) throws ClonException {
            it.validateNotEmpty(PartType.EXPRESSION);
            int keyStart = it.getIndex();
            it.readUntilMatch(ASSIGN_OPERATOR);
            int keyEnd = it.getIndex();
            TokenIterator keyIterator = new TokenIterator(it, keyStart, keyEnd);
            KeyReader.ValueApplier applier = new KeyReader(keyIterator).read(map);
            it.readCharacter(ASSIGN_OPERATOR);
            int valueStart = it.getIndex();
            it.readUntilParenthesisOrIteratorEnd();
            int valueEnd = it.getIndex();
            TokenIterator valueIterator = new TokenIterator(it, valueStart, valueEnd);
            Object value = new ValueReader(valueIterator).read();
            it.validateEndAndParenthesis(PartType.EXPRESSION);
            applier.apply(value);
            return map;
        }
    }

    private static class KeyReader {
        private final TokenIterator it;

        public KeyReader(TokenIterator it) {
            this.it = it;
        }

        @SuppressWarnings("unchecked")
        private ValueApplier read(Map<String, Object> map) throws ClonException {
            it.validateNotEmpty(PartType.KEY);
            TokenIterator.peekParenthesisCorrect(it, PartType.KEY);
            String key = it.readString(PartType.KEY);
            ValueApplier applier;
            if (it.current() == CharacterIterator.DONE) {
                if (map.containsKey(key)) {
                    throw ClonException.Builder.getOverrideExceptionBuilder(key).build(it);
                }
                applier = new ValueApplier(key, map);
            } else {
                it.readCharacter(PARENTHESIS_OPEN);
                if (it.current() == PARENTHESIS_CLOSE) {
                    if (!map.containsKey(key)) {
                        List<Object> array = new ArrayList<>();
                        map.put(key, array);
                        applier = new ValueApplier(key, array);
                    } else if (map.get(key) instanceof List) {
                        applier = new ValueApplier(key, (List<Object>) map.get(key));
                    } else {
                        throw ClonException.Builder.getOverrideExceptionBuilder(key).build(it);
                    }
                } else {
                    int innerKeyStart = it.getIndex();
                    it.readUntilMatchOnSameDepth(PARENTHESIS_CLOSE);
                    int innerKeyEnd = it.getIndex();
                    TokenIterator innerKeyIterator = new TokenIterator(it, innerKeyStart, innerKeyEnd);

                    if (!map.containsKey(key)) {
                        Map<String, Object> innerMap = new LinkedHashMap<>();
                        map.put(key, innerMap);
                        applier = new KeyReader(innerKeyIterator).read(innerMap);
                    } else if(map.get(key) instanceof Map) {
                        applier = new KeyReader(innerKeyIterator).read((Map<String, Object>) map.get(key));
                    } else {
                        throw ClonException.Builder.getOverrideExceptionBuilder(key).build(it);
                    }
                }
                it.readCharacter(PARENTHESIS_CLOSE);
            }
            it.validateEndAndParenthesis(PartType.KEY);
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
                if (list != null) {
                    list.add(object);
                } else if (map != null) {
                    map.put(key, object);
                } else {
                    throw new ClonException("Could not resolve object tree");
                }
            }
        }
    }

    private static class ValueReader {
        private final TokenIterator it;

        public ValueReader(TokenIterator it) {
            this.it = it;
        }

        public Object read() throws ClonException {
            it.validateNotEmpty(PartType.VALUE);
            TokenIterator.peekParenthesisCorrect(it, PartType.VALUE);
            Object result;
            String expression = it.getCurrentExpression();
            if (expression.charAt(0) == PARENTHESIS_OPEN) {
                if (expression.charAt(1) == PARENTHESIS_CLOSE) {
                    throw ClonException.Builder.getEmptyExceptionBuilder(PartType.VALUE).build(it);
                }
                if (TokenIterator.peekContainsOnDepth(it, 1, ASSIGN_OPERATOR)) {
                    result = readObject();
                } else {
                    result = readArray();
                }
            } else if (isLong()) {
                result = readLong();
            } else if (isDouble()) {
                result = readDouble();
            } else if ("true".equals(expression) || "false".equals(expression)) {
                result = readBoolean();
            } else if ("null".equals(expression)) {
                result = readNull();
            } else {
                result = readString(PartType.VALUE);
            }
            it.validateEndAndParenthesis(PartType.VALUE);
            return result;
        }

        private boolean isLong() {
            try {
                Long.valueOf(it.getCurrentExpression());
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }

        private boolean isDouble() {
            try {
                Double.valueOf(it.getCurrentExpression());
                return true;
            } catch (NumberFormatException ignored) {
                return false;
            }
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

        private Object readString(PartType context) throws ClonException {
            return it.readString(context);
        }

        private Object readArray() throws ClonException {
            it.readCharacter(PARENTHESIS_OPEN);
            List<Object> values = new ArrayList<>();
            while (it.current() != PARENTHESIS_CLOSE) {
                int valueStart = it.getIndex();
                it.readUntilMatchOnSameDepth(SEPARATOR, PARENTHESIS_CLOSE);
                int valueEnd = it.getIndex();
                ValueReader reader = new ValueReader(new TokenIterator(it, valueStart, valueEnd));
                values.add(reader.read());
                if (it.current() != PARENTHESIS_CLOSE) {
                    it.readCharacter(SEPARATOR);
                    if (it.current() == PARENTHESIS_CLOSE) {
                        throw ClonException.Builder.getEmptyExceptionBuilder(PartType.VALUE).build(it);
                    }
                }
            }
            it.readCharacter(PARENTHESIS_CLOSE);
            return values;
        }

        private Object readObject() throws ClonException {
            it.readCharacter(PARENTHESIS_OPEN);
            Map<String, Object> objectMap = new LinkedHashMap<>();
            while (it.current() != PARENTHESIS_CLOSE) {
                int expressionStart = it.getIndex();
                it.readUntilMatchOnSameDepth(SEPARATOR, PARENTHESIS_CLOSE);
                int expressionEnd = it.getIndex();
                ExpressionReader reader = new ExpressionReader(new TokenIterator(it, expressionStart, expressionEnd));
                reader.read(objectMap);
                if (it.current() != PARENTHESIS_CLOSE) {
                    it.readCharacter(SEPARATOR);
                    if (it.current() == PARENTHESIS_CLOSE) {
                        throw ClonException.Builder.getEmptyExceptionBuilder(PartType.VALUE).build(it);
                    }
                }
            }
            it.readCharacter(PARENTHESIS_CLOSE);
            return objectMap;
        }
    }

    protected static class TokenIterator {
        private final String expression;
        private final StringCharacterIterator it;
        private final Stack<Character> parenthesis = new Stack<>();
        private final int start;
        private final int end;

        protected TokenIterator(String expression, int start, int end) {
            this.expression = expression;
            it = new StringCharacterIterator(expression, start, end, start);
            this.start = start;
            this.end = end;
        }

        protected TokenIterator(TokenIterator it, int start, int end) {
            this(it.expression, start, end);
        }

        protected TokenIterator(TokenIterator other) {
            this(other, other.start, other.end);
        }

        protected static boolean peekContainsOnDepth(TokenIterator iterator, int depth, Character ... characters) throws ClonException {
            TokenIterator it = new TokenIterator(iterator);
            while (!it.isEnd()) {
                if (Arrays.asList(characters).contains(it.current()) && it.parenthesis.size() == depth) {
                    return true;
                }
                it.next();
            }
            return false;
        }

        protected static void peekParenthesisCorrect(TokenIterator iterator, PartType context) throws ClonException {
            TokenIterator it = new TokenIterator(iterator);
            while (!it.isEnd()) {
                it.next();
            }
            it.validateParenthesis(context);
        }

        protected String getCurrentExpression() {
            return expression.substring(getIndex(), end);
        }

        protected String getCurrentExpressionPart() {
            return expression.substring(start, end);
        }

        protected String getCompleteExpression() {
            return expression;
        }

        protected String readUntilMatch(Character ... chars) throws ClonException {
            StringBuilder builder = new StringBuilder();
            while (!Arrays.asList(chars).contains(it.current())) {
                if (it.current() == CharacterIterator.DONE) {
                    throw ClonException.Builder.getCharacterNotFoundExceptionBuilder(chars).build(this);
                }
                builder.append(it.current());
                next();
            }
            return builder.toString();
        }

        protected String readUntilMatchOnSameDepth(Character ... chars) throws ClonException {
            StringBuilder builder = new StringBuilder();
            int depth = parenthesis.size();
            while (!Arrays.asList(chars).contains(it.current()) || parenthesis.size() != depth) {
                if (it.current() == CharacterIterator.DONE) {
                    throw ClonException.Builder.getCharacterNotFoundExceptionBuilder(chars).build(this);
                }
                builder.append(it.current());
                next();
            }
            return builder.toString();
        }

        protected String readUntilParenthesisOrIteratorEnd() throws ClonException {
            StringBuilder builder = new StringBuilder();
            int depth = parenthesis.size();
            boolean capsuled = STRING_INDICATORS.contains(it.current()) || it.current() == PARENTHESIS_OPEN;
            while (it.current() != CharacterIterator.DONE) {
                builder.append(it.current());
                next();
                if (capsuled && depth == parenthesis.size()) {
                    break;
                }
            }
            return builder.toString();
        }

        protected String readString(PartType context) throws ClonException {
            if (STRING_INDICATORS.contains(it.current())) {
                return readEncapsulatedString(context, it.current());
            }
            StringBuilder name = new StringBuilder();
            while (!isOperationSymbol(it.current()) && !isEnd()) {
                if (!isNameSymbol(it.current())) {
                    throw ClonException.Builder.getUnsupportedSymbolExceptionBuilder(it.current()).build(this);
                }
                name.append(it.current());
                next();
            }
            if (name.toString().isEmpty()) {
                throw ClonException.Builder.getNameEmptyExceptionBuilder(context).build(this);
            }
            return name.toString();
        }

        private String readEncapsulatedString(PartType context, Character parenthesis) throws ClonException {
            readCharacter(parenthesis);
            StringBuilder name = new StringBuilder();
            while (it.current() != parenthesis) {
                if (isEnd()) {
                    throw ClonException.Builder.getParenthesisOpenExceptionBuilder().build(this);
                }
                name.append(it.current());
                next();
            }
            readCharacter(parenthesis);
            if (name.toString().isEmpty() && context != PartType.VALUE) {
                throw ClonException.Builder.getNameEmptyExceptionBuilder(context).build(this);
            }
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

        protected boolean inString() {
            return !parenthesis.empty() && STRING_INDICATORS.contains(parenthesis.peek());
        }

        protected char current() {
            return it.current();
        }

        protected Character readCharacter(Character ... expectedCurrent) throws ClonException {
            if (!Arrays.asList(expectedCurrent).contains(it.current())) {
                throw ClonException.Builder.getUnexpectedCharacterExceptionBuilder(expectedCurrent).build(this);
            }
            Character current = it.current();
            next();
            return current;
        }

        protected char next() throws ClonException {
            if (STRING_INDICATORS.contains(it.current())) {
                if (inString() && it.current() == parenthesis.peek()) {
                    parenthesis.pop();
                } else if (!inString()) {
                    parenthesis.push(it.current());
                }
            }
            if (!inString()) {
                if (it.current() == PARENTHESIS_OPEN) {
                    parenthesis.push(it.current());
                } else if (it.current() == PARENTHESIS_CLOSE) {
                    if (!parenthesis.empty()) {
                        parenthesis.pop();
                    } else {
                        throw ClonException.Builder.getParenthesisCloseExceptionBuilder().build(this);
                    }
                }
            }
            return it.next();
        }

        protected int getIndex() {
            return it.getIndex();
        }

        protected boolean isEnd() {
            return it.current() == CharacterIterator.DONE;
        }

        protected void validateNotEmpty(PartType context) throws ClonException {
            if (Strings.isNullOrEmpty(getCurrentExpression())) {
                throw ClonException.Builder.getEmptyExceptionBuilder(context).build(this);
            }
        }

        protected void validateEnd() throws ClonException {
            if (!isEnd()) {
                throw ClonException.Builder.getNotEndExceptionBuilder().build(this);
            }
        }

        protected void validateParenthesis(PartType context) throws ClonException {
            if (!parenthesis.empty()) {
                throw (STRING_INDICATORS.contains(parenthesis.peek()) ? ClonException.Builder.getNoStringEndExceptionBuilder() : ClonException.Builder.getParenthesisOpenExceptionBuilder()).build(this);
            }
        }

        protected void validateEndAndParenthesis(PartType context) throws ClonException {
            validateEnd();
            validateParenthesis(context);
        }
    }

    public static class ClonException extends Exception {
        private static final long serialVersionUID = -821669828214088092L;

        private ClonException(String message) {
            super(message);
        }

        protected static class Builder {
            String message;
            String expression;
            String part;
            int errorIndex;
            int partStartIndex;

            protected ClonException build(TokenIterator iterator) {
                expression = iterator.getCompleteExpression();
                part = iterator.getCurrentExpressionPart();
                errorIndex = iterator.getIndex();
                partStartIndex = iterator.start;
                return build();
            }

            protected ClonException build() {
                StringBuilder builder = new StringBuilder();
                builder.append(message).append("\n");
                builder.append(expression).append("\n");
                char[] spaces = new char[partStartIndex];
                Arrays.fill(spaces, ' ');
                builder.append(spaces).append(part).append("\n");
                spaces = new char[errorIndex];
                Arrays.fill(spaces, ' ');
                builder.append(spaces).append('^');
                return new ClonException(builder.toString());
            }

            protected Builder setMessage(String message) {
                this.message = message;
                return this;
            }

            protected Builder setExpression(String expression) {
                this.expression = expression;
                return this;
            }

            protected Builder setPart(String part) {
                this.part = part;
                return this;
            }

            protected Builder setErrorIndex(int errorIndex) {
                this.errorIndex = errorIndex;
                return this;
            }

            protected Builder setPartStartIndex(int partStartIndex) {
                this.partStartIndex = partStartIndex;
                return this;
            }

            public static Builder getUnexpectedCharacterExceptionBuilder(Character ... expected) {
                return new Builder().setMessage("Expected '" + Arrays.toString(expected) + "'");
            }

            public static Builder getNotEndExceptionBuilder() {
                return new Builder().setMessage("Expected expression to end");
            }

            public static Builder getCharacterNotFoundExceptionBuilder(Character ... expected) {
                return new Builder().setMessage("Expected " + Arrays.toString(expected));
            }

            public static Builder getParenthesisOpenExceptionBuilder() {
                return new Builder().setMessage("Expected '" + PARENTHESIS_CLOSE + "'");
            }

            public static Builder getParenthesisCloseExceptionBuilder() {
                return new Builder().setMessage("Unexpected '" + PARENTHESIS_CLOSE + "'");
            }

            public static Builder getNoStringEndExceptionBuilder() {
                return new Builder().setMessage("Expected string or key name to end");
            }

            public static Builder getEmptyExceptionBuilder(PartType context) {
                return new Builder().setMessage(context != PartType.VALUE ? context + " can not be empty" : "Empty string value must be surrounded by ' or \"");
            }

            public static Builder getUnsupportedSymbolExceptionBuilder(Character c) {
                return new Builder().setMessage("Unsupported symbol '" + c + "'. Consider using quotes");
            }

            public static Builder getNameEmptyExceptionBuilder(PartType context) {
                return new Builder().setMessage(context == PartType.KEY ? "Key names can not be empty" : "String values can not be empty");
            }

            public static Builder getOverrideExceptionBuilder(String key) {
                return new Builder().setMessage("Can not override field '" + key + "'");
            }
        }
    }
}
