package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ValidationErrors;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public interface OptTraceable<T> extends BaseTraceable<Optional<T>> {

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  default T getValue() {
    return get().get();
  }

  default boolean isPresent() {
    return get().isPresent();
  }

  <U> OptTraceable<U> map(Function<? super T, ? extends U> mapper);

  <U> OptTraceable<U> flatMap(Function<? super T, ? extends OptTraceable<? extends U>> mapper);

  Traceable<T> orElse(T other);

  static <T> OptTraceable<T> of(Source source, Optional<T> value, boolean isSecret) {
    return new OptTraceableImpl<>(source, value, isSecret);
  }

  static <T> OptTraceable<T> of(Source source, T value, boolean isSecret) {
    return of(source, Optional.of(value), isSecret);
  }

  static <T> OptTraceable<T> of(Traceable<T> traceable, boolean isSecret) {
    return of(traceable.getSource(), traceable.get(), isSecret);
  }

  static <T> OptTraceable<T> ofNullable(Source source, @Nullable T value, boolean isSecret) {
    return of(source, Optional.ofNullable(value), isSecret);
  }

  static <T> OptTraceable<T> empty(Source source, boolean isSecret) {
    return of(source, Optional.empty(), isSecret);
  }

  static <T> OptTraceable<T> of(Source source, Optional<T> value) {
    return of(source, value, false);
  }

  static <T> OptTraceable<T> of(Source source, T value) {
    return of(source, value, false);
  }

  static <T> OptTraceable<T> of(Traceable<T> traceable) {
    return of(traceable, false);
  }

  static <T> OptTraceable<T> ofNullable(Source source, @Nullable T value) {
    return ofNullable(source, value, false);
  }

  static <T> OptTraceable<T> empty(Source source) {
    return of(source, Optional.empty());
  }

  static <T> OptTraceable<T> validationErrors(ValidationErrors error) {
    return new ErroneousOptTraceable<>(error);
  }
}
