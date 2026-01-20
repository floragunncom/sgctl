package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class TraceableDocNodeImpl implements TraceableDocNode {

  private final DocNode docNode;
  private final ValidationErrors errors;
  private final Source source;
  private final boolean isSecret;

  public TraceableDocNodeImpl(DocNode docNode, ValidationErrors errors, Source source) {
    this.docNode = expand(docNode, errors);
    this.errors = errors;
    this.source = source;
    this.isSecret = isSecret;
  }

  // <editor-fold desc="explode DocNode">
  private static DocNode expand(DocNode input, ValidationErrors errors) {
    if (!input.isMap()) return input;
    return DocNode.wrap(expand(input.toMap(), "", errors));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> expand(
      Map<String, Object> input, String path, ValidationErrors errors) {
    Map<String, Object> result = new HashMap<>();
    outer:
    for (var entry : input.entrySet()) {
      String key = entry.getKey();
      Object value;
      if (entry.getValue() instanceof Map<?, ?> m) {
        value = expand((Map<String, Object>) m, path + "." + key, errors);
      } else {
        value = entry.getValue();
      }

      String pathPrefix = path.isEmpty() ? "" : path + ".";
      String[] keyParts = key.split("\\.");
      if (keyParts.length > 1) {
        Map<String, Object> currentMap = result;
        for (int i = 0; i < keyParts.length - 1; i++) {
          String part = keyParts[i];
          Object current = currentMap.computeIfAbsent(part, x -> new HashMap<>());
          if (current instanceof Map<?, ?> m) {
            currentMap = (Map<String, Object>) m;
          } else {
            String conflictPath = pathPrefix + String.join(".", Arrays.copyOf(keyParts, i + 1));
            errors.add(new InvalidTreeStructureValidationError(conflictPath));
            break outer;
          }
        }
        putOrMergeInto(currentMap, keyParts[keyParts.length - 1], value, pathPrefix + key, errors);
      } else {
        putOrMergeInto(result, key, value, pathPrefix + key, errors);
      }
    }
    return result;
  }

  private static Map<String, Object> merge(
      Map<String, Object> a, Map<String, Object> b, String path, ValidationErrors errors) {
    Map<String, Object> result = new HashMap<>(a);
    for (var entry : b.entrySet()) {
      String key = entry.getKey();
      Object valueB = entry.getValue();
      String newPath = path.isEmpty() ? key : path + "." + key;
      putOrMergeInto(result, key, valueB, newPath, errors);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static void putOrMergeInto(
      Map<String, Object> target, String key, Object value, String path, ValidationErrors errors) {
    if (target.containsKey(key)) {
      Object existingValue = target.get(key);
      if (existingValue instanceof Map && value instanceof Map) {
        value =
            merge((Map<String, Object>) existingValue, (Map<String, Object>) value, path, errors);
      } else {
        errors.add(new InvalidTreeStructureValidationError(path));
      }
    }
    target.put(key, value);
  }

  // </editor-fold>

  @Override
  public TraceableAttribute.Optional get(String attribute) {
    if (docNode.hasNonNull(attribute)) {
      return new TraceableAttributeImpl.OptionalImpl(
          new Source.Attribute(source, attribute), docNode.getAsNode(attribute), errors, isSecret);
    }

    DocNode currentNode = docNode;
    var path = attribute.split("\\.");
    for (var pathElement : path) {
      currentNode = currentNode.getAsNode(pathElement);
    }

    return new TraceableAttributeImpl.OptionalImpl(
        new Source.Attribute(source, attribute), currentNode, errors, isSecret);
  }

  @Override
  public TraceableAttribute.Required asAttribute() {
    return new TraceableAttributeImpl.RequiredImpl(source, docNode, errors, isSecret);
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
