package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.ConfigValidationException;

public class UnhandledConfigValidationException extends RuntimeException {
  public UnhandledConfigValidationException(ConfigValidationException ex) {
    super(
        "A ConfigValidationException was thrown and suppressed, but then never re-thrown. (Missing .throwExceptionForPresentErrors?)",
        ex);
  }
}
