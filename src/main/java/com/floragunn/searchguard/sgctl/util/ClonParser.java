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
    private static final Character EXPRESSION_OPEN = '[';
    private static final Character EXPRESSION_CLOSE = ']';
    private static final Character SEPARATOR = ',';

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
        return c == SEPARATOR || c == ASSIGN_OPERATOR || c == EXPRESSION_OPEN || c == EXPRESSION_CLOSE;
    }

    private static boolean isNameSymbol(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private static class ExpressionReader {
        private final TokenIterator it;

        private ExpressionReader(TokenIterator it) {
            this.it = it;
        }

        public Map<String, Object> read(Map<String, Object> map) throws ClonException {
            it.validateNotEmpty("Expression");
            int keyStart = it.getIndex();
            it.readUntilMatch(ASSIGN_OPERATOR);
            int keyEnd = it.getIndex();
            TokenIterator keyIterator = new TokenIterator(it, keyStart, keyEnd);
            KeyReader.ValueApplier applier = new KeyReader(keyIterator).read(map);
            it.readCharacter(ASSIGN_OPERATOR);
            Object value = new ValueReader(it).read();
            it.validateEndAndParenthesis();
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
            it.validateNotEmpty("Key");
            it.peekParenthesisCorrect();
            String key = it.readString();
            if (it.current() == CharacterIterator.DONE || it.current() == EXPRESSION_CLOSE) {
                if (map.containsKey(key)) {
                    throw ClonException.Builder.getOverrideExceptionBuilder(key).build(it);
                }
                return new ValueApplier(key, map);
            }
            it.readCharacter(EXPRESSION_OPEN);
            if (it.current() == EXPRESSION_CLOSE) {
                ValueApplier applier;
                if (!map.containsKey(key)) {
                    List<Object> array = new ArrayList<>();
                    map.put(key, array);
                    applier = new ValueApplier(key, array);
                } else if (map.get(key) instanceof List) {
                    applier = new ValueApplier(key, (List<Object>) map.get(key));
                } else {
                    throw ClonException.Builder.getOverrideExceptionBuilder(key).build(it);
                }
                it.readCharacter(EXPRESSION_CLOSE);
                return applier;
            }
            ValueApplier applier;
            if (!map.containsKey(key)) {
                Map<String, Object> innerMap = new LinkedHashMap<>();
                map.put(key, innerMap);
                applier = read(innerMap);
            } else if(map.get(key) instanceof Map) {
                applier = read((Map<String, Object>) map.get(key));
            } else {
                throw ClonException.Builder.getOverrideExceptionBuilder(key).build(it);
            }
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
            it.validateNotEmpty("Value");
            it.peekParenthesisCorrect();
            Object result;
            String expression = it.getCurrentExpression();
            if (expression.charAt(0) == EXPRESSION_OPEN) {
                if (expression.charAt(1) == EXPRESSION_CLOSE) {
                    throw ClonException.Builder.getEmptyExceptionBuilder("Array or object").build(it);
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
                result = readString();
            }
            it.validateEndAndParenthesis();
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
                ValueReader reader = new ValueReader(new TokenIterator(it, valueStart, valueEnd));
                values.add(reader.read());
                if (it.current() != EXPRESSION_CLOSE) {
                    it.readCharacter(SEPARATOR);
                    if (it.current() == EXPRESSION_CLOSE) {
                        throw ClonException.Builder.getEmptyExceptionBuilder("Value").build(it);
                    }
                }
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
                ExpressionReader reader = new ExpressionReader(new TokenIterator(it, expressionStart, expressionEnd));
                reader.read(objectMap);
                if (it.current() != EXPRESSION_CLOSE) {
                    it.readCharacter(SEPARATOR);
                    if (it.current() == EXPRESSION_CLOSE) {
                        throw ClonException.Builder.getEmptyExceptionBuilder("Value").build(it);
                    }
                }
            }
            it.readCharacter(EXPRESSION_CLOSE);
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

        protected String getCurrentExpression() {
            return expression.substring(getIndex(), end);
        }

        protected String getCompleteExpression() {
            return expression;
        }

        protected void peekParenthesisCorrect() throws ClonException {
            TokenIterator it = new TokenIterator(this);
            while (!it.isEnd()) {
                it.next();
            }
            it.validateParenthesis();
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

        protected String readString() throws ClonException {
            if (STRING_INDICATORS.contains(it.current())) {
                return readEncapsulatedString(it.current());
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
                throw ClonException.Builder.getNameEmptyExceptionBuilder().build(this);
            }
            return name.toString();
        }

        private String readEncapsulatedString(Character parenthesis) throws ClonException {
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
            if (name.toString().isEmpty()) {
                throw ClonException.Builder.getNameEmptyExceptionBuilder().build(this);
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
                } else {
                    parenthesis.push(it.current());
                }
            }
            if (!inString()) {
                if (it.current() == EXPRESSION_OPEN) {
                    parenthesis.push(it.current());
                } else if (it.current() == EXPRESSION_CLOSE) {
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

        protected void validateNotEmpty(String context) throws ClonException {
            if (Strings.isNullOrEmpty(getCurrentExpression())) {
                throw ClonException.Builder.getEmptyExceptionBuilder(context).build(this);
            }
        }

        protected void validateEnd() throws ClonException {
            if (!isEnd()) {
                throw ClonException.Builder.getNotEndExceptionBuilder().build(this);
            }
        }

        protected void validateParenthesis() throws ClonException {
            if (!parenthesis.empty()) {
                throw (STRING_INDICATORS.contains(parenthesis.peek()) ? ClonException.Builder.getNoStringEndExceptionBuilder() : ClonException.Builder.getParenthesisOpenExceptionBuilder()).build(this);
            }
        }

        protected void validateEndAndParenthesis() throws ClonException {
            validateEnd();
            validateParenthesis();
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
            int index;

            protected ClonException build(TokenIterator iterator) {
                expression = iterator.getCompleteExpression();
                index = iterator.getIndex();
                return build();
            }

            protected ClonException build() {
                StringBuilder builder = new StringBuilder();
                builder.append(message).append("\n");
                builder.append(expression).append("\n");
                char[] spaces = new char[index];
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

            protected Builder setIndex(int index) {
                this.index = index;
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
                return new Builder().setMessage("Expected '" + EXPRESSION_CLOSE + "'");
            }

            public static Builder getParenthesisCloseExceptionBuilder() {
                return new Builder().setMessage("Unexpected '" + EXPRESSION_CLOSE + "'");
            }

            public static Builder getNoStringEndExceptionBuilder() {
                return new Builder().setMessage("Expected string or key name to end");
            }

            public static Builder getEmptyExceptionBuilder(String context) {
                return new Builder().setMessage(context + " can not be empty");
            }

            public static Builder getUnsupportedSymbolExceptionBuilder(Character c) {
                return new Builder().setMessage("Unsupported symbol '" + c + "'. Consider using quotes");
            }

            public static Builder getNameEmptyExceptionBuilder() {
                return new Builder().setMessage("Key names and strings values can not be empty");
            }

            public static Builder getOverrideExceptionBuilder(String key) {
                return new Builder().setMessage("Can not override field '" + key + "'");
            }
        }
    }
}
