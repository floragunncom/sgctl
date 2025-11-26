package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.Optional;

public record Roles(ImmutableMap<String, Role> roles) {

  public static Roles parse(DocNode doc, Parser.Context context) throws ConfigValidationException {

    var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);

    var builder = new ImmutableMap.Builder<String, Role>(doc.size());
    // Iterating over Roles
    for (var name : doc.keySet()) {
      builder.with(name, vDoc.get(name).by(Role::parse));
    }

    vDoc.throwExceptionForPresentErrors();

    return new Roles(builder.build());
  }

  public record Role(
      ImmutableList<String> runAs,
      ImmutableList<String> cluster,
      Optional<DocNode> global,
      ImmutableList<Index> indices,
      ImmutableList<Application> applications,
      ImmutableList<RemoteIndex> remoteIndices,
      ImmutableList<RemoteCluster> remoteCluster,
      ImmutableMap<String, Object> metadata,
      ImmutableMap<String, Object> transientMetadata,
      Optional<String> description) {
    public static Role parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      // Required. If missing, an error will be registered
      if (!node.containsKey("cluster")) errors.add(new MissingAttribute("cluster", node));
      if (!node.containsKey("indices")) errors.add(new MissingAttribute("indices", node));
      if (!node.containsKey("applications")) errors.add(new MissingAttribute("applications", node));
      if (!node.containsKey("metadata")) errors.add(new MissingAttribute("metadata", node));

      Role role =
          new Role(
              v.get("run_as").asList().withEmptyListAsDefault().ofStrings(),
              node.containsKey("cluster")
                  ? v.get("cluster").asList().ofStrings()
                  : ImmutableList.empty(),
              Optional.ofNullable(node.containsKey("global") ? v.get("global").asDocNode() : null),

              // Parsed by other functions
              node.containsKey("indices")
                  ? v.get("indices").asList().ofObjectsParsedBy(Index::parse)
                  : ImmutableList.empty(),
              node.containsKey("applications")
                  ? v.get("applications").asList().ofObjectsParsedBy(Application::parse)
                  : ImmutableList.empty(),
              v.get("remote_indices")
                  .asList()
                  .withEmptyListAsDefault()
                  .ofObjectsParsedBy(RemoteIndex::parse),
              v.get("remote_cluster")
                  .asList()
                  .withEmptyListAsDefault()
                  .ofObjectsParsedBy(RemoteCluster::parse),

              // Metadata is required
              node.containsKey("metadata") ? v.get("metadata").asMap() : ImmutableMap.empty(),

              // Is Optional, becomes an empty map if missing
              node.containsKey("transient_metadata")
                  ? v.get("transient_metadata").asMap()
                  : ImmutableMap.empty(),
              Optional.ofNullable(
                  node.containsKey("description") ? v.get("description").asString() : null));

      v.throwExceptionForPresentErrors();
      return role;
    }
  }

  // Parser für Indices
  public record Index(
      ImmutableList<String> names,
      ImmutableList<String> privileges,
      Optional<DocNode> fieldSecurity,
      Optional<String> query,
      boolean allowRestrictedIndices) {
    public static Index parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      // Required keys
      if (!node.containsKey("names")) errors.add(new MissingAttribute("names", node));
      if (!node.containsKey("privileges")) errors.add(new MissingAttribute("privileges", node));

      Index index =
          new Index(
              node.containsKey("names")
                  ? v.get("names").asList().ofStrings()
                  : ImmutableList.empty(),
              node.containsKey("privileges")
                  ? v.get("privileges").asList().ofStrings()
                  : ImmutableList.empty(),
              Optional.ofNullable(
                  node.containsKey("field_security") ? v.get("field_security").asDocNode() : null),
              Optional.ofNullable(node.containsKey("query") ? v.get("query").asString() : null),
              node.containsKey("allow_restricted_indices")
                  ? v.get("allow_restricted_indices").asBoolean()
                  : false);
      v.throwExceptionForPresentErrors();
      return index;
    }
  }

  // Parser for Remote Indices
  public record RemoteIndex(
      ImmutableList<String> clusters,
      ImmutableList<String> names,
      ImmutableList<String> privileges,
      Optional<DocNode> fieldSecurity,
      Optional<String> query,
      boolean allowRestrictedIndices) {
    public static RemoteIndex parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      // Required keys
      if (!node.containsKey("clusters")) errors.add(new MissingAttribute("clusters", node));
      if (!node.containsKey("names")) errors.add(new MissingAttribute("names", node));
      if (!node.containsKey("privileges")) errors.add(new MissingAttribute("privileges", node));

      RemoteIndex remoteindex =
          new RemoteIndex(
              node.containsKey("clusters")
                  ? v.get("clusters").asList().ofStrings()
                  : ImmutableList.empty(),
              node.containsKey("names")
                  ? v.get("names").asList().ofStrings()
                  : ImmutableList.empty(),
              node.containsKey("privileges")
                  ? v.get("privileges").asList().ofStrings()
                  : ImmutableList.empty(),
              Optional.ofNullable(
                  node.containsKey("field_security") ? v.get("field_security").asDocNode() : null),
              Optional.ofNullable(node.containsKey("query") ? v.get("query").asString() : null),
              node.containsKey("allow_restricted_indices")
                  ? v.get("allow_restricted_indices").asBoolean()
                  : false);
      v.throwExceptionForPresentErrors();
      return remoteindex;
    }
  }

  // Parser for Applications
  public record Application(
      String application, ImmutableList<String> privileges, ImmutableList<String> resources) {
    public static Application parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      // Required keys
      if (!node.containsKey("application")) errors.add(new MissingAttribute("application", node));
      if (!node.containsKey("privileges")) errors.add(new MissingAttribute("privileges", node));
      if (!node.containsKey("resources")) errors.add(new MissingAttribute("resources", node));

      Application application =
          new Application(
              node.containsKey("application") ? v.get("application").asString() : "",
              node.containsKey("privileges")
                  ? v.get("privileges").asList().ofStrings()
                  : ImmutableList.empty(),
              node.containsKey("resources")
                  ? v.get("resources").asList().ofStrings()
                  : ImmutableList.empty());
      v.throwExceptionForPresentErrors();
      return application;
    }
  }

  // Parser for Remote Clusters
  public record RemoteCluster(ImmutableList<String> clusters, ImmutableList<String> privileges) {
    public static RemoteCluster parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      RemoteCluster remoteCluster =
          new RemoteCluster(
              v.get("clusters").asList().withEmptyListAsDefault().ofStrings(),
              v.get("privileges").asList().withEmptyListAsDefault().ofStrings());
      v.throwExceptionForPresentErrors();
      return remoteCluster;
    }
  }
}
