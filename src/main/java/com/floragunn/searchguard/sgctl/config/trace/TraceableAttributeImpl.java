package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.fluent.collections.ImmutableList;
import org.jspecify.annotations.Nullable;

abstract class TraceableAttributeImpl implements TraceableAttribute {

  protected final Source.Attribute source;
  protected final DocNode node;
  protected final ValidationErrors errors;

  protected @Nullable String expected;

  public TraceableAttributeImpl(Source.Attribute source, DocNode node, ValidationErrors errors) {
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

  protected <T> ImmutableList<Traceable<T>> parseList(TraceableParser<T> parser) {
    var list = node.getAsListOfNodes(null);
    var builder = new ImmutableList.Builder<Traceable<T>>();
    for (int i = 0; i < list.size(); i++) {
      var element = list.get(i);
      var elementSource = new Source.ListEntry(source, i);
      try {
        builder.add(Traceable.of(elementSource, parser.parse(element, elementSource)));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart() + "." + i, e);
        builder.add(Traceable.validationErrors(e.getValidationErrors()));
      }
    }
    return builder.build();
  }

  static final class OptionalImpl extends TraceableAttributeImpl
      implements TraceableAttribute.Optional {

    public OptionalImpl(Source.Attribute source, DocNode node, ValidationErrors errors) {
      super(source, node, errors);
    }

    @Override
    public <T> OptTraceable<T> as(TraceableParser<T> parser) {
      if (node.isNull() || node.isEmpty()) return OptTraceable.empty(source);

      try {
        return OptTraceable.of(source, parser.parse(node, source));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart(), e);
        return OptTraceable.validationErrors(e.getValidationErrors());
      }
    }

    @Override
    public TraceableDocNode asTraceableDocNode() {
      return TraceableDocNode.of(node, source, new ValidationErrors(errors, source.name()));
    }

    @Override
    public <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(TraceableParser<T> parser) {
      if (node.isNull()) return OptTraceable.empty(source);
      return OptTraceable.of(source, parseList(parser));
    }

    @Override
    public Required required() {
      return new RequiredImpl(source, node, errors);
    }
  }

  static final class RequiredImpl extends TraceableAttributeImpl
      implements TraceableAttribute.Required {

    public RequiredImpl(Source.Attribute source, DocNode node, ValidationErrors errors) {
      super(source, node, errors);
    }

    @Override
    public <T> Traceable<T> as(TraceableParser<T> parser) {
      if (node.isNull() || node.isEmpty()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }

      try {
        return Traceable.of(source, parser.parse(node, source));
      } catch (ConfigValidationException e) {
        errors.add(source.pathPart(), e);
        return Traceable.validationErrors(e.getValidationErrors());
      }
    }

    @Override
    public TraceableDocNode asTraceableDocNode() {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
      }
      return TraceableDocNode.of(node, source, new ValidationErrors(errors, source.name()));
    }

    @Override
    public <T> Traceable<ImmutableList<Traceable<T>>> asListOf(TraceableParser<T> parser) {
      if (node.isNull()) {
        var error = new MissingAttribute(source.pathPart(), node);
        errors.add(error);
        return Traceable.validationErrors(new ValidationErrors(error));
      }
      return Traceable.of(source, parseList(parser));
    }
  }
}
