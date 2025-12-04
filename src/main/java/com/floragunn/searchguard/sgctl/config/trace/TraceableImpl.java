package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.searchguard.sgctl.util.ThrowingFunction;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Optional;

class TraceableImpl<T> implements Traceable<T> {

  private static final MethodHandle getAttributePath;
  private static final ThreadLocal<ArrayDeque<String>> currentPathStack =
      ThreadLocal.withInitial(ArrayDeque::new);

  static {
    MethodHandle handle;
    try {
      handle =
          MethodHandles.privateLookupIn(
                  ValidatingDocNode.AbstractAttribute.class, MethodHandles.lookup())
              .findVirtual(
                  ValidatingDocNode.AbstractAttribute.class,
                  "getAttributePathForValidationError",
                  MethodType.methodType(String.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      e.printStackTrace();
      handle = null;
    }
    getAttributePath = handle;
  }

  private final Source source;
  private final T value;

  public TraceableImpl(Source source, T value) {
    this.source = source;
    this.value = value;
  }

  @Override
  public T get() {
    return value;
  }

  @Override
  public <R, E extends Throwable> Traceable<R> map(
      ThrowingFunction<? super T, ? extends R, E> transform) throws E {
    if (source instanceof Source.Doc dns) currentPathStack.get().push(dns.path());

    try {
      return new TraceableImpl<>(source, transform.apply(value));
    } finally {
      if (source instanceof Source.Doc) currentPathStack.get().pop();
    }
  }

  static <T extends ValidatingDocNode.Attribute> TraceableImpl<T> of(
      T attribute, Parser.Context ctx) {
    if (getAttributePath == null) return new TraceableImpl<>(new Source.None(), attribute);

    try {
      var path = (String) getAttributePath.invoke(attribute);
      var currentPath = currentPathStack.get().peek();
      var fullPath = currentPath == null ? path : currentPath + "." + path;

      Optional<String> fileName;
      Optional<DocNode> rootNode;
      if (ctx instanceof TraceParserContext tpc) {
        fileName = Optional.of(tpc.fileName());
        rootNode = Optional.ofNullable(tpc.rootNode());
      } else {
        fileName = Optional.empty();
        rootNode = Optional.empty();
      }

      return new TraceableImpl<>(new Source.Doc(fullPath, fileName, rootNode), attribute);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public String toString() {
    return "TraceableImpl{" + "source=" + source + ", value=" + value + '}';
  }
}
