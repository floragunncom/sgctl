package com.floragunn.searchguard.sgctl.util.mapping.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts a subset of Lucene regex syntax into Java regex patterns.
 */
public class LuceneRegexParser {
    private static final Pattern regexEnabledPattern = Pattern.compile("^/.+/$");
    private static final Pattern rangePattern = Pattern.compile("[^\\\\]<\\d+?-\\d+?>");
    private static final Pattern emptyPattern = Pattern.compile("[^\\\\]#");
    private static final Pattern andPattern = Pattern.compile("[^\\\\]&");
    private static final Pattern doubleAndPattern = Pattern.compile("[^\\\\]&.*?[^\\\\]&");
    private static final Pattern complementPattern = Pattern.compile("[^\\\\]~");
    private static final Pattern anyStringPattern = Pattern.compile("[^\\\\]@");

    public static String toJavaRegex(String luceneReg) throws Exception {
        if (!regexEnabledPattern.matcher(luceneReg).find()) return luceneReg;
        if (complementPattern.matcher(luceneReg).find()) throw new Exception("Encountered a complement operator '~'. This can not be perfectly represented in Java regex.");
        luceneReg = replaceRanges(luceneReg);
        luceneReg = replaceEmpty(luceneReg);
        luceneReg = replaceAny(luceneReg);
        luceneReg = replaceAnd(luceneReg);
        return luceneReg;
    }

    private static String replaceAny(String luceneReg) {
        var matcher = anyStringPattern.matcher(luceneReg);
        while (matcher.find()) {
            var matchStart = matcher.start()+1;
            luceneReg = luceneReg.substring(0, matchStart) + ".*" + luceneReg.substring(matchStart+1);
            matcher = anyStringPattern.matcher(luceneReg);
        }
        return luceneReg;
    }

    private static String replaceEmpty(String luceneReg) {
        var matcher = emptyPattern.matcher(luceneReg);
        while (matcher.find()) {
            var matchStart = matcher.start()+1;
            luceneReg = luceneReg.substring(0, matchStart) + "(?!)" + luceneReg.substring(matchStart+1);
            matcher = emptyPattern.matcher(luceneReg);
        }
        return luceneReg;
    }

    private static String replaceAnd(String luceneReg) {
        var matcher = doubleAndPattern.matcher(luceneReg);
        var firstMatchIndex = 0;
        while (matcher.find()) {
            final var matchStart = matcher.start();
            if (firstMatchIndex == 0) firstMatchIndex = matchStart+1;
            final var matchEnd = matcher.end();
            final var match = matcher.group();
            luceneReg = luceneReg.substring(0, matchStart+1) + "(?=" + match.substring(2, match.length()-1) + ")&" + luceneReg.substring(matchEnd);
            matcher = doubleAndPattern.matcher(luceneReg);
        }
        if (firstMatchIndex != 0) {
            luceneReg = "/(?=" + luceneReg.substring(1, firstMatchIndex) + ')' + luceneReg.substring(firstMatchIndex);
            matcher = andPattern.matcher(luceneReg);
        }
        if (matcher.find()) {
            final var lastAnd = matcher.start();
            luceneReg = luceneReg.substring(0, lastAnd+1) + luceneReg.substring(lastAnd+2);
        }
        return luceneReg;
    }

    private static String replaceRanges(String luceneReg) {
        var matcher = rangePattern.matcher(luceneReg);
        var buffer = new StringBuilder();
        while (matcher.find()) {
            final var match = matcher.group();
            final var split = match.split("-");
            var min = Integer.parseInt(split[0].substring(2));
            var max = Integer.parseInt(split[1].substring(0, split[1].length()-1));
            if (min > max) {
                final var tmp = min;
                min = max;
                max = tmp;
            }
            matcher.appendReplacement(buffer, rangeToRegex(min, max));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    //region Lucene Range to Regex
    private static String rangeToRegex(int min, int max) {
        assert(min <= max);
        final var maxLength = String.valueOf(max).length();
        final var pairs = splitIntoDigitAlignedRanges(new Range(min, max));
        final var regex = new StringBuilder("(");
        for (int i = 0; i < pairs.size(); i++) {
            final var range = pairs.get(i);
            final var minStr = String.valueOf(range.min);
            final var maxStr = String.valueOf(range.max);
            if (maxStr.length() < maxLength) {
                regex.append("0{0,").append(maxLength-maxStr.length()).append('}').append(digitAlignedRangeToRegex(minStr, maxStr));
            } else {
                regex.append(digitAlignedRangeToRegex(minStr, maxStr));
            }
            if (i != pairs.size()-1) {
                regex.append('|');
            }
        }
        regex.append(')');
        return regex.toString();
    }

    private static List<Range> splitIntoDigitAlignedRanges(Range range) {
        var leadingSubranges = new ArrayList<Range>();
        var middleStartPoint = addLeadingSubranges(leadingSubranges, range);
        var trailingSubranges = new ArrayList<Range>();
        int middleEndPoint = addTrailingSubranges(trailingSubranges, new Range(middleStartPoint, range.max));

        var digitAlignedRanges = new ArrayList<>(leadingSubranges);
        if (middleEndPoint > middleStartPoint) {
            digitAlignedRanges.add(new Range(middleStartPoint, middleEndPoint));
        }
        digitAlignedRanges.addAll(trailingSubranges);
        return digitAlignedRanges;
    }

     private static String digitAlignedRangeToRegex(String min, String max) {
        assert min.length() == max.length();
        var result = new StringBuilder();
        for (int pos = 0; pos < min.length(); pos++) {
            final var startDigit = min.charAt(pos);
            final var endDigit = max.charAt(pos);
            if (startDigit == endDigit) {
                result.append(startDigit);
            } else {
                result.append('[').append(startDigit).append('-').append(endDigit).append(']');
            }
        }
        return result.toString();
    }

     private static int addTrailingSubranges(List<Range> trailingRanges, Range range) {
        var high = range.max;
        var low = expandToNextLowerBoundary(high);
        while (low >= range.min) {
            trailingRanges.add(new Range(low, high));
            high = low - 1;
            low = expandToNextLowerBoundary(high);
        }
        Collections.reverse(trailingRanges);
        return high;
    }

    private static int addLeadingSubranges(ArrayList<Range> leadingRanges, Range range) {
        var low = range.min;
        var high = expandToNextUpperBoundary(low);
        while (high < range.max) {
            leadingRanges.add(new Range(low, high));
            low = high + 1;
            high = expandToNextUpperBoundary(low);
        }
        return low;
    }

    private static int expandToNextUpperBoundary(int num) {
        var chars = String.valueOf(num).toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == '0') {
                chars[i] = '9';
            } else {
                chars[i] = '9';
                break;
            }
        }
        return Integer.parseInt(String.valueOf(chars));
    }

     private static int expandToNextLowerBoundary(int num) {
        var chars = String.valueOf(num).toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == '9') {
                chars[i] = '0';
            } else {
                chars[i] = '0';
                break;
            }
        }
        return Integer.parseInt(String.valueOf(chars));
    }

    private record Range(int min, int max) { }
    //endregion
}
