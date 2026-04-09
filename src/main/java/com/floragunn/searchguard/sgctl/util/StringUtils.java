package com.floragunn.searchguard.sgctl.util;

import java.util.stream.Collectors;

public class StringUtils {

  public static String indentLines(String s, int indentLevel) {
    var indent = "\t".repeat(indentLevel);
    return s.lines().map(line -> indent + line).collect(Collectors.joining("\n"));
  }

  public static String shorten(String s, int maxLength) {
    if (s.length() <= maxLength) return s;
    return s.substring(0, maxLength - 3) + "...";
  }
}
