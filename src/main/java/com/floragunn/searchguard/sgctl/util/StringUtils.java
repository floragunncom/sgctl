package com.floragunn.searchguard.sgctl.util;

import java.util.stream.Collectors;

public class StringUtils {

  public static String indentLines(String s, int indentLevel) {
    var indent = "\t".repeat(indentLevel);
    return s.lines().map(line -> indent + line).collect(Collectors.joining("\n"));
  }
}
