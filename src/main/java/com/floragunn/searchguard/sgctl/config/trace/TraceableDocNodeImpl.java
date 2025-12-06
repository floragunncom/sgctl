package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import java.util.Optional;

class TraceableDocNodeImpl implements TraceableDocNode {

  private final DocNode docNode;
  private final ValidationErrors errors;
  private final Source source;

  public TraceableDocNodeImpl(DocNode docNode, ValidationErrors errors, Source source) {
    this.docNode = docNode;
    this.errors = errors;
    this.source = source;
  }

  @Override
  public TraceableAttribute.Optional get(String attribute) {
    if (docNode.hasNonNull(attribute)) {
      return new TraceableAttributeImpl.OptionalImpl(
          new Source.Attribute(source, attribute), docNode.getAsNode(attribute), errors);
    }

    DocNode currentNode = docNode;
    var path = attribute.split("\\.");
    for (var pathElement : path) {
      currentNode = currentNode.getAsNode(pathElement);
    }

    return new TraceableAttributeImpl.OptionalImpl(
        new Source.Attribute(source, attribute), currentNode, errors);
  }

  @Override
  public Optional<TraceableAttribute.Required> tryAsAttribute() {
    if (source instanceof Source.Attribute) {
      return Optional.of(
          new TraceableAttributeImpl.RequiredImpl((Source.Attribute) source, docNode, errors));
    }
    return Optional.empty();
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public boolean hasNonNull(String attribute) {
    return docNode.hasNonNull(attribute);
  }

  @Override
  public void throwExceptionForPresentErrors() throws ConfigValidationException {
    errors.throwExceptionForPresentErrors();
  }
}
