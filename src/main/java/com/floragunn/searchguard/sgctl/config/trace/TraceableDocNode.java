package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableSet;

public interface TraceableDocNode {

  static TraceableDocNode of(
      DocNode doc, Source source, ValidationErrors errors, boolean isSecret) {
    return new TraceableDocNodeImpl(doc, errors, source, isSecret);
  }

  static TraceableDocNode of(DocNode doc, Source source, ValidationErrors errors) {
    return of(doc, source, errors, false);
  }

  static TraceableDocNode of(DocNode doc, Source source) {
    return of(doc, source, new ValidationErrors());
  }

  static <T> T parse(DocNode doc, Source source, TraceableDocNodeParser<T> parser)
      throws ConfigValidationException {
    var tDoc = of(doc, source);
    var result = parser.parse(tDoc);
    tDoc.throwExceptionForPresentErrors();
    return result;
  }

  TraceableAttribute.Optional get(String attribute);

  boolean hasNonNull(String attribute);

  int getAttributeCount();

  ImmutableSet<String> getAttributeNames();

  TraceableAttribute.Required asAttribute();

  Source getSource();

  ValidationErrors getErrors();

  void throwExceptionForPresentErrors() throws ConfigValidationException;
}
