package com.floragunn.searchguard.sgctl.config.trace;

import java.util.Optional;

class OptTraceableImpl<T> implements OptTraceable<T> {

  private final Source source;
  private final Optional<T> value;

  public OptTraceableImpl(Source source, Optional<T> value) {
    this.source = source;
    this.value = value;
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
}
