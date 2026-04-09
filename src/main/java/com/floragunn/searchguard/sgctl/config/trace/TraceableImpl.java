package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.searchguard.sgctl.util.StringUtils;
import java.util.Objects;
import java.util.function.Function;

class TraceableImpl<T> implements Traceable<T> {

  private final Source source;
  private final T value;
  private final boolean isSecret;

  public TraceableImpl(Source source, T value, boolean isSecret) {
    this.source = source;
    this.value = value;
    this.isSecret = isSecret;
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  public <R> Traceable<R> map(Function<? super T, ? extends R> mapper) {
    return new TraceableImpl<>(source, mapper.apply(value), isSecret);
  }

  @Override
  public <R> Traceable<R> flatMap(Function<? super T, ? extends Traceable<R>> mapper) {
    return mapper.apply(value);
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public boolean isSecret() {
    return isSecret;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TraceableImpl<?> other
        && source.equals(other.source)
        && isSecret == other.isSecret
        && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, value, isSecret);
  }

  @Override
  public String toString() {
    return StringUtils.shorten(isSecret ? "***" : String.valueOf(value), 100);
  }
}
