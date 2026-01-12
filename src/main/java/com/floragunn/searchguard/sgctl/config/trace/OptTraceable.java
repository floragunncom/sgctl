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

  <U> OptTraceable<U> map(Function<? super T, ? extends U> mapper);

  <U> OptTraceable<U> flatMap(Function<? super T, ? extends OptTraceable<? extends U>> mapper);

  Traceable<T> orElse(T other);

  static <T> OptTraceable<T> of(Source source, Optional<T> value) {
    return new OptTraceableImpl<>(source, value);
  }

  static <T> OptTraceable<T> of(Source source, T value) {
    return of(source, Optional.of(value));
  }

  static <T> OptTraceable<T> of(Traceable<T> traceable) {
    return of(traceable.getSource(), traceable.get());
  }

  static <T> OptTraceable<T> ofNullable(Source source, @Nullable T value) {
    return of(source, Optional.ofNullable(value));
  }

  static <T> OptTraceable<T> empty(Source source) {
    return of(source, Optional.empty());
  }

  static <T> OptTraceable<T> validationErrors(ValidationErrors error) {
    return new ErroneousOptTraceable<>(error);
  }
}
