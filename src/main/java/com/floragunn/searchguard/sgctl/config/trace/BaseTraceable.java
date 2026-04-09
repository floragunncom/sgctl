package com.floragunn.searchguard.sgctl.config.trace;

public interface BaseTraceable<T> {

  Source getSource();

  boolean isSecret();

  T get();
}
