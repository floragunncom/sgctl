package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.searchguard.sgctl.util.ThrowingFunction;
import java.util.Optional;

public interface Traceable<T> {

  Source getSource();

  <R, E extends Throwable> Traceable<R> map(ThrowingFunction<? super T, ? extends R, E> transform)
      throws E;

  T get();

  sealed interface Source {

    record None() implements Source {}

    record Doc(String path, Optional<String> file, Optional<DocNode> rootNode) implements Source {}
  }

  static <T extends ValidatingDocNode.Attribute> Traceable<T> of(T attribute, Parser.Context ctx) {
    return TraceableImpl.of(attribute, ctx);
  }
}
