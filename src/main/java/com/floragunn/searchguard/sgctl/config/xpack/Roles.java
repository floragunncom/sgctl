package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.OptTraceable;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;

public record Roles(Traceable<ImmutableMap<String, Traceable<Role>>> roles) {

  public static Roles parse(TraceableDocNode tDoc) {
    return new Roles(tDoc.asAttribute().asMapOf(Role::parse));
  }

  public record Role(
      Traceable<ImmutableList<Traceable<String>>> runAs,
      Traceable<ImmutableList<Traceable<String>>> cluster,
      OptTraceable<DocNode> global,
      Traceable<ImmutableList<Traceable<Index>>> indices,
      Traceable<ImmutableList<Traceable<Application>>> applications,
      Traceable<ImmutableList<Traceable<RemoteIndex>>> remoteIndices,
      Traceable<ImmutableList<Traceable<RemoteCluster>>> remoteCluster,
      Traceable<ImmutableMap<String, Traceable<String>>> metadata, // changed from Object to String
      OptTraceable<ImmutableMap<String, Traceable<String>>>
          transientMetadata, // changed from Object to String
      OptTraceable<String> description) {

    public static Role parse(TraceableDocNode tDoc) {

      var runAs = tDoc.get("run_as").required().asListOfStrings();
      var cluster = tDoc.get("cluster").required().asListOfStrings();
      var global = tDoc.get("global").asDocNode();

      // Parsed by other functions
      var indices = tDoc.get("indices").required().asListOf(Index::parse);
      var applications = tDoc.get("applications").required().asListOf(Application::parse);
      var remoteIndices =
          tDoc.get("remote_indices")
              .required()
              .asListOf(RemoteIndex::parse); // add empty list as default??
      var remoteCluster =
          tDoc.get("remote_cluster")
              .required()
              .asListOf(RemoteCluster::parse); // add empty list as default??

      // Metadata is required
      var metadata = tDoc.get("metadata").required().asMapOfStrings();
      // Is Optional, becomes an empty map if missing
      var transientMetadata =
          tDoc.get("transient_metadata").asMapOfStrings(); // add empty map as default??
      var description = tDoc.get("description").asString();

      // v.throwExceptionForPresentErrors();

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
      Traceable<ImmutableList<Traceable<String>>> names,
      Traceable<ImmutableList<Traceable<String>>> privileges,
      OptTraceable<DocNode> fieldSecurity,
      OptTraceable<String> query,
      Traceable<Boolean> allowRestrictedIndices) {
    public static Index parse(TraceableDocNode tDoc) {

      // Required keys
      var names = tDoc.get("names").required().asListOfStrings();
      var privileges = tDoc.get("privileges").required().asListOfStrings();

      var field_security = tDoc.get("field_security").asDocNode();
      var query = tDoc.get("query").asString();
      var allow_restricted_indices = tDoc.get("allow_restricted_indices").asBoolean(false);

      // v.throwExceptionForPresentErrors();

      return new Index(names, privileges, field_security, query, allow_restricted_indices);
    }
  }

  // Parser for Remote Indices
  public record RemoteIndex(
      Traceable<ImmutableList<Traceable<String>>> clusters,
      Traceable<ImmutableList<Traceable<String>>> names,
      Traceable<ImmutableList<Traceable<String>>> privileges,
      OptTraceable<DocNode> fieldSecurity,
      OptTraceable<String> query,
      Traceable<Boolean> allowRestrictedIndices) {

    public static RemoteIndex parse(TraceableDocNode tDoc) {
      // ValidationErrors errors = new ValidationErrors();
      // ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      var clusters = tDoc.get("clusters").required().asListOfStrings();
      var names = tDoc.get("names").required().asListOfStrings();
      var privileges = tDoc.get("privileges").required().asListOfStrings();

      var field_security = tDoc.get("field_security").asDocNode();
      var query = tDoc.get("query").asString();
      var allow_restricted_indices = tDoc.get("allow_restricted_indices").asBoolean(false);

      // v.throwExceptionForPresentErrors();
      return new RemoteIndex(
          clusters, names, privileges, field_security, query, allow_restricted_indices);
    }
  }

  // Parser for Applications
  public record Application(
      Traceable<String> application,
      Traceable<ImmutableList<Traceable<String>>> privileges,
      Traceable<ImmutableList<Traceable<String>>> resources) {

    public static Application parse(TraceableDocNode tDoc) {
      // ValidationErrors errors = new ValidationErrors();
      // ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      var application = tDoc.get("application").required().asString();
      var privileges = tDoc.get("privileges").required().asListOfStrings();
      var resources = tDoc.get("resources").required().asListOfStrings();

      // v.throwExceptionForPresentErrors();
      return new Application(application, privileges, resources);
    }
  }

  // Parser for Remote Clusters
  public record RemoteCluster(
      Traceable<ImmutableList<Traceable<String>>> clusters,
      Traceable<ImmutableList<Traceable<String>>> privileges) {

    public static RemoteCluster parse(TraceableDocNode tDoc) {
      // ValidationErrors errors = new ValidationErrors();
      // ValidatingDocNode v = new ValidatingDocNode(node, errors, context);

      var clusters = tDoc.get("clusters").required().asListOfStrings();
      var privileges = tDoc.get("privileges").required().asListOfStrings();

      // v.throwExceptionForPresentErrors();
      return new RemoteCluster(clusters, privileges);
    }
  }
}
