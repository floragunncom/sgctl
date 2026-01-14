package com.floragunn.searchguard.sgctl.config.trace;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.List;
import java.util.Optional;
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
  public void testTraceableDocNodeMergeDotSeparatedWithNormalYamlComplex()
      throws ConfigValidationException {
    var yaml =
        """
                root.outer.inner.enabled: true
                root.outer:
                  inner:
                    value: 3
                  inner2.enabled: false
                  inner2:
                    value: 3
                root:
                  outer:
                    list: []
                """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, Source.NONE);
    var outer = tDoc.get("root.outer").required().as(Outer::parse);

    tDoc.throwExceptionForPresentErrors();

    assertTrue(outer.get().inner().get().enabled().get());
    assertEquals(3, outer.get().inner().get().value().getValue());
    assertFalse(outer.get().inner2().getValue().enabled().get());
    assertEquals(3, outer.get().inner2().getValue().value().getValue());
    assertEquals(List.of(), outer.get().list().get());
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
  public void testAsNestedMap() throws ConfigValidationException {
    var yaml =
        """
        root:
          foo:
            one: 1
            two: 2
          bar:
            three: 3
            four: 4
    """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("nested_test.yml"));
    var nestedMap =
        vDoc.get("root").required().asMapOf(TraceableAttribute.Required::asMapOfStrings);
    vDoc.throwExceptionForPresentErrors();

    var foo = nestedMap.get().get("foo");
    var bar = nestedMap.get().get("bar");
    var one = foo.get().get("one");
    var two = foo.get().get("two");
    var three = bar.get().get("three");
    var four = bar.get().get("four");

    assertEquals("nested_test.yml: root.foo", foo.getSource().fullPathString());
    assertEquals("nested_test.yml: root.foo.one", one.getSource().fullPathString());
    assertEquals("nested_test.yml: root.foo.two", two.getSource().fullPathString());
    assertEquals("nested_test.yml: root.bar", bar.getSource().fullPathString());
    assertEquals("nested_test.yml: root.bar.three", three.getSource().fullPathString());
    assertEquals("nested_test.yml: root.bar.four", four.getSource().fullPathString());
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
  public void testErroneousWithDefault() throws ConfigValidationException {
    var yaml = "value: 'five'";
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));

    assertDoesNotThrow(() -> tDoc.get("value").asInt(1337));
    assertThrows(ConfigValidationException.class, tDoc::throwExceptionForPresentErrors);
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

  @Test
  public void testMapTraceable() throws ConfigValidationException {
    var yaml =
        """
            a: 1
            b: "string"
            """;

    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));

    var a = vDoc.get("a").required().asInt().map(i -> i + 1);
    assertEquals(2, a.get());
    assertDoesNotThrow(() -> vDoc.get("b").required().asInt().map(i -> i + 1));
    assertThrows(ConfigValidationException.class, vDoc::throwExceptionForPresentErrors);
  }

  @Test
  public void testMapOptTraceable() throws ConfigValidationException {
    var yaml =
        """
            a: 1
            b: "string"
            """;

    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));

    var a = vDoc.get("a").asInt().map(i -> i + 1);
    assertEquals(Optional.of(2), a.get());
    assertDoesNotThrow(() -> vDoc.get("b").required().asInt().map(i -> i + 1));
    assertEquals(Optional.empty(), vDoc.get("c").asInt().map(i -> i + 1).get());
    assertThrows(ConfigValidationException.class, vDoc::throwExceptionForPresentErrors);
  }

  @Test
  public void testFlatMapTraceable() throws ConfigValidationException {
    var yaml =
        """
            a: 1
            b: "string"
            """;

    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));

    var a = vDoc.get("a").required().asInt().flatMap(i -> Traceable.of(Source.NONE, i + 1));
    assertEquals(2, a.get());
    assertEquals(Source.NONE, a.getSource());
    assertDoesNotThrow(
        () -> vDoc.get("b").required().asInt().flatMap(i -> Traceable.of(Source.NONE, i + 1)));
    assertThrows(ConfigValidationException.class, vDoc::throwExceptionForPresentErrors);
  }

  @Test
  public void testFlatMapOptTraceable() throws ConfigValidationException {
    var yaml =
        """
            a: 1
            b: "string"
            """;

    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var vDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));

    var a = vDoc.get("a").asInt().flatMap(i -> OptTraceable.ofNullable(Source.NONE, i + 1));
    var c = vDoc.get("c").asInt().flatMap(i -> OptTraceable.ofNullable(Source.NONE, i + 1));
    assertEquals(Optional.of(2), a.get());
    assertEquals(Source.NONE, a.getSource());
    assertDoesNotThrow(
        () -> vDoc.get("b").asInt().flatMap(i -> OptTraceable.ofNullable(Source.NONE, i + 1)));
    assertEquals(Optional.empty(), c.get());
    assertThrows(ConfigValidationException.class, vDoc::throwExceptionForPresentErrors);
  }

  @Test
  public void testSecretAttribute() throws ConfigValidationException {
    var yaml =
        """
        password: secret
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var toDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var passwordAttr = toDoc.get("password").required().secret().asString();
    toDoc.throwExceptionForPresentErrors();

    assertTrue(passwordAttr.isSecret());
    assertEquals("***", passwordAttr.toString());
  }

  @Test
  public void testSecretPropagatesIntoSubDocNodes() throws ConfigValidationException {
    var yaml =
        """
        parent:
          a: secret
          b: secret
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var parent = tDoc.get("parent").secret().asTraceableDocNode();
    var a = parent.get("a").required().asString();
    var b = parent.get("b").required().asString();
    tDoc.throwExceptionForPresentErrors();

    assertTrue(a.isSecret());
    assertTrue(b.isSecret());
    assertEquals("***", a.toString());
    assertEquals("***", b.toString());
  }

  @Test
  public void testSecretPropagatesIntoListElements() throws ConfigValidationException {
    var yaml =
        """
        list:
        - secret1
        - secret2
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var list = tDoc.get("list").required().secret().asListOfStrings();
    tDoc.throwExceptionForPresentErrors();

    var elem1 = list.get().get(0);
    var elem2 = list.get().get(1);

    assertTrue(list.isSecret());
    assertTrue(elem1.isSecret());
    assertTrue(elem2.isSecret());
    assertEquals("***", list.toString());
    assertEquals("***", elem1.toString());
    assertEquals("***", elem2.toString());
  }

  @Test
  public void testSecretPropagatesIntoMapValues() throws ConfigValidationException {
    var yaml =
        """
        map:
          a: secret1
          b: secret2
        """;
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var map = tDoc.get("map").required().secret().asMapOfStrings();
    tDoc.throwExceptionForPresentErrors();

    var valA = map.get().get("a");
    var valB = map.get().get("b");

    assertTrue(map.isSecret());
    assertTrue(valA.isSecret());
    assertTrue(valB.isSecret());
    assertEquals("***", map.toString());
    assertEquals("***", valA.toString());
    assertEquals("***", valB.toString());
  }

  @Test
  public void testSecretPropagatesIntoListDefaults() throws ConfigValidationException {
    var yaml = "not: empty";
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var list = tDoc.get("list").secret().asListOfStrings(ImmutableList.of("default1", "default2"));
    tDoc.throwExceptionForPresentErrors();

    var elem1 = list.get().get(0);
    var elem2 = list.get().get(1);

    assertTrue(list.isSecret());
    assertTrue(elem1.isSecret());
    assertTrue(elem2.isSecret());
    assertEquals("***", list.toString());
    assertEquals("***", elem1.toString());
    assertEquals("***", elem2.toString());
  }

  @Test
  public void testSecretPropagatesIntoMapDefaults() throws ConfigValidationException {
    var yaml = "not: empty";
    var node = DocNode.wrap(DocReader.yaml().read(yaml));
    var tDoc = TraceableDocNode.of(node, new Source.Config("test.yml"));
    var map =
        tDoc.get("map")
            .secret()
            .asMapOfStrings(ImmutableMap.of("key1", "default1", "key2", "default2"));
    tDoc.throwExceptionForPresentErrors();

    var valA = map.get().get("key1");
    var valB = map.get().get("key2");

    assertTrue(map.isSecret());
    assertTrue(valA.isSecret());
    assertTrue(valB.isSecret());
    assertEquals("***", map.toString());
    assertEquals("***", valA.toString());
    assertEquals("***", valB.toString());
  }
}
