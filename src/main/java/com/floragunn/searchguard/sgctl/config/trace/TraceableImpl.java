package com.floragunn.searchguard.sgctl.config.trace;

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
}
