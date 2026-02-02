package com.floragunn.searchguard.sgctl.config.trace;

@FunctionalInterface
public interface TraceableDocNodeParser<R> {

  R parse(TraceableDocNode node);
}
