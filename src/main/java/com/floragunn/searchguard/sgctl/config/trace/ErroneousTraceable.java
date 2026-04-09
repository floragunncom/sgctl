package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import java.util.function.Function;

class ErroneousTraceable<T> implements Traceable<T> {

  private final ValidationErrors errors;

  public ErroneousTraceable(ValidationErrors errors) {
    this.errors = errors;
  }

  @Override
  public Source getSource() {
    throw new UnhandledConfigValidationException(new ConfigValidationException(errors));
  }

  @Override
  public boolean isSecret() {
    return false;
  }

  @Override
  public T get() {
    throw new UnhandledConfigValidationException(new ConfigValidationException(errors));
  }

  @Override
  public <R> Traceable<R> map(Function<? super T, ? extends R> mapper) {
    return new ErroneousTraceable<>(errors);
  }

  @Override
  public <R> Traceable<R> flatMap(Function<? super T, ? extends Traceable<R>> mapper) {
    return new ErroneousTraceable<>(errors);
  }
}
