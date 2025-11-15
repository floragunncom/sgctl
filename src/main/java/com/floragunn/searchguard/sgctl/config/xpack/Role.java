package com.floragunn.searchguard.sgctl.config.xpack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

// TODO: To look at: non-absent @JsonInclude is a bit arbitrary and based on what elasticsearch outputs in its role API
// ref https://www.elastic.co/docs/deploy-manage/users-roles/cluster-or-deployment-auth/role-structure
public record Role(
    @JsonInclude(Include.NON_ABSENT)
    @JsonProperty("run_as")
    List<String> runAs,
    
    @JsonInclude(Include.NON_ABSENT)
    List<String> cluster,

    JsonNode global, // TODO, unsure of contents
    
    @JsonInclude(Include.NON_ABSENT)
    List<Index> indices,
    
    @JsonInclude(Include.NON_ABSENT)
    List<Application> applications,
    
    @JsonProperty("remote_indices")
    List<Index> remoteIndices,
    
    @JsonProperty("remote_cluster")
    List<RemoteCluster> remoteCluster,

    @JsonInclude(Include.NON_ABSENT)
    JsonNode metadata,

    @JsonInclude(Include.NON_ABSENT)
    @JsonProperty("transient_metadata")
    JsonNode transientMetadata,
    
    String description
) {

    @JsonInclude(Include.NON_EMPTY)
    public record Application(
        String application,

        List<String> privileges,

        List<String> resources
    ) {
    }

    public record Index(
        List<String> names,
        
        List<String> privileges,
        
        @JsonProperty("allow_restricted_indices")
        boolean allowRestrictedIndices,
        
        List<String> clusters // only for remote indices
    ) {
    }

    public record RemoteCluster(
        List<String> privileges,
        List<String> clusters
    ) {
    }

} 
