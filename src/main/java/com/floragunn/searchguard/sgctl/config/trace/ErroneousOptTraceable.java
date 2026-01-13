package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import java.util.Optional;
import java.util.function.Function;

class ErroneousOptTraceable<T> implements OptTraceable<T> {

  private final ValidationErrors errors;

  public ErroneousOptTraceable(ValidationErrors errors) {
    this.errors = errors;
  }

  @Override
  public Source getSource() {
    throw new UnhandledConfigValidationException(new ConfigValidationException(errors));
  }

  @Override
  public Optional<T> get() {
    throw new UnhandledConfigValidationException(new ConfigValidationException(errors));
  }

  @Override
  public <U> OptTraceable<U> map(Function<? super T, ? extends U> mapper) {
    return new ErroneousOptTraceable<>(errors);
  }

  @Override
  public <U> OptTraceable<U> flatMap(
      Function<? super T, ? extends OptTraceable<? extends U>> mapper) {
    return new ErroneousOptTraceable<>(errors);
  }

  @Override
  public Traceable<T> orElse(T other) {
    return new ErroneousTraceable<>(errors);
  }
}
