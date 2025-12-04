package com.floragunn.searchguard.sgctl.config.trace;

import com.floragunn.codova.config.templates.PipeExpression;
import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.VariableResolvers;
import com.floragunn.fluent.collections.ImmutableMap;

public interface TraceParserContext extends Parser.Context {

  String fileName();

  DocNode rootNode();

  static TraceParserContext of(String fileName, DocNode rootNode) {
    return new TraceParserContext() {
      @Override
      public VariableResolvers variableResolvers() {
        return null;
      }

      @Override
      public ImmutableMap<String, PipeExpression.PipeFunction> pipeFunctions() {
        return null;
      }

      @Override
      public String fileName() {
        return fileName;
      }

      @Override
      public DocNode rootNode() {
        return rootNode;
      }
    };
  }
}
