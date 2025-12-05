package com.floragunn.searchguard.sgctl.config.trace;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
public class TraceableTest {

  private record Inner(Traceable<Boolean> enabled, OptTraceable<Integer> value) {

    static Inner parse(DocNode doc, Source src) throws ConfigValidationException {
      var tDoc = TraceableDocNode.of(doc, src);
      var enabled = tDoc.get("enabled").required().asBoolean();
      var value = tDoc.get("value").asInt();

      tDoc.throwExceptionForPresentErrors();

      return new Inner(enabled, value);
    }

    static Inner legacyParse(DocNode doc) throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors());
      var enabled = vDoc.get("enabled").required().asBoolean();
      var value = vDoc.get("value").asInteger();

      vDoc.throwExceptionForPresentErrors();

      return new Inner(
          Traceable.of(Source.NONE, enabled), OptTraceable.ofNullable(Source.NONE, value));
    }
  }

  private record User(Traceable<Boolean> enabled, Traceable<String> name) {

    static User parse(DocNode doc, Source src) throws ConfigValidationException {
      var tDoc = TraceableDocNode.of(doc, src);
      var enabled = tDoc.get("user.enabled").asBoolean(true);
      var name = tDoc.get("user.name").required().asString();

      tDoc.throwExceptionForPresentErrors();

      return new User(enabled, name);
    }

    static User legacyParse(DocNode doc) throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors());
      var enabled = vDoc.get("user.enabled").withDefault(true).asBoolean();
      var name = vDoc.get("user.name").required().asString();

      vDoc.throwExceptionForPresentErrors();

      return new User(Traceable.of(Source.NONE, enabled), Traceable.of(Source.NONE, name));
    }
  }

  private record Outer(
      Traceable<Inner> inner,
      Traceable<ImmutableList<Traceable<User>>> list,
      OptTraceable<DocNode> additional) {

    static Outer parse(DocNode doc, Source src) throws ConfigValidationException {
      var tDoc = TraceableDocNode.of(doc, src);
      var abc = tDoc.get("inner").required().as(Inner::parse);
      var users = tDoc.get("list").required().asListOf(User::parse);
      var additional = tDoc.get("additional").asDocNode();

      tDoc.throwExceptionForPresentErrors();

      return new Outer(abc, users, additional);
    }

    static Outer legacyParse(DocNode doc) throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors());
      var abc = vDoc.get("inner").required().by(Inner::legacyParse);
      var users = vDoc.get("list").required().asList().ofObjectsParsedBy(User::legacyParse);
      var additional = vDoc.get("additional").asDocNode();

      vDoc.throwExceptionForPresentErrors();

      return new Outer(
          Traceable.of(Source.NONE, abc),
          Traceable.of(Source.NONE, Traceable.ofList(Source.NONE, users)),
          OptTraceable.ofNullable(Source.NONE, additional));
    }
  }

  @Test
  public void testInvalidValuesForTypesValidationErrors() throws ConfigValidationException {
    assertEqualValidationErrors(
        """
            root:
              outer:
                inner:
                  enabled: maybe
                  value: three
                list: []
            """);
  }

  @Test
  public void testListMissingValidationError() throws ConfigValidationException {
    assertEqualValidationErrors(
        """
            root:
              outer:
                inner:
                  enabled: true
            """);
  }

  @Test
  public void testListEntriesInvalidValidationError() throws ConfigValidationException {
    assertEqualValidationErrors(
        """
            root:
              outer:
                inner:
                  enabled: true
                list:
                - user:
                    name: "user1"
                - user:
                    name:
            """);
  }

  private void assertEqualValidationErrors(String yaml) throws ConfigValidationException {
    var node = DocNode.wrap(DocReader.yaml().read(yaml));

    var tValidationErrors = new ValidationErrors();
    var vValidationErrors = new ValidationErrors();

    var tDoc = TraceableDocNode.of(node, Source.NONE, tValidationErrors);
    var vDoc = new ValidatingDocNode(node, vValidationErrors);

    tDoc.get("root.outer").required().as(Outer::parse);
    vDoc.get("root.outer").required().by(Outer::legacyParse);

    assertEquals(vValidationErrors.toString(), tValidationErrors.toString());
  }

  @Test
  public void testAsTraceableDocNodeWorks() throws ConfigValidationException {
    var yaml =
        """
        root:
          stuff: "blah"
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var root = vDoc.get("root").required().asTraceableDocNode();
    var stuff = root.get("stuff").required();
    assertEquals("test.yml: root.stuff", stuff.getSource().fullPathString());
  }

  @Test
  public void testAsTraceableDocNodeFails() throws ConfigValidationException {
    var yaml =
        """
        blah:
          stuff: "blah"
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    vDoc.get("root").required().asTraceableDocNode();
    assertThrows(ConfigValidationException.class, vDoc::throwExceptionForPresentErrors);
  }

  @Test
  public void testTraceablePaths() throws ConfigValidationException {
    var yaml =
        """
        root:
          outer:
            inner:
              enabled: true
              value: 3
            list:
            - user:
                enabled: false
                name: "user1"
            - user:
                name: "user2"
            additional:
              test: "lol"
        """;

    var node = DocNode.wrap(DocReader.yaml().read(yaml));

    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));

    var outer = vDoc.get("root.outer").required().as(Outer::parse);
    vDoc.throwExceptionForPresentErrors();

    var inner = outer.get().inner();
    var enabled = inner.get().enabled();
    var value = inner.get().value();
    var list = outer.get().list();
    var user1 = list.get().get(0);
    var user1Enabled = user1.get().enabled();
    var user1Name = user1.get().name();
    var user2 = list.get().get(1);
    var user2Enabled = user2.get().enabled();
    var user2Name = user2.get().name();
    var additional = outer.get().additional();

    assertEquals("test.yml: root.outer", outer.getSource().fullPathString());
    assertEquals("test.yml: root.outer.inner", inner.getSource().fullPathString());
    assertEquals("test.yml: root.outer.inner.enabled", enabled.getSource().fullPathString());
    assertEquals("test.yml: root.outer.inner.value", value.getSource().fullPathString());
    assertEquals("test.yml: root.outer.list", list.getSource().fullPathString());
    assertEquals("test.yml: root.outer.list.0", user1.getSource().fullPathString());
    assertEquals(
        "test.yml: root.outer.list.0.user.enabled", user1Enabled.getSource().fullPathString());
    assertEquals("test.yml: root.outer.list.0.user.name", user1Name.getSource().fullPathString());
    assertEquals("test.yml: root.outer.list.1", user2.getSource().fullPathString());
    assertEquals(
        "test.yml: root.outer.list.1.user.enabled", user2Enabled.getSource().fullPathString());
    assertEquals("test.yml: root.outer.list.1.user.name", user2Name.getSource().fullPathString());
    assertEquals("test.yml: root.outer.additional", additional.getSource().fullPathString());
  }
}
