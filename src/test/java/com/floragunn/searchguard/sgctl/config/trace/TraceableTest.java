package com.floragunn.searchguard.sgctl.config.trace;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
public class TraceableTest {

  private record Inner(Traceable<Boolean> enabled, OptTraceable<Integer> value) {

    static Inner parse(TraceableDocNode tDoc) {
      return new Inner(tDoc.get("enabled").required().asBoolean(), tDoc.get("value").asInt());
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

    static User parse(TraceableDocNode tDoc) {
      return new User(
          tDoc.get("user.enabled").asBoolean(true), tDoc.get("user.name").required().asString());
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
      OptTraceable<Inner> inner2,
      Traceable<ImmutableList<Traceable<User>>> list,
      OptTraceable<DocNode> additional) {

    static Outer parse(TraceableDocNode tDoc) {
      return new Outer(
          tDoc.get("inner").required().as(Inner::parse),
          tDoc.get("inner2").as(Inner::parse),
          tDoc.get("list").required().asListOf(User::parse),
          tDoc.get("additional").asDocNode());
    }

    static Outer legacyParse(DocNode doc) throws ConfigValidationException {
      var vDoc = new ValidatingDocNode(doc, new ValidationErrors());
      var inner = vDoc.get("inner").required().by(Inner::legacyParse);
      var inner2 = vDoc.get("inner2").by(Inner::legacyParse);
      var list = vDoc.get("list").required().asList().ofObjectsParsedBy(User::legacyParse);
      var additional = vDoc.get("additional").asDocNode();

      vDoc.throwExceptionForPresentErrors();

      return new Outer(
          Traceable.of(Source.NONE, inner),
          OptTraceable.ofNullable(Source.NONE, inner2),
          Traceable.of(Source.NONE, Traceable.ofList(Source.NONE, list)),
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
                inner2:
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
                inner2:
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
  public void testTraceableMap() throws ConfigValidationException {
    var yaml =
        """
        root:
          inners:
            foo.enabled: true
            foo:
              value: 3
            bar:
              enabled: false
            bar.value: 7
          stringMap:
            a: "lol"
            b: "zzz"
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("map_test.yml"));

    var innersOpt = vDoc.get("root.inners").asMapOf(Inner::parse);
    var stringMap = vDoc.get("root.stringMap").required().asMapOfStrings();
    var defaultMap = vDoc.get("root.defaultMap").asMapOfInts(ImmutableMap.of("first", 1));
    vDoc.throwExceptionForPresentErrors();

    var inners = innersOpt.get().orElseThrow();
    assertTrue(inners.containsKey("foo"));
    assertTrue(inners.containsKey("bar"));
    var foo = inners.get("foo");
    var bar = inners.get("bar");
    assertNotNull(foo);
    assertNotNull(bar);
    var enabled = bar.get().value;

    var a = stringMap.get().get("a");
    var b = stringMap.get().get("b");
    assertNotNull(a);
    assertNotNull(b);

    var first = defaultMap.get().get("first");
    assertNotNull(first);
    assertEquals(1, first.get());

    assertEquals("map_test.yml: root.inners", innersOpt.getSource().fullPathString());
    assertEquals("map_test.yml: root.inners.foo", foo.getSource().fullPathString());
    assertEquals("map_test.yml: root.inners.bar", bar.getSource().fullPathString());
    assertEquals("map_test.yml: root.inners.bar.value", enabled.getSource().fullPathString());
    assertEquals("map_test.yml: root.stringMap", stringMap.getSource().fullPathString());
    assertEquals("map_test.yml: root.stringMap.a", a.getSource().fullPathString());
    assertEquals("map_test.yml: root.stringMap.b", b.getSource().fullPathString());
    assertEquals("map_test.yml: root.defaultMap", defaultMap.getSource().fullPathString());
    assertEquals("map_test.yml: root.defaultMap.first", first.getSource().fullPathString());
  }

  @Test
  public void testAsAttribute() throws ConfigValidationException {
    var yaml =
        """
        root: "test"
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var fileAsAttr = vDoc.asAttribute();
    var root = vDoc.get("root").required().asTraceableDocNode();
    var rootAttr = root.asAttribute();
    var test = rootAttr.asString();
    assertEquals("test", test.get());
    assertEquals("test.yml: ", fileAsAttr.getSource().fullPathString());
    assertEquals("test.yml: root", rootAttr.getSource().fullPathString());
    assertEquals("test.yml: root", test.getSource().fullPathString());
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
