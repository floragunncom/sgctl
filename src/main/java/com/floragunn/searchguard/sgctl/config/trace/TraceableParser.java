package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.errors.InvalidAttributeValue;
import com.floragunn.codova.validation.errors.MissingAttribute;
import java.util.Arrays;
import java.util.Locale;

// TODO: for numeric parsers, maybe check whether there is conversion loss
@FunctionalInterface
public interface TraceableParser<R> {

  R parse(DocNode doc, Source source) throws ConfigValidationException;

  TraceableParser<String> STRING =
      (doc, source) -> {
        validateNotNull(doc);
        var obj = doc.get(null);
        if (obj instanceof String
            || obj instanceof Number
            || obj instanceof Character
            || obj instanceof Boolean
            || obj instanceof Enum) {
          return String.valueOf(obj);
        } else {
          throw new ConfigValidationException(
              new InvalidAttributeValue(null, obj, "A string value"));
        }
      };

  TraceableParser<Integer> INT =
      (doc, source) -> {
        validateNotNull(doc);
        var obj = doc.get(null);
        if (obj instanceof Number num) {
          return num.intValue();
        } else {
          throw new ConfigValidationException(
              new InvalidAttributeValue(null, obj, "A numeric value"));
        }
      };

  TraceableParser<Long> LONG =
      (doc, source) -> {
        validateNotNull(doc);
        var obj = doc.get(null);
        if (obj instanceof Number num) {
          return num.longValue();
        } else {
          throw new ConfigValidationException(
              new InvalidAttributeValue(null, obj, "A numeric value"));
        }
      };

  TraceableParser<Float> FLOAT =
      (doc, source) -> {
        validateNotNull(doc);
        var obj = doc.get(null);
        if (obj instanceof Number num) {
          return num.floatValue();
        } else {
          throw new ConfigValidationException(
              new InvalidAttributeValue(null, obj, "A numeric value"));
        }
      };

  TraceableParser<Double> DOUBLE =
      (doc, source) -> {
        validateNotNull(doc);
        var obj = doc.get(null);
        if (obj instanceof Number num) {
          return num.doubleValue();
        } else {
          throw new ConfigValidationException(
              new InvalidAttributeValue(null, obj, "A numeric value"));
        }
      };

  TraceableParser<Boolean> BOOLEAN =
      (doc, source) -> {
        validateNotNull(doc);
        var obj = doc.get(null);
        if (obj instanceof Boolean bool) {
          return bool;
        } else {
          throw new ConfigValidationException(
              new InvalidAttributeValue(null, obj, "true or false"));
        }
      };

  TraceableParser<DocNode> DOC_NODE =
      (doc, source) -> {
        validateNotNull(doc);
        return doc;
      };

  TraceableParser<TraceableDocNode> TRACEABLE_DOC_NODE =
      (doc, source) -> {
        validateNotNull(doc);
        return TraceableDocNode.of(doc, source);
      };

  static <E extends Enum<E>> TraceableParser<E> enumeration(Class<E> enumClass) {
    return (doc, source) -> {
      validateNotNull(doc);
      var obj = doc.get(null);

      if (obj instanceof String value) {
        for (E e : enumClass.getEnumConstants()) {
          if (value.equalsIgnoreCase(e.name())) {
            return e;
          }
        }
      }

      var enumNames =
          Arrays.stream(enumClass.getEnumConstants())
              .map(e -> e.name().toLowerCase(Locale.ROOT))
              .toList();

      throw new ConfigValidationException(
          new InvalidAttributeValue(null, obj, "Any of: " + enumNames));
    };
  }

  private static void validateNotNull(DocNode docNode) throws ConfigValidationException {
    if (docNode.isNull() || docNode.isEmpty()) {
      throw new ConfigValidationException(new MissingAttribute(null, null));
    }
  }
}
