package com.floragunn.searchguard.sgctl.config.trace;

import java.util.Objects;

class TraceableImpl<T> implements Traceable<T> {

  private final Source source;
  private final T value;

  public TraceableImpl(Source source, T value) {
    this.source = source;
    this.value = value;
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TraceableImpl<?> other
        && source.equals(other.source)
        && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
