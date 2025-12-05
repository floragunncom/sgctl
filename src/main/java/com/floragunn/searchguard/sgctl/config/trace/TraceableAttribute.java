package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.fluent.collections.ImmutableList;

public interface TraceableAttribute {

  Source getSource();

  void expected(String message);

  interface Optional extends TraceableAttribute {

    default OptTraceable<String> asString() {
      return as(TraceableParser.STRING);
    }

    default OptTraceable<Integer> asInt() {
      return as(TraceableParser.INT);
    }

    default OptTraceable<Long> asLong() {
      return as(TraceableParser.LONG);
    }

    default OptTraceable<Float> asFloat() {
      return as(TraceableParser.FLOAT);
    }

    default OptTraceable<Double> asDouble() {
      return as(TraceableParser.DOUBLE);
    }

    default OptTraceable<Boolean> asBoolean() {
      return as(TraceableParser.BOOLEAN);
    }

    default <E extends Enum<E>> OptTraceable<E> asEnum(Class<E> enumClass) {
      return as(TraceableParser.enumeration(enumClass));
    }

    <T> OptTraceable<T> as(TraceableParser<T> parser);

    default Traceable<String> asString(String defaultValue) {
      return as(TraceableParser.STRING, defaultValue);
    }

    default Traceable<Integer> asInt(Integer defaultValue) {
      return as(TraceableParser.INT, defaultValue);
    }

    default Traceable<Long> asLong(Long defaultValue) {
      return as(TraceableParser.LONG, defaultValue);
    }

    default Traceable<Float> asFloat(Float defaultValue) {
      return as(TraceableParser.FLOAT, defaultValue);
    }

    default Traceable<Double> asDouble(Double defaultValue) {
      return as(TraceableParser.DOUBLE, defaultValue);
    }

    default Traceable<Boolean> asBoolean(Boolean defaultValue) {
      return as(TraceableParser.BOOLEAN, defaultValue);
    }

    default <T> Traceable<T> as(TraceableParser<T> parser, T defaultValue) {
      return as(parser).orElse(defaultValue);
    }

    default OptTraceable<ImmutableList<Traceable<String>>> asListOfStrings() {
      return asListOf(TraceableParser.STRING);
    }

    default OptTraceable<ImmutableList<Traceable<Integer>>> asListOfInts() {
      return asListOf(TraceableParser.INT);
    }

    default OptTraceable<ImmutableList<Traceable<Long>>> asListOfLongs() {
      return asListOf(TraceableParser.LONG);
    }

    default OptTraceable<ImmutableList<Traceable<Float>>> asListOfFloats() {
      return asListOf(TraceableParser.FLOAT);
    }

    default OptTraceable<ImmutableList<Traceable<Double>>> asListOfDoubles() {
      return asListOf(TraceableParser.DOUBLE);
    }

    default OptTraceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans() {
      return asListOf(TraceableParser.BOOLEAN);
    }

    <T> OptTraceable<ImmutableList<Traceable<T>>> asListOf(TraceableParser<T> parser);

    default Traceable<ImmutableList<Traceable<String>>> asListOfStrings(
        ImmutableList<String> defaultValue) {
      return asListOf(TraceableParser.STRING, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Integer>>> asListOfInts(
        ImmutableList<Integer> defaultValue) {
      return asListOf(TraceableParser.INT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Long>>> asListOfLongs(
        ImmutableList<Long> defaultValue) {
      return asListOf(TraceableParser.LONG, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Float>>> asListOfFloats(
        ImmutableList<Float> defaultValue) {
      return asListOf(TraceableParser.FLOAT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Double>>> asListOfDoubles(
        ImmutableList<Double> defaultValue) {
      return asListOf(TraceableParser.DOUBLE, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans(
        ImmutableList<Boolean> defaultValue) {
      return asListOf(TraceableParser.BOOLEAN, defaultValue);
    }

    default <T> Traceable<ImmutableList<Traceable<T>>> asListOf(
        TraceableParser<T> parser, ImmutableList<T> defaultValue) {
      return asListOf(parser).orElse(Traceable.ofList(getSource(), defaultValue));
    }

    default Traceable<ImmutableList<Traceable<String>>> asListOfStrings(String... defaultValue) {
      return asListOf(TraceableParser.STRING, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Integer>>> asListOfInts(Integer... defaultValue) {
      return asListOf(TraceableParser.INT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Long>>> asListOfLongs(Long... defaultValue) {
      return asListOf(TraceableParser.LONG, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Float>>> asListOfFloats(Float... defaultValue) {
      return asListOf(TraceableParser.FLOAT, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Double>>> asListOfDoubles(Double... defaultValue) {
      return asListOf(TraceableParser.DOUBLE, defaultValue);
    }

    default Traceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans(Boolean... defaultValue) {
      return asListOf(TraceableParser.BOOLEAN, defaultValue);
    }

    default <T> Traceable<ImmutableList<Traceable<T>>> asListOf(
        TraceableParser<T> parser, T... defaultValue) {
      return asListOf(parser).orElse(Traceable.ofList(getSource(), defaultValue));
    }

    Required required();
  }

  interface Required extends TraceableAttribute {

    default Traceable<String> asString() {
      return as(TraceableParser.STRING);
    }

    default Traceable<Integer> asInt() {
      return as(TraceableParser.INT);
    }

    default Traceable<Long> asLong() {
      return as(TraceableParser.LONG);
    }

    default Traceable<Float> asFloat() {
      return as(TraceableParser.FLOAT);
    }

    default Traceable<Double> asDouble() {
      return as(TraceableParser.DOUBLE);
    }

    default Traceable<Boolean> asBoolean() {
      return as(TraceableParser.BOOLEAN);
    }

    default <E extends Enum<E>> Traceable<E> asEnum(Class<E> enumClass) {
      return as(TraceableParser.enumeration(enumClass));
    }

    <T> Traceable<T> as(TraceableParser<T> parser);

    default Traceable<ImmutableList<Traceable<String>>> asListOfStrings() {
      return asListOf(TraceableParser.STRING);
    }

    default Traceable<ImmutableList<Traceable<Integer>>> asListOfInts() {
      return asListOf(TraceableParser.INT);
    }

    default Traceable<ImmutableList<Traceable<Long>>> asListOfLongs() {
      return asListOf(TraceableParser.LONG);
    }

    default Traceable<ImmutableList<Traceable<Float>>> asListOfFloats() {
      return asListOf(TraceableParser.FLOAT);
    }

    default Traceable<ImmutableList<Traceable<Double>>> asListOfDoubles() {
      return asListOf(TraceableParser.DOUBLE);
    }

    default Traceable<ImmutableList<Traceable<Boolean>>> asListOfBooleans() {
      return asListOf(TraceableParser.BOOLEAN);
    }

    <T> Traceable<ImmutableList<Traceable<T>>> asListOf(TraceableParser<T> parser);
  }
}
