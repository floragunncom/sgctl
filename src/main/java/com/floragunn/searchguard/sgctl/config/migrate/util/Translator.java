package com.floragunn.searchguard.sgctl.config.migrate.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Translator {

  private static final String SEPARATOR = "\t";

  private static Translator instance;

  private final Map<String, Map<String, String>> translationMappings;

  private String language = "en"; // TODO: Change dynamically

  private Translator() {
    var rows =
        new BufferedReader(
                new InputStreamReader(
                    Objects.requireNonNull(
                        Translator.class.getResourceAsStream("translations.csv")),
                    StandardCharsets.UTF_8))
            .lines();

    var rowsIter = rows.iterator();
    var header = Arrays.stream(rowsIter.next().split(SEPARATOR)).toList();

    translationMappings = new HashMap<>();
    rowsIter.forEachRemaining(
        row -> {
          final var values = Arrays.stream(row.split(SEPARATOR)).toList();
          assert values.size() == header.size();
          final String key = values.get(0);
          final Map<String, String> languages = new HashMap<>(values.size() - 1);
          for (int i = 1; i < header.size(); i++) {
            languages.put(header.get(i), values.get(i));
          }
          translationMappings.put(key, languages);
        });
    rows.close();
  }

  public static Translator getInstance() {
    if (instance == null) {
      instance = new Translator();
    }
    return instance;
  }

  public String translateKey(String key, String language) {
    return Optional.ofNullable(translationMappings.get(key))
        .flatMap(languages -> Optional.of(languages.get(language)))
        .orElse(key);
  }

  public String translate(List<String> template) {
    final StringBuilder inconvertibleErrorMessageBuilder = new StringBuilder();
    for (var msgPart : template) {
      if (msgPart.startsWith("{") && msgPart.endsWith("}")) {
        final String translationKey = msgPart.toUpperCase().substring(1, msgPart.length() - 1);
        final String replacement = translateKey(translationKey, language);
        if (replacement == null) {
          throw new IllegalStateException(
              "Error Message template contains unknown translation key: " + translationKey);
        }
        inconvertibleErrorMessageBuilder.append(replacement);
      } else {
        inconvertibleErrorMessageBuilder.append(msgPart);
      }
    }
    return inconvertibleErrorMessageBuilder.toString();
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}
