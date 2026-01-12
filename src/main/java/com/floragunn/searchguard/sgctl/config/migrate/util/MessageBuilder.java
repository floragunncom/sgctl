package com.floragunn.searchguard.sgctl.config.migrate.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/** Utility class for incrementally building formatted text messages. */
public class MessageBuilder {

  private Optional<StringBuilder> stringBuilder;

  private final String SENTENCE_END = ".";
  private final String SEPARATOR = ", ";
  private final String KEY_VALUE_SEPARATOR = ": ";

  private boolean isNewSentence = false;

  /** Creates a new, valid MessageBuilder. */
  public MessageBuilder() {
    stringBuilder = Optional.of(new StringBuilder());
  }

  private StringBuilder ensureValid() {
    if (stringBuilder.isPresent()) {
      return stringBuilder.get();
    }
    throw new IllegalStateException("Message builder is not valid");
  }

  /**
   * Appends an item to the message.
   *
   * @param item the item to append to the message
   */
  public <T> void append(T item) {
    StringBuilder builder = ensureValid();
    if (isNewSentence) {
      builder.append(" ");
      isNewSentence = false;
    }

    builder.append(item);
  }

  /**
   * Appends items separated by the separator.
   *
   * @param items the items to append
   */
  public <T> void appendSeparated(List<T> items) {
    for (int i = 0; i < items.size(); i++) {
      append(items.get(i).toString());
      if (i != items.size() - 1) {
        append(SEPARATOR);
      }
    }
  }

  /**
   * Appends mapped items, separated by a predefined separator.
   *
   * @param items the items to append
   * @param mapper function converting items to strings
   */
  public <T> void appendSeparated(Iterable<T> items, Function<T, String> mapper) {
    final var stream = StreamSupport.stream(items.spliterator(), false);
    final var mapped = stream.map(mapper).toList();
    appendSeparated(mapped);
  }

  /**
   * Appends a key value pair.
   *
   * @param key the key
   * @param value the value
   */
  public <T> void appendKeyValue(String key, T value) {
    append(key);
    append(KEY_VALUE_SEPARATOR);
    append(value.toString());
  }

  /**
   * Appends a key value pair using a singular or plural key depending on item count.
   *
   * @param keyOne key used for a single item
   * @param keyMultiple key used for multiple items
   * @param items the items to append
   */
  public <T> void appendKeyValueSeparated(String keyOne, String keyMultiple, List<T> items) {
    if (items.isEmpty()) {
      return;
    }
    if (items.size() == 1) {
      append(keyOne);
      append(KEY_VALUE_SEPARATOR);
      append(items.iterator().next().toString());
    } else {
      append(keyMultiple);
      append(KEY_VALUE_SEPARATOR);
      appendSeparated(items);
    }
  }

  /** Appends a line break. */
  public void nextLine() {
    isNewSentence = false;
    append("\n");
  }

  /** Ends the current sentence. */
  public void nextSentence() {
    if (isNewSentence) return;
    StringBuilder builder = ensureValid();
    builder.append(SENTENCE_END);
    isNewSentence = true;
  }

  /**
   * Appends a key value pair from mapped iterable items.
   *
   * @param keyOne key used for a single item
   * @param keyMultiple key used for multiple items
   * @param items the items to append
   * @param mapper function converting items to strings
   */
  public <T> void appendKeyValueSeparated(
      String keyOne, String keyMultiple, Iterable<T> items, Function<T, String> mapper) {
    final var stream = StreamSupport.stream(items.spliterator(), false);
    final var mapped = stream.map(mapper).toList();
    appendKeyValueSeparated(keyOne, keyMultiple, mapped);
  }

  /**
   * Appends a key value pair from mapped array items.
   *
   * @param keyOne key used for a single item
   * @param keyMultiple key used for multiple items
   * @param items the items to append
   * @param mapper function converting items to strings
   */
  public <T> void appendKeyValueSeparated(
      String keyOne, String keyMultiple, T[] items, Function<T, String> mapper) {
    final var stream = Arrays.stream(items);
    final var mapped = stream.map(mapper).toList();
    appendKeyValueSeparated(keyOne, keyMultiple, mapped);
  }

  /**
   * Finalizes and returns the built message. After calling this method, the builder becomes
   * invalid.
   *
   * @return the final message string
   */
  public String finalizeMessage() {
    ensureValid();
    StringBuilder builder = ensureValid();
    String message = builder.toString();
    stringBuilder = Optional.empty(); // Make sure it can't accidentally be finalized again
    return message;
  }
}
