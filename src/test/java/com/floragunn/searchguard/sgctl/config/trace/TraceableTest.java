package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidationErrors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TraceableTest {

  private record Inner(Traceable<Boolean> enabled, Traceable<Integer> value) {

    static Inner parse(DocNode node, Parser.Context ctx) throws ConfigValidationException {
      var vNode = new TraceableDocNode(node, new ValidationErrors(), ctx);
      var enabled = vNode.get("enabled").map(a -> a.asBoolean());
      var value = vNode.get("value").map(a -> a.asInt());

      vNode.throwExceptionForPresentErrors();

      return new Inner(enabled, value);
    }
  }

  private record Outer(Traceable<Inner> abc) {

    static Outer parse(DocNode node, Parser.Context ctx) throws ConfigValidationException {
      var vNode = new TraceableDocNode(node, new ValidationErrors(), ctx);
      var abc = vNode.get("abc").map(a -> a.by(Inner::parse));

      vNode.throwExceptionForPresentErrors();

      return new Outer(abc);
    }
  }

  @Test
  public void testTraceable() throws ConfigValidationException {
    var node =
        DocNode.wrap(
            DocReader.yaml()
                .read(
"""
trace:
  sub_object:
    abc:
      enabled: true
      value: 5
"""));

    var vDoc =
        new TraceableDocNode(node, new ValidationErrors(), TraceParserContext.of("test.yml", node));

    Traceable<Outer> subObject = vDoc.get("trace.sub_object").map(a -> a.by(Outer::parse));

    vDoc.throwExceptionForPresentErrors();

    assertTraceable("trace.sub_object", "test.yml", node, subObject);
    assertTraceable("trace.sub_object.abc", "test.yml", node, subObject.get().abc());
    assertTraceable(
        "trace.sub_object.abc.enabled", "test.yml", node, subObject.get().abc().get().enabled());
    assertTraceable(
        "trace.sub_object.abc.value", "test.yml", node, subObject.get().abc().get().value());
  }

  private <T> void assertTraceable(
      String expectedPath, String expectedFile, DocNode expectedRootNode, Traceable<T> actual) {
    var source = actual.getSource();

    Assertions.assertInstanceOf(Traceable.Source.Doc.class, source);
    var docNodeSource = (Traceable.Source.Doc) source;

    Assertions.assertEquals(expectedPath, docNodeSource.path());
    Assertions.assertTrue(docNodeSource.file().isPresent());
    Assertions.assertEquals(expectedFile, docNodeSource.file().get());
    Assertions.assertTrue(docNodeSource.rootNode().isPresent());
    Assertions.assertEquals(expectedRootNode, docNodeSource.rootNode().get());
  }
}
