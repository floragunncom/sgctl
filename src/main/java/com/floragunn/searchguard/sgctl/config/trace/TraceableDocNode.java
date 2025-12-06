package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import java.util.Optional;

public interface TraceableDocNode {

  static TraceableDocNode of(DocNode doc, Source source, ValidationErrors errors) {
    return new TraceableDocNodeImpl(doc, errors, source);
  }

  static TraceableDocNode of(DocNode doc, Source source) {
    return of(doc, source, new ValidationErrors());
  }

  TraceableAttribute.Optional get(String attribute);

  boolean hasNonNull(String attribute);

  Optional<TraceableAttribute.Required> tryAsAttribute();

  Source getSource();

  void throwExceptionForPresentErrors() throws ConfigValidationException;
}
