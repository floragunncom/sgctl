package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;

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
  public T get() {
    throw new UnhandledConfigValidationException(new ConfigValidationException(errors));
  }
}
