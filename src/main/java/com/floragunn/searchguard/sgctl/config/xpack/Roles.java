package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
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

      var runAs = v.get("run_as").asList().withEmptyListAsDefault().ofStrings();
      var cluster = v.get("cluster").required().asList().ofStrings();
      var global =
          Optional.ofNullable(node.containsKey("global") ? v.get("global").asDocNode() : null);

      // Parsed by other functions
      var indices = v.get("indices").required().asList().ofObjectsParsedBy(Index::parse);
      var applications =
          v.get("applications").required().asList().ofObjectsParsedBy(Application::parse);
      var remoteIndices =
          v.get("remote_indices")
              .asList()
              .withEmptyListAsDefault()
              .ofObjectsParsedBy(RemoteIndex::parse);
      var remoteCluster =
          v.get("remote_cluster")
              .asList()
              .withEmptyListAsDefault()
              .ofObjectsParsedBy(RemoteCluster::parse);

      // Metadata is required
      var metadata = v.get("metadata").asMap();
      // Is Optional, becomes an empty map if missing
      var transientMetadata =
          node.containsKey("transient_metadata")
              ? v.get("transient_metadata").asMap()
              : ImmutableMap.<String, Object>empty();
      var description =
          Optional.ofNullable(
              node.containsKey("description") ? v.get("description").asString() : null);

      v.throwExceptionForPresentErrors();

      return new Role(
          runAs,
          cluster,
          global,
          indices,
          applications,
          remoteIndices,
          remoteCluster,
          metadata,
          transientMetadata,
          description);
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
      var names = v.get("names").required().asList().ofStrings();
      var privileges = v.get("privileges").required().asList().ofStrings();

      var field_security =
          Optional.ofNullable(
              node.containsKey("field_security") ? v.get("field_security").asDocNode() : null);
      var query = Optional.ofNullable(node.containsKey("query") ? v.get("query").asString() : null);
      var allow_restricted_indices =
          v.get("allow_restricted_indices").withDefault(false).asBoolean();

      v.throwExceptionForPresentErrors();

      return new Index(names, privileges, field_security, query, allow_restricted_indices);
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

      var clusters = v.get("clusters").required().asList().ofStrings();
      var names = v.get("names").required().asList().ofStrings();
      var privileges = v.get("privileges").required().asList().ofStrings();

      var field_security =
          Optional.ofNullable(
              node.containsKey("field_security") ? v.get("field_security").asDocNode() : null);
      var query = Optional.ofNullable(node.containsKey("query") ? v.get("query").asString() : null);
      var allow_restricted_indices =
          (node.containsKey("allow_restricted_indices")
              ? v.get("allow_restricted_indices").asBoolean()
              : false);

      v.throwExceptionForPresentErrors();
      return new RemoteIndex(
          clusters, names, privileges, field_security, query, allow_restricted_indices);
    }
  }

  // Parser for Applications
  public record Application(
      String application, ImmutableList<String> privileges, ImmutableList<String> resources) {
    public static Application parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      var application = v.get("application").required().asString();
      var privileges = v.get("privileges").required().asList().ofStrings();
      var resources = v.get("resources").required().asList().ofStrings();

      v.throwExceptionForPresentErrors();
      return new Application(application, privileges, resources);
    }
  }

  // Parser for Remote Clusters
  public record RemoteCluster(ImmutableList<String> clusters, ImmutableList<String> privileges) {
    public static RemoteCluster parse(DocNode node, Parser.Context context)
        throws ConfigValidationException {
      ValidationErrors errors = new ValidationErrors();
      ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      var clusters = v.get("clusters").required().asList().ofStrings();
      var privileges = v.get("privileges").required().asList().ofStrings();

      v.throwExceptionForPresentErrors();
      return new RemoteCluster(clusters, privileges);
    }
  }
}
