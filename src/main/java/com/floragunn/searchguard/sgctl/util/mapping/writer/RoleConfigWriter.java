package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;

import java.util.*;

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
        print(DocWriter.yaml().writeAsString(this));
    }

    private void createSGRoles() {
        for (var role : ir.getRoles()) {
            var name = role.getName();
            var description = role.getDescription();
            var clusterPermissions = toSGClusterPrivileges(role);
            var index = toSGIndices(role);
            var sgRole = new SGRole(name, description, clusterPermissions, index);
            roles.add(sgRole);
        }
    }

    private List<SGRole.SGIndex> toSGIndices(Role role) {
        List<SGRole.SGIndex> sgIndices = new ArrayList<>();
        for (var index : role.getIndices()) {
            var indexPatterns = index.getNames();
            var indexPermissions = toSGIndexPrivileges(index.getPrivileges(), role);
            var fls = toSGFLS(index, role);
            var dls = toSGDLS(index, role);
            var sgIndex = new SGRole.SGIndex(indexPatterns, indexPermissions, dls, fls);
            sgIndices.add(sgIndex);
        }
        return sgIndices;
    }

    private List<String> toSGFLS(Role.Index index, Role role) {
        // TODO: Check whether there is a restriction on the ~
        if (index.getFieldSecurity() == null) {
            return Collections.emptyList();
        }
        var fls = index.getFieldSecurity().getGrant();
        for (var field : fls) {
            if (field.isEmpty()) continue;
            if (field.charAt(0) == '~') {
                report.addManualAction(FILE_NAME,
                        role.getName() + "->index->field_security->" + field,
                        "There is a '~' at the start of a field security key. In SG this is used to mark an exclusion.");
                fls.remove(field);
            }
        }
        if (index.getFieldSecurity().getExcept() == null) return fls;
        for (var except : index.getFieldSecurity().getExcept()) {
            fls.add("~" + except);
        }
        return fls;
    }

    private String toSGDLS(Role.Index index, Role role) {
        var query = index.getQuery();
        if (query == null) {
            return null;
        }

        return query;
    }

    private List<String> toSGClusterPrivileges(Role role) {
        var privileges = role.getCluster();
        var sgPrivileges = new ArrayList<String>() ;

        for (var privilege : privileges) {
            switch (privilege) {
                case "all":
                    sgPrivileges.add("SGS_CLUSTER_ALL");
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
                    sgPrivileges.add("SGS_CLUSTER_MANAGE_ILM");
                    break;
                case "manage_index_templates":
                    sgPrivileges.add("SGS_CLUSTER_MANAGE_INDEX_TEMPLATES");
                    break;
                case "manage_inference":
                    break;
                case "manage_ingest_pipelines":
                    sgPrivileges.add("SGS_CLUSTER_MANAGE_PIPELINES");
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
                    sgPrivileges.add("SGS_CLUSTER_MANAGE_PIPELINES");
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
                    sgPrivileges.add("SGS_CLUSTER_MONITOR");
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
                    report.addManualAction(FILE_NAME, role.getName() + "->cluster_permissions", "The privilege: " + privilege + " is unknown and can not be automatically mapped.");
                    break;
            }
        }
        return sgPrivileges;
    }

    private List<String> toSGIndexPrivileges(List<String> privileges, Role role) {
        var sgPrivileges = new ArrayList<String>() ;
        for (var privilege : privileges) {
            switch (privilege) {
                case "all":
                    sgPrivileges.add("SGS_INDICES_ALL");
                    break;
                case "create":
                    sgPrivileges.add("SGS_CREATE_INDEX");
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
                    sgPrivileges.add("SGS_DELETE");
                    break;
                case "delete_index":
                    break;
                case "index", "write":
                    sgPrivileges.add("SGS_WRITE");
                    break;
                case "maintenance":
                    break;
                case "manage":
                    sgPrivileges.add("SGS_MANAGE");
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
                    sgPrivileges.add("SGS_INDICES_MONITOR");
                    break;
                case "read":
                    sgPrivileges.add("SGS_READ");
                    break;
                case "read_cross_cluster":
                    break;
                case "read_failure_store":
                    break;
                case "view_index_metadata":
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
        List<String> clusterPermissions;
        List<SGIndex> index;

        public SGRole(String name, String description, List<String> clusterPermissions, List<SGIndex> index) {
            this.name = name;
            this.description = description;
            this.clusterPermissions = clusterPermissions;
            this.index = index;
        }

        @Override
        public Object toBasicObject() {
            var contents = new LinkedHashMap<String, Object>();
            if (description != null) {
                contents.put("description", description);
            }
            contents.put("cluster_permissions", clusterPermissions);
            contents.put("index_permissions", index);
            return contents;
        }

        static class SGIndex implements Document<SGIndex> {
            List<String> indexPatterns;
            List<String> allowedActions;
            String dls;
            List<String> fls;

            public SGIndex(List<String> indexPatterns, List<String> allowedActions, String dls, List<String> fls) {
                this.indexPatterns = indexPatterns;
                this.allowedActions = allowedActions;
                this.dls = dls;
                this.fls = fls;
            }

            @Override
            public Object toBasicObject() {
                var contents = new LinkedHashMap<String, Object>();
                contents.put("index_patterns", indexPatterns);
                contents.put("allowed_actions", allowedActions);
                if (dls != null) {
                    contents.put("_dls_", dls);
                }
                if (fls != null && !fls.isEmpty()) {
                    contents.put("_fls_", fls);
                }
                return contents;
            }
        }
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static void printErr(Object line) {
        System.err.println(line);
    }
}
