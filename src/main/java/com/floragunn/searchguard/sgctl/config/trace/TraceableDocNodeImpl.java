package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableSet;
import java.util.HashMap;
import java.util.Map;

class TraceableDocNodeImpl implements TraceableDocNode {

  private final DocNode docNode;
  private final ValidationErrors errors;
  private final Source source;

  public TraceableDocNodeImpl(DocNode docNode, ValidationErrors errors, Source source) {
    this.docNode = exploded(docNode);
    this.errors = errors;
    this.source = source;
  }

  // <editor-fold desc="explode DocNode">

  // TODO: for invalid configs with leaf values colliding with maps, throw a validation error
  //  instead of class cast exception

  private static DocNode exploded(DocNode input) {
    if (!input.isMap()) return input;
    return DocNode.wrap(exploded(input.toMap()));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> exploded(Map<String, Object> input) {
    Map<String, Object> result = new HashMap<>();
    for (var entry : input.entrySet()) {
      var key = entry.getKey();
      var value =
          (entry.getValue() instanceof Map<?, ?> m)
              ? exploded((Map<String, Object>) m)
              : entry.getValue();

      var keyParts = key.split("\\.");
      if (keyParts.length > 1) {
        var currentMap = result;
        for (int i = 0; i < keyParts.length - 1; i++) {
          var part = keyParts[i];
          if (!currentMap.containsKey(part)) {
            currentMap.put(part, new HashMap<String, Object>());
          }
          currentMap = (Map<String, Object>) currentMap.get(part);
        }
        putOrMergeInto(currentMap, keyParts[keyParts.length - 1], value);
      } else {
        putOrMergeInto(result, key, value);
      }
    }
    return result;
  }

  private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
    Map<String, Object> result = new HashMap<>(a);
    for (var entry : b.entrySet()) {
      var key = entry.getKey();
      var valueB = entry.getValue();
      putOrMergeInto(result, key, valueB);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void putOrMergeInto(Map<String, Object> target, String key, Object value) {
    if (target.containsKey(key)) {
      var existingValue = target.get(key);
      if (existingValue instanceof Map && value instanceof Map) {
        value = merge((Map<String, Object>) existingValue, (Map<String, Object>) value);
      }
    }
    target.put(key, value);
  }

  // </editor-fold>

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
