package com.floragunn.searchguard.sgctl.testutil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TextAssertions {

  public static void assertEqualsNormalized(String expected, String actual) {
    assertEquals(normalize(expected), normalize(actual));
  }

  public static void assertEqualsNormalized(String expected, String actual, String message) {
    assertEquals(normalize(expected), normalize(actual), message);
  }

  private static String normalize(String s) {
    return s.replace("\r\n", "\n").replace("\r", "\n");
  }
}
