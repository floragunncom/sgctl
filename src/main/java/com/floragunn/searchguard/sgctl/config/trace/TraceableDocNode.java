package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;

public interface TraceableDocNode {

  static TraceableDocNode of(DocNode doc, Source source, ValidationErrors errors) {
    return new TraceableDocNodeImpl(doc, errors, source);
  }

  static TraceableDocNode of(DocNode doc, Source source) {
    return of(doc, source, new ValidationErrors());
  }

  TraceableAttribute.Optional get(String attribute);

  boolean hasNonNull(String attribute);

  int getAttributeCount();

  TraceableAttribute.Required asAttribute();

  Source getSource();

  ValidationErrors getErrors();

  void throwExceptionForPresentErrors() throws ConfigValidationException;
}
