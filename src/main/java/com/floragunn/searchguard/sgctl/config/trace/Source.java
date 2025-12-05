package com.floragunn.searchguard.sgctl.config.trace;

import java.util.LinkedList;
import java.util.List;

public sealed interface Source {

  Source.None NONE = new Source.None();

  String pathPart();

  default List<Source> path() {
    var path = new LinkedList<Source>();
    Source current = this;
    while (current instanceof Child child) {
      path.addFirst(current);
      current = child.parent();
    }
    path.addFirst(current);
    return path;
  }

  default String fullPathString() {
    var path = path();
    var sb = new StringBuilder();
    if (path.get(0) instanceof Config cfg) {
      sb.append(cfg.file());
      sb.append(": ");
      var pathParts = path.subList(1, path.size()).stream().map(Source::pathPart).toList();
      sb.append(String.join(".", pathParts));
    } else {
      var pathParts = path.stream().map(Source::pathPart).toList();
      sb.append(String.join(".", pathParts));
    }
    return sb.toString();
  }

  sealed interface Child extends Source {
    Source parent();
  }

  record None() implements Source {

    @Override
    public String pathPart() {
      return "";
    }
  }

  record Config(String file) implements Source {

    @Override
    public String pathPart() {
      return file;
    }
  }

  record Attribute(Source parent, String name) implements Child {

    @Override
    public String pathPart() {
      return name;
    }
  }

  record ListEntry(Source parent, int index) implements Child {

    @Override
    public String pathPart() {
      return String.valueOf(index);
    }
  }
}
