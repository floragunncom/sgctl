package com.floragunn.searchguard.sgctl.config.trace;

@FunctionalInterface
public interface TraceableAttributeParser<R> {

  Traceable<R> parse(TraceableAttribute.Required attribute);
}
