package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;

public class TraceableDocNode {

  private final ValidatingDocNode delegate;
  private final Parser.Context ctx;

  public TraceableDocNode(DocNode docNode, ValidationErrors errors, Parser.Context ctx) {
    this.delegate = new ValidatingDocNode(docNode, errors, ctx);
    this.ctx = ctx;
  }

  public Traceable<ValidatingDocNode.Attribute> get(String attribute) {
    return Traceable.of(delegate.get(attribute), ctx);
  }

  public void throwExceptionForPresentErrors() throws ConfigValidationException {
    delegate.throwExceptionForPresentErrors();
  }
}
