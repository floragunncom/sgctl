package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class RoleConfigWriter implements Document<RoleConfigWriter> {
    private IntermediateRepresentation ir;
    private MigrationReport report;
    private List<SGRole> roles;

    private static final String FILE_NAME = "sg_roles.yml";

    public RoleConfigWriter(IntermediateRepresentation ir) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        this.roles = new ArrayList<>();
        createSGRoles();
    }

    public void createSGRoles() {
        for (var role : ir.getRoles()) {
            var name = role.getName();
            var description = role.getDescription();
            var clusterPermissions = toSGClusterPrivileges(role);
            var indexPermissions = toSGClusterPrivileges(role);
        }
    }

    public List<String> toSGClusterPrivileges(Role role) {
        var privileges = role.getCluster();
        var sgPrivileges = new ArrayList<String>() ;

        for (var privilege : privileges) {
            switch (privilege) {
                case "all":
                    break;
                case "cancel_task":
                    break;
                case "create_snapshot":
                    break;
                case "cross_cluster_replication": // Note: This privilege must not be directly granted. It is used internally by Create Cross-Cluster API key and Update Cross-Cluster API key to manage cross-cluster API keys.
                    break;
                case "cross_cluster_search": // Note: This privilege must not be directly granted. It is used internally by Create Cross-Cluster API key and Update Cross-Cluster API key to manage cross-cluster API keys.
                    break;
                case "grant_api_key":
                    break;
                case "manage":
                    break;
                case "manage_api_key":
                    break;
                case "manage_autoscaling":
                    break;
                case "manage_ccr":
                    break;
                case "manage_data_frame_transforms": // Deprecated 7.5.0
                    break;
                case "manage_data_stream_global_retention": // Deprecated 8.16.0
                    break;
                case "manage_enrich":
                    break;
                case "manage_ilm":
                    break;
                case "manage_index_templates":
                    break;
                case "manage_inference":
                    break;
                case "manage_ingest_pipelines":
                    break;
                case "manage_logstash_pipelines":
                    break;
                case "manage_ml":
                    break;
                case "manage_oidc":
                    break;
                case "manage_own_api_key":
                    break;
                case "manage_pipeline":
                    break;
                case "manage_rollup":
                    break;
                case "manage_saml":
                    break;
                case "manage_search_application":
                    break;
                case "manage_search_query_rules":
                    break;
                case "manage_search_synonyms":
                    break;
                case "manage_security":
                    break;
                case "manage_service_account":
                    break;
                case "manage_slm":
                    break;
                case "manage_token":
                    break;
                case "manage_transform":
                    break;
                case "manage_watcher":
                    break;
                case "monitor":
                    break;
                case "monitor_data_stream_global_retention": // Deprecated 8.16.0
                    break;
                case "monitor_enrich":
                    break;
                case "monitor_esql":
                    break;
                case "monitor_inference":
                    break;
                case "monitor_ml":
                    break;
                case "monitor_rollup":
                    break;
                case "monitor_snapshot":
                    break;
                case "monitor_stats":
                    break;
                case "monitor_text_structure":
                    break;
                case "monitor_transform":
                    break;
                case "monitor_watcher":
                    break;
                case "read_ccr":
                    break;
                case "read_ilm":
                    break;
                case "read_pipeline":
                    break;
                case "read_slm":
                    break;
                case "read_security":
                    break;
                case "transport_client":
                    break;
                default:
                    report.addManualAction(FILE_NAME, role.getName() + "->cluster_permissions", "The privilege: " + privilege + " is unknown and can not be automatically mapped");
                    break;
            }
        }
        return sgPrivileges;
    }

    public List<String> toSGIndexPrivileges(List<String> privileges, Role role) {
        var sgPrivileges = new ArrayList<String>() ;
        for (var privilege : privileges) {
            switch (privilege) {
                case "all":
                    break;
                case "create":
                    break;
                case "create_doc":
                    break;
                case "create_index":
                    break;
                case "cross_cluster_replication":
                    break;
                case "cross_cluster_replication_internal":
                    break;
                case "delete":
                    break;
                case "delete_index":
                    break;
                case "index":
                    break;
                case "maintenance":
                    break;
                case "manage":
                    break;
                case "manage_data_stream_lifecycle":
                    break;
                case "manage_failure_store":
                    break;
                case "manage_follow_index":
                    break;
                case "manage_ilm":
                    break;
                case "manage_leader_index":
                    break;
                case "monitor":
                    break;
                case "read":
                    break;
                case "read_cross_cluster":
                    break;
                case "read_failure_store":
                    break;
                case "view_index_metadata":
                    break;
                case "write":
                    break;
                default:

                    break;
            }
        }
        return sgPrivileges;
    }

    @Override
    public Object toBasicObject() {
        var contents = new LinkedHashMap<String, SGRole>();
        for (var role : roles) {
            contents.put(role.name, role);
        }
        return contents;
    }

    static class SGRole implements Document<SGRole> {
        String name;
        String description;

        @Override
        public Object toBasicObject() {
            return null;
        }
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
