package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;

public interface TraceableAttribute {

  Source getSource();

  void expected(String message);

  TraceableDocNode asTraceableDocNode();

  interface Optional extends TraceableAttribute {

    default OptTraceable<String> asString() {
      return as(DocNodeParser.STRING);
    }

    default OptTraceable<Integer> asInt() {
      return as(DocNodeParser.INT);
    }

    default OptTraceable<Long> asLong() {
      return as(DocNodeParser.LONG);
    }

    default OptTraceable<Float> asFloat() {
      return as(DocNodeParser.FLOAT);
    }

    default OptTraceable<Double> asDouble() {
      return as(DocNodeParser.DOUBLE);
    }

    default OptTraceable<Boolean> asBoolean() {
      return as(DocNodeParser.BOOLEAN);
    }

    default <E extends Enum<E>> OptTraceable<E> asEnum(Class<E> enumClass) {
      return as(DocNodeParser.enumeration(enumClass));
    }

    default OptTraceable<DocNode> asDocNode() {
      return as(DocNodeParser.DOC_NODE);
    }

    <T> OptTraceable<T> as(DocNodeParser<T> parser);

    <T> OptTraceable<T> as(TraceableDocNodeParser<T> parser);

    default Traceable<String> asString(String defaultValue) {
      return as(DocNodeParser.STRING, defaultValue);
    }

    default Traceable<Integer> asInt(Integer defaultValue) {
      return as(DocNodeParser.INT, defaultValue);
    }

    default Traceable<Long> asLong(Long defaultValue) {
      return as(DocNodeParser.LONG, defaultValue);
    }

    default Traceable<Float> asFloat(Float defaultValue) {
      return as(DocNodeParser.FLOAT, defaultValue);
    }

    default Traceable<Double> asDouble(Double defaultValue) {
      return as(DocNodeParser.DOUBLE, defaultValue);
    }

    default Traceable<Boolean> asBoolean(Boolean defaultValue) {
      return as(DocNodeParser.BOOLEAN, defaultValue);
    }

    default <T> Traceable<T> as(DocNodeParser<T> parser, T defaultValue) {
      return as(parser).orElse(defaultValue);
    }

    default OptTraceable<ImmutableList<Traceable<String>>> asListOfStrings() {
      return asListOf(DocNodeParser.STRING);
    }

    default OptTraceable<ImmutableList<Traceable<Integer>>> asListOfInts() {
      return asListOf(DocNodeParser.INT);
    }

    default OptTraceable<ImmutableList<Traceable<Long>>> asListOfLongs() {
      return asListOf(DocNodeParser.LONG);
    }

    default OptTraceable<ImmutableList<Traceable<Float>>> asListOfFloats() {
      return asListOf(DocNodeParser.FLOAT);
    }

    default OptTraceable<ImmutableList<Traceable<Double>>> asListOfDoubles() {
      return asListOf(DocNodeParser.DOUBLE);
    }

    default OptTraceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans() {
      return asListOf(DocNodeParser.BOOLEAN);
    }

    <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(DocNodeParser<T> parser);

    <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(TraceableDocNodeParser<T> parser);

    default Traceable<ImmutableList<Traceable<String>>> asListOfStrings(
        ImmutableList<String> defaultValue) {
      return asListOf(DocNodeParser.STRING, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Integer>>> asListOfInts(
        ImmutableList<Integer> defaultValue) {
      return asListOf(DocNodeParser.INT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Long>>> asListOfLongs(
        ImmutableList<Long> defaultValue) {
      return asListOf(DocNodeParser.LONG, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Float>>> asListOfFloats(
        ImmutableList<Float> defaultValue) {
      return asListOf(DocNodeParser.FLOAT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Double>>> asListOfDoubles(
        ImmutableList<Double> defaultValue) {
      return asListOf(DocNodeParser.DOUBLE, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans(
        ImmutableList<Boolean> defaultValue) {
      return asListOf(DocNodeParser.BOOLEAN, defaultValue);
    }

    default <T> Traceable<ImmutableList<Traceable<T>>> asListOf(
        DocNodeParser<T> parser, ImmutableList<T> defaultValue) {
      return asListOf(parser).orElse(Traceable.ofList(getSource(), defaultValue));
    }

    default <T> Traceable<ImmutableList<Traceable<T>>> asListOf(
        TraceableDocNodeParser<T> parser, ImmutableList<T> defaultValue) {
      return asListOf(parser).orElse(Traceable.ofList(getSource(), defaultValue));
    }

    default Traceable<ImmutableList<Traceable<String>>> asListOfStrings(String... defaultValue) {
      return asListOf(DocNodeParser.STRING, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Integer>>> asListOfInts(Integer... defaultValue) {
      return asListOf(DocNodeParser.INT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Long>>> asListOfLongs(Long... defaultValue) {
      return asListOf(DocNodeParser.LONG, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Float>>> asListOfFloats(Float... defaultValue) {
      return asListOf(DocNodeParser.FLOAT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Double>>> asListOfDoubles(Double... defaultValue) {
      return asListOf(DocNodeParser.DOUBLE, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans(Boolean... defaultValue) {
      return asListOf(DocNodeParser.BOOLEAN, defaultValue);
    }

    default <T> Traceable<ImmutableList<Traceable<T>>> asListOf(
        DocNodeParser<T> parser, T... defaultValue) {
      return asListOf(parser).orElse(Traceable.ofList(getSource(), defaultValue));
    }

    default <T> Traceable<ImmutableList<Traceable<T>>> asListOf(
        TraceableDocNodeParser<T> parser, T... defaultValue) {
      return asListOf(parser).orElse(Traceable.ofList(getSource(), defaultValue));
    }

    <T> OptTraceable<ImmutableMap<String, Traceable<T>>> asMapOf(DocNodeParser<T> parser);

    <T> OptTraceable<ImmutableMap<String, Traceable<T>>> asMapOf(TraceableDocNodeParser<T> parser);

    default OptTraceable<ImmutableMap<String, Traceable<String>>> asMapOfStrings() {
      return asMapOf(DocNodeParser.STRING);
    }

    default OptTraceable<ImmutableMap<String, Traceable<Integer>>> asMapOfInts() {
      return asMapOf(DocNodeParser.INT);
    }

    default OptTraceable<ImmutableMap<String, Traceable<Double>>> asMapOfDoubles() {
      return asMapOf(DocNodeParser.DOUBLE);
    }

    default OptTraceable<ImmutableMap<String, Traceable<Long>>> asMapOfLongs() {
      return asMapOf(DocNodeParser.LONG);
    }

    default OptTraceable<ImmutableMap<String, Traceable<Boolean>>> asMapOfBooleans() {
      return asMapOf(DocNodeParser.BOOLEAN);
    }

    Required required();
  }

  interface Required extends TraceableAttribute {

    default Traceable<String> asString() {
      return as(DocNodeParser.STRING);
    }

    default Traceable<Integer> asInt() {
      return as(DocNodeParser.INT);
    }

    default Traceable<Long> asLong() {
      return as(DocNodeParser.LONG);
    }

    default Traceable<Float> asFloat() {
      return as(DocNodeParser.FLOAT);
    }

    default Traceable<Double> asDouble() {
      return as(DocNodeParser.DOUBLE);
    }

    default Traceable<Boolean> asBoolean() {
      return as(DocNodeParser.BOOLEAN);
    }

    default <E extends Enum<E>> Traceable<E> asEnum(Class<E> enumClass) {
      return as(DocNodeParser.enumeration(enumClass));
    }

    default Traceable<DocNode> asDocNode() {
      return as(DocNodeParser.DOC_NODE);
    }

    <T> Traceable<T> as(DocNodeParser<T> parser);

    <T> Traceable<T> as(TraceableDocNodeParser<T> parser);

    default Traceable<ImmutableList<Traceable<String>>> asListOfStrings() {
      return asListOf(DocNodeParser.STRING);
    }

    default Traceable<ImmutableList<Traceable<Integer>>> asListOfInts() {
      return asListOf(DocNodeParser.INT);
    }

    default Traceable<ImmutableList<Traceable<Long>>> asListOfLongs() {
      return asListOf(DocNodeParser.LONG);
    }

    default Traceable<ImmutableList<Traceable<Float>>> asListOfFloats() {
      return asListOf(DocNodeParser.FLOAT);
    }

    default Traceable<ImmutableList<Traceable<Double>>> asListOfDoubles() {
      return asListOf(DocNodeParser.DOUBLE);
    }

    default Traceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans() {
      return asListOf(DocNodeParser.BOOLEAN);
    }

    <T> Traceable<ImmutableList<Traceable<T>>> asListOf(DocNodeParser<T> parser);

    <T> Traceable<ImmutableList<Traceable<T>>> asListOf(TraceableDocNodeParser<T> parser);

    default Traceable<ImmutableMap<String, Traceable<String>>> asMapOfStrings() {
      return asMapOf(DocNodeParser.STRING);
    }

    default Traceable<ImmutableMap<String, Traceable<Integer>>> asMapOfInts() {
      return asMapOf(DocNodeParser.INT);
    }

    default Traceable<ImmutableMap<String, Traceable<Double>>> asMapOfDoubles() {
      return asMapOf(DocNodeParser.DOUBLE);
    }

    default Traceable<ImmutableMap<String, Traceable<Long>>> asMapOfLongs() {
      return asMapOf(DocNodeParser.LONG);
    }

    default Traceable<ImmutableMap<String, Traceable<Boolean>>> asMapOfBooleans() {
      return asMapOf(DocNodeParser.BOOLEAN);
    }

    <T> Traceable<ImmutableMap<String, Traceable<T>>> asMapOf(DocNodeParser<T> parser);

    <T> Traceable<ImmutableMap<String, Traceable<T>>> asMapOf(TraceableDocNodeParser<T> parser);
  }
}
