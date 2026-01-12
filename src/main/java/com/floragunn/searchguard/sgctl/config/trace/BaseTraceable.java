package com.floragunn.searchguard.sgctl.config.trace;

public interface BaseTraceable<T> {

  Source getSource();

  T get();
}
