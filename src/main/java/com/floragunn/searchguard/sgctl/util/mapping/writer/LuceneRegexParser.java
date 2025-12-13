package com.floragunn.searchguard.sgctl.util.mapping.writer;

import java.util.regex.Pattern;

public class LuceneRegexParser {
    private static final Pattern rangePattern = Pattern.compile("[^\\\\]<\\d+?-\\d+?>");
    private static final Pattern emptyPattern = Pattern.compile("[^\\\\]#");
    private static final Pattern andPattern = Pattern.compile("[^\\\\]&");
    private static final Pattern doubleAndPattern = Pattern.compile("[^\\\\]&.*?[^\\\\]&");
    private static final Pattern complementPattern = Pattern.compile("[^\\\\]~");

    static public String toJavaRegex(String luceneReg) throws Exception {
        if (!luceneReg.matches("^/.*/$")) return luceneReg;
        if (complementPattern.matcher(luceneReg).find()) throw new Exception("Encountered a complement operator '~'. This can not be perfectly represented in Java regex.");
        var matcher = rangePattern.matcher(luceneReg);
        while (matcher.find()) {
            var match = matcher.group();
            luceneReg = luceneRangeToJavaRegex(match.substring(1));
            matcher = rangePattern.matcher(luceneReg);
        }
        matcher = emptyPattern.matcher(luceneReg);
        while (matcher.find()) {
            var matchStart = matcher.start()+1;
            luceneReg = luceneReg.substring(0, matchStart) + "(?!)" + luceneReg.substring(matchStart+1);
            matcher = emptyPattern.matcher(luceneReg);
        }
        luceneReg = luceneAndToJavaRegex(luceneReg);
        print(luceneReg);
        return luceneReg;
    }

    static private String luceneAndToJavaRegex(String luceneReg) {
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
        luceneReg = "/(?=" + luceneReg.substring(1, firstMatchIndex) + ")" + luceneReg.substring(firstMatchIndex);
        matcher = andPattern.matcher(luceneReg);
        if (matcher.find()) {
            final var lastAnd = matcher.start();
            luceneReg = luceneReg.substring(0, lastAnd+1) + luceneReg.substring(lastAnd+2);
        }
        return luceneReg;
    }

    static private String luceneRangeToJavaRegex(String luceneReg) throws Exception {
        var separatorIndex = luceneReg.indexOf('-');
        var startStr = luceneReg.substring(1, separatorIndex);
        var endStr = luceneReg.substring(separatorIndex + 1, luceneReg.length() - 1);
        var start = Integer.parseInt(startStr);
        var end = Integer.parseInt(endStr);
        if (start > end) throw new Exception("The minimum of a range property can not be larger then the maximum. (" + start + "≰" + end + ")");
        var regex = new StringBuilder("(");
        for (var i = 0; i < endStr.length(); i++) {
            if (i == separatorIndex) {

            } else if (i == endStr.length()-1) {
                if (endStr.charAt(0) != '1') {
                    regex.append("[1-").append(endStr.charAt(0)).append("]");
                } else {
                    regex.append(1);
                }
                for (int j = 1; j < i; j++) {
                    if (endStr.charAt(j) == 0)
                        regex.append("[0-" + endStr.charAt(j) + "]");
                }
            } else {
                regex.append("|");
            }
        }
        regex.append(")");
        print(regex);
        return regex.toString();
    }

    static void print(Object line) {
        System.out.println(line);
    }
}
