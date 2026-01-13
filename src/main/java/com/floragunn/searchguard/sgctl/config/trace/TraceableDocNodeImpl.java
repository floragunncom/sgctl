package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableSet;

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
  public TraceableAttribute.Required asAttribute() {
    return new TraceableAttributeImpl.RequiredImpl(source, docNode, errors);
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public ValidationErrors getErrors() {
    return errors;
  }

  @Override
  public int getAttributeCount() {
    return docNode.size();
  }

  @Override
  public ImmutableSet<String> getAttributeNames() {
    return ImmutableSet.of(docNode.keySet());
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
