package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.*;
import org.jspecify.annotations.Nullable;

abstract class TraceableAttributeImpl implements TraceableAttribute {

  protected final Source source;
  protected final DocNode node;
  protected final ValidationErrors errors;

  protected @Nullable String expected;

  public TraceableAttributeImpl(Source source, DocNode node, ValidationErrors errors) {
    this.source = source;
    this.node = node;
    this.errors = errors;
  }

  @Override
  public void expected(String message) {
    this.expected = message;
  }

  @Override
  public Source getSource() {
    return source;
  }

  protected <T> ImmutableList<Traceable<T>> parseList(DocNodeParser<T> parser) {
    var list = node.getAsListOfNodes(null);
    var builder = new ImmutableList.Builder<Traceable<T>>();
    for (int i = 0; i < list.size(); i++) {
      var element = list.get(i);
      var elementSource = new Source.ListEntry(source, i);
      try {
        builder.add(Traceable.of(elementSource, parser.parse(element)));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart() + "." + i, e);
        builder.add(Traceable.validationErrors(e.getValidationErrors()));
      }
    }
    return builder.build();
  }

  protected <T> ImmutableList<Traceable<T>> parseList(TraceableDocNodeParser<T> parser) {
    var list = node.getAsListOfNodes(null);
    var builder = new ImmutableList.Builder<Traceable<T>>();
    for (int i = 0; i < list.size(); i++) {
      var element = list.get(i);
      var elementSource = new Source.ListEntry(source, i);

      var subErrors = new ValidationErrors();
      var result = parser.parse(TraceableDocNode.of(element, elementSource, subErrors));
      errors.add(source.pathPart() + "." + i, subErrors);
      builder.add(Traceable.of(elementSource, result));
    }
    return builder.build();
  }

  protected <T> ImmutableList<Traceable<T>> parseList(TraceableAttributeParser<T> parser) {
    var list = node.getAsListOfNodes(null);
    var builder = new ImmutableList.Builder<Traceable<T>>();
    for (int i = 0; i < list.size(); i++) {
      var element = list.get(i);
      var elementSource = new Source.ListEntry(source, i);

      var subErrors = new ValidationErrors();
      var attribute = new TraceableAttributeImpl.RequiredImpl(elementSource, element, subErrors);
      var result = parser.parse(attribute);
      errors.add(source.pathPart() + "." + i, subErrors);
      builder.add(result);
    }
    return builder.build();
  }

  protected <T> ImmutableMap<String, Traceable<T>> parseMap(DocNodeParser<T> parser) {
    var builder = new ImmutableMap.Builder<String, Traceable<T>>();
    for (var entry : node.toMapOfNodes().entrySet()) {
      var key = entry.getKey();
      var element = entry.getValue();
      var elementSource = new Source.Attribute(source, key);

      try {
        builder.put(key, Traceable.of(elementSource, parser.parse(element)));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart() + "." + key, e);
        builder.put(key, Traceable.validationErrors(e.getValidationErrors()));
      }
    }
    return builder.build();
  }

  protected <T> ImmutableMap<String, Traceable<T>> parseMap(TraceableDocNodeParser<T> parser) {
    var builder = new ImmutableMap.Builder<String, Traceable<T>>();
    for (var entry : node.toMapOfNodes().entrySet()) {
      var key = entry.getKey();
      var element = entry.getValue();
      var elementSource = new Source.Attribute(source, key);
      var subErrors = new ValidationErrors();
      var result = parser.parse(TraceableDocNode.of(element, elementSource, subErrors));
      errors.add(source.pathPart() + "." + key, subErrors);
      builder.put(key, Traceable.of(elementSource, result));
    }
    return builder.build();
  }

  protected <T> ImmutableMap<String, Traceable<T>> parseMap(TraceableAttributeParser<T> parser) {
    var builder = new ImmutableMap.Builder<String, Traceable<T>>();
    for (var entry : node.toMapOfNodes().entrySet()) {
      var key = entry.getKey();
      var element = entry.getValue();
      var elementSource = new Source.Attribute(source, key);
      var subErrors = new ValidationErrors();
      var attribute = new TraceableAttributeImpl.RequiredImpl(elementSource, element, subErrors);
      var result = parser.parse(attribute);
      errors.add(source.pathPart() + "." + key, subErrors);
      builder.put(key, result);
    }
    return builder.build();
  }

  static final class OptionalImpl extends TraceableAttributeImpl
      implements TraceableAttribute.Optional {

    public OptionalImpl(Source source, DocNode node, ValidationErrors errors) {
      super(source, node, errors);
    }

    @Override
    public <T> OptTraceable<T> as(DocNodeParser<T> parser) {
      if (node.isNull() || node.isEmpty()) return OptTraceable.empty(source);

      try {
        return OptTraceable.of(source, parser.parse(node));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart(), e);
        return OptTraceable.validationErrors(e.getValidationErrors());
      }
    }

    @Override
    public <T> OptTraceable<T> as(TraceableDocNodeParser<T> parser) {
      if (node.isNull() || node.isEmpty()) return OptTraceable.empty(source);

      var subErrors = new ValidationErrors();
      var result = parser.parse(TraceableDocNode.of(node, source, subErrors));
      errors.add(source.pathPart(), subErrors);
      return OptTraceable.of(source, result);
    }

    @Override
    public <T> OptTraceable<T> as(TraceableAttributeParser<T> parser) {
      if (node.isNull() || node.isEmpty()) return OptTraceable.empty(source);

      var subErrors = new ValidationErrors();
      var result = parser.parse(new TraceableAttributeImpl.RequiredImpl(source, node, subErrors));
      errors.add(source.pathPart(), subErrors);
      return OptTraceable.of(result);
    }

    @Override
    public TraceableDocNode asTraceableDocNode() {
      return TraceableDocNode.of(node, source, new ValidationErrors(errors, source.pathPart()));
    }

    @Override
    public <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(DocNodeParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseList(parser));
    }

    @Override
    public <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(
        TraceableDocNodeParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseList(parser));
    }

    @Override
    public <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(
        TraceableAttributeParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseList(parser));
    }

    @Override
    public <T> OptTraceable<ImmutableMap<String, Traceable<T>>> asMapOf(
        TraceableDocNodeParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseMap(parser));
    }

    @Override
    public <T> OptTraceable<ImmutableMap<String, Traceable<T>>> asMapOf(
        TraceableAttributeParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseMap(parser));
    }

    @Override
    public <T> OptTraceable<ImmutableMap<String, Traceable<T>>> asMapOf(DocNodeParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseMap(parser));
    }

    @Override
    public Required required() {
      return new RequiredImpl(source, node, errors);
    }
  }

  static final class RequiredImpl extends TraceableAttributeImpl
      implements TraceableAttribute.Required {

    public RequiredImpl(Source source, DocNode node, ValidationErrors errors) {
      super(source, node, errors);
    }

    @Override
    public <T> Traceable<T> as(DocNodeParser<T> parser) {
      if (node.isNull() || node.isEmpty()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }

      try {
        return Traceable.of(source, parser.parse(node));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart(), e);
        return Traceable.validationErrors(e.getValidationErrors());
      }
    }

    @Override
    public <T> Traceable<T> as(TraceableDocNodeParser<T> parser) {
      if (node.isNull() || node.isEmpty()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }

      var subErrors = new ValidationErrors();
      var result = parser.parse(TraceableDocNode.of(node, source, subErrors));
      errors.add(source.pathPart(), subErrors);
      return Traceable.of(source, result);
    }

    @Override
    public <T> Traceable<T> as(TraceableAttributeParser<T> parser) {
      if (node.isNull() || node.isEmpty()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }

      var subErrors = new ValidationErrors();
      var attribute = TraceableDocNode.of(node, source, subErrors).asAttribute();
      var result = parser.parse(attribute);
      errors.add(source.pathPart(), subErrors);
      return result;
    }

    @Override
    public TraceableDocNode asTraceableDocNode() {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
      }
      return TraceableDocNode.of(node, source, new ValidationErrors(errors, source.pathPart()));
    }

    @Override
    public <T> Traceable<ImmutableList<Traceable<T>>> asListOf(DocNodeParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }
      return Traceable.of(source, parseList(parser));
    }

    @Override
    public <T> Traceable<ImmutableList<Traceable<T>>> asListOf(TraceableDocNodeParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }

      return Traceable.of(source, parseList(parser));
    }

    @Override
    public <T> Traceable<ImmutableList<Traceable<T>>> asListOf(TraceableAttributeParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }

      return Traceable.of(source, parseList(parser));
    }

    @Override
    public <T> Traceable<ImmutableMap<String, Traceable<T>>> asMapOf(DocNodeParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }
      return Traceable.of(source, parseMap(parser));
    }

    @Override
    public <T> Traceable<ImmutableMap<String, Traceable<T>>> asMapOf(
        TraceableDocNodeParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }
      return Traceable.of(source, parseMap(parser));
    }

    @Override
    public <T> Traceable<ImmutableMap<String, Traceable<T>>> asMapOf(
        TraceableAttributeParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }
      return Traceable.of(source, parseMap(parser));
    }
  }
}
