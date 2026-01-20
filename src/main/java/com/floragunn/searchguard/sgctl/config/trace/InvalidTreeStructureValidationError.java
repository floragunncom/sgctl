package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.validation.errors.ValidationError;

class InvalidTreeStructureValidationError extends ValidationError {

  public InvalidTreeStructureValidationError(String attribute) {
    super(
        attribute,
        "A shortcut-style node (like 'a.b: value') "
            + "conflicts with a nested-style node (like 'a: { b: { c: value } }'), "
            + "resulting in an invalid tree structure.");
  }
}
