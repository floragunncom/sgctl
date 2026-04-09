package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.List;
import java.util.function.Function;

public interface Traceable<T> extends BaseTraceable<T> {

  <R> Traceable<R> map(Function<? super T, ? extends R> mapper);

  <R> Traceable<R> flatMap(Function<? super T, ? extends Traceable<R>> mapper);

  static <T> Traceable<T> of(Source source, T value, boolean isSecret) {
    return new TraceableImpl<>(source, value, isSecret);
  }

  static <T> ImmutableList<Traceable<T>> ofList(Source parent, boolean isSecret, T... values) {
    return ofList(parent, List.of(values), isSecret);
  }

  static <T> ImmutableList<Traceable<T>> ofList(
      Source parent, Iterable<? extends T> values, boolean isSecret) {
    var builder = new ImmutableList.Builder<Traceable<T>>();
    int i = 0;
    for (var value : values) {
      builder.add(Traceable.of(new Source.ListEntry(parent, i++), value, isSecret));
    }
    return builder.build();
  }

  static <T> ImmutableMap<String, Traceable<T>> ofMap(
      Source parent, ImmutableMap<String, T> map, boolean isSecret) {
    var builder = new ImmutableMap.Builder<String, Traceable<T>>();
    for (var entry : map.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();
      builder.put(key, Traceable.of(new Source.Attribute(parent, key), value, isSecret));
    }
    return builder.build();
  }

  static <T> Traceable<T> of(Source source, T value) {
    return of(source, value, false);
  }

  static <T> ImmutableList<Traceable<T>> ofList(Source parent, T... values) {
    return ofList(parent, false, values);
  }

  static <T> ImmutableList<Traceable<T>> ofList(Source parent, Iterable<? extends T> values) {
    return ofList(parent, values, false);
  }

  static <T> ImmutableMap<String, Traceable<T>> ofMap(Source parent, ImmutableMap<String, T> map) {
    return ofMap(parent, map, false);
  }

  static <T> Traceable<T> validationErrors(ValidationErrors error) {
    return new ErroneousTraceable<>(error);
  }
}
