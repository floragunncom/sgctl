package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import java.util.List;

public interface Traceable<T> {

  Source getSource();

  T get();

  static <T> Traceable<T> of(Source source, T value) {
    return new TraceableImpl<>(source, value);
  }

  static <T> ImmutableList<Traceable<T>> ofList(Source parent, T... values) {
    return ofList(parent, List.of(values));
  }

  static <T> ImmutableList<Traceable<T>> ofList(Source parent, Iterable<? extends T> values) {
    var builder = new ImmutableList.Builder<Traceable<T>>();
    int i = 0;
    for (var value : values) {
      builder.add(Traceable.of(new Source.ListEntry(parent, i++), value));
    }
    return builder.build();
  }

  static <T> Traceable<T> validationErrors(ValidationErrors error) {
    return new ErroneousTraceable<>(error);
  }
}
