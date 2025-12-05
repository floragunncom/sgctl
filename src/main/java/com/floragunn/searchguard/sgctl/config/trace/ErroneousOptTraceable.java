package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import java.util.Optional;

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
  public Traceable<T> orElse(T other) {
    throw new UnhandledConfigValidationException(new ConfigValidationException(errors));
  }
}
