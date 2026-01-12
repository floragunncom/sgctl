package com.floragunn.searchguard.sgctl.config.trace;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

class OptTraceableImpl<T> implements OptTraceable<T> {

  private final Source source;
  private final Optional<T> value;

  public OptTraceableImpl(Source source, Optional<T> value) {
    this.source = source;
    this.value = value;
  }

  @Override
  public <U> OptTraceableImpl<U> map(Function<? super T, ? extends U> mapper) {
    return new OptTraceableImpl<>(source, value.map(mapper));
  }

  @Override
  public <U> OptTraceable<U> flatMap(
      Function<? super T, ? extends OptTraceable<? extends U>> mapper) {
    if (value.isEmpty()) {
      return new OptTraceableImpl<>(source, Optional.empty());
    } else {
      @SuppressWarnings("unchecked")
      var r = (OptTraceable<U>) mapper.apply(value.get());
      return Objects.requireNonNull(r);
    }
  }

  @Override
  public Optional<T> get() {
    return value;
  }

  @Override
  public Traceable<T> orElse(T other) {
    return new TraceableImpl<>(source, value.orElse(other));
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof OptTraceableImpl<?> other
        && source.equals(other.source)
        && Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, value);
  }

  @Override
  public String toString() {
    return value.map(String::valueOf).orElse("");
  }
}
