package com.floragunn.searchguard.sgctl.util.mapping.writer;

import java.util.*;

import com.floragunn.codova.documents.Document;
import org.jspecify.annotations.NonNull;


public class ActionGroupConfigWriter implements Document<ActionGroupConfigWriter> {
    private final Set<ActionGroup> actionGroups = new HashSet<>();

    static final String FILE_NAME = "sg_action_groups.yml";

    void addActionGroup(CustomClusterActionGroup actionGroup) {
        this.actionGroups.add(actionGroup.toActionGroup());
    }

    // obviously a temporary solution
    void addActionGroup(CustomIndexActionGroup actionGroup) {
        this.actionGroups.add(actionGroup.toActionGroup());
    }

    @Override
    public Object toBasicObject() {
        var contents = new LinkedHashMap<String, ActionGroup>();
        for(var actionGroup : actionGroups){
            contents.put(actionGroup.name, actionGroup);
        }
        return contents;
    }

    static class ActionGroup implements Document<ActionGroup>{
        @NonNull String name;
        List<String> allowedActions;
        @NonNull String type; // must be "index", "cluster" or "kibana"
        String description;

        public ActionGroup(@NonNull String name, List<String> allowedActions, @NonNull String type, String description) {
            if (!type.matches("^(index)|(cluster)|(kibana)$")) throw new IllegalArgumentException();
            this.name = name;
            this.allowedActions = allowedActions;
            this.type = type;
            this.description = description;
        }

        @Override
        public Object toBasicObject() {
            var contents = new LinkedHashMap<String, Object>();
            contents.put("allowed_actions", allowedActions);
            contents.put("type", type);
            if (description != null && !description.isEmpty()) {
                contents.put("description", description);
            }
            return contents;
        }
    }

    enum CustomClusterActionGroup {

        // Might be cleaner to use static final patterns like in the elasticsearch repo
        SGS_CANCEL_TASK_CUSTOM(
            "SGS_CANCEL_TASK_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'cancel_task'",
            List.of(
            "cluster:admin/tasks/cancel*"
        )),
        SGS_CREATE_SNAPSHOT_CUSTOM(
            "SGS_CREATE_SNAPSHOT_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'create_snapshot'",
            List.of(
                "cluster:admin/snapshot/create",
                "cluster:admin/snapshot/status*",
                "cluster:admin/snapshot/get",
                "cluster:admin/snapshot/status",
                "cluster:admin/repository/get"
        )),
        SGS_GRANT_API_KEY_CUSTOM(
            "SGS_GRANT_API_KEY_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'grant_api_key'",
            List.of(
            "cluster:admin/xpack/security/api_key/grant*"
        )),
        SGS_MANAGE_CUSTOM(
            "SGS_MANAGE_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage'",
            List.of(
            "cluster:*",
            "indices:admin/template/*",
            "indices:admin/index_template/*",
            "cluster:admin/xpack/security/*"
        )),
        SGS_MANAGE_API_KEY_CUSTOM(
            "SGS_MANAGE_API_KEY_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_api_key'",
            List.of(
            "cluster:admin/xpack/security/api_key/*"
        )),
        SGS_MANAGE_AUTOSCALING_CUSTOM(
            "SGS_MANAGE_AUTOSCALING_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_autoscaling'",
            List.of(
            "cluster:admin/autoscaling/*"
        )),
        SGS_MANAGE_CCR_CUSTOM(
            "SGS_MANAGE_CCR_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_ccr'",
            List.of(
            "cluster:admin/xpack/ccr/*",
            "cluster:monitor/state",
            "cluster:admin/xpack/security/user/has_privileges"
        )),
        SGS_MANAGE_ENRICH_CUSTOM(
            "SGS_MANAGE_ENRICH_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_enrich'",
            List.of(
            "cluster:admin/xpack/enrich/*", 
            "cluster:monitor/xpack/enrich/*"
        )),
        SGS_MANAGE_INFERENCE_CUSTOM(
            "SGS_MANAGE_INFERENCE_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_inference'",
            List.of(
            "cluster:admin/xpack/inference/*",
            "cluster:monitor/xpack/inference*",
            "cluster:admin/xpack/ml/trained_models/deployment/start",
            "cluster:admin/xpack/ml/trained_models/deployment/stop",
            "cluster:monitor/xpack/ml/trained_models/deployment/infer"
        )),
        SGS_MANAGE_LOGSTASH_PIPELINES_CUSTOM(
            "SGS_MANAGE_LOGSTASH_PIPELINES_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'manage_logstash_pipelines'",
            List.of(
            "cluster:admin/logstash/pipeline/*"
        )),
        SGS_MANAGE_ML_CUSTOM(
            "SGS_MANAGE_ML_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'manage_ml'",
            List.of(
            "cluster:admin/xpack/ml/*", 
            "cluster:monitor/xpack/ml/*"
        )),
        SGS_MANAGE_OIDC_CUSTOM(
            "SGS_MANAGE_OIDC_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'manage_oidc'",
            List.of(
            "cluster:admin/xpack/security/oidc/*"
        )),
        SGS_MANAGE_OWN_API_KEY_CUSTOM(
            "SGS_MANAGE_OWN_API_KEY_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_own_api_key'",
            List.of(
            // TODO: Not sure, needs second look
            "cluster:admin/xpack/security/api_key/*"
        )),
        SGS_MANAGE_ROLLUP_CUSTOM(
            "SGS_MANAGE_ROLLUP_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_rollup'",
            List.of(
            "cluster:admin/xpack/rollup/*", 
            "cluster:monitor/xpack/rollup/*"
        )),
        SGS_MANAGE_SAML_CUSTOM(
            "SGS_MANAGE_SAML_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'manage_saml'",
            List.of(
            "cluster:admin/xpack/security/saml/*",
            "cluster:admin/xpack/security/token/invalidate",
            "cluster:admin/xpack/security/token/refresh",
            "cluster:monitor/xpack/security/saml/metadata"
        )),
        SGS_MANAGE_SEARCH_APPLICATION_CUSTOM(
            "SGS_MANAGE_SEARCH_APPLICATION_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_search_application'",
            List.of(
            "cluster:admin/xpack/application/search_application/*"
        )),
        SGS_MANAGE_SEARCH_QUERY_RULES_CUSTOM(
            "SGS_MANAGE_SEARCH_QUERY_RULES_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'manage_search_query_rules'",
            List.of(
            "cluster:admin/xpack/query_rules/*"
        )),
        SGS_MANAGE_SEARCH_SYNONYMS_CUSTOM(
            "SGS_MANAGE_SEARCH_SYNONYMS_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_search_synonyms'",
            List.of(
            "cluster:admin/synonyms/*",
            "cluster:admin/synonyms_sets/*",
            "cluster:admin/synonym_rules/*"           
        )),
        SGS_MANAGE_SECURITY_CUSTOM(
            "SGS_MANAGE_SECURITY_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_security'",
            List.of(
            "cluster:admin/xpack/security/*"    
        )),
        SGS_MANAGE_SERVICE_ACCOUNT_CUSTOM(
            "SGS_MANAGE_SERVICE_ACCOUNT_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_service_account'",
            List.of(
            "cluster:admin/xpack/security/service_account/*"
        )),
        SGS_MANAGE_TOKEN_CUSTOM(
            "SGS_MANAGE_TOKEN_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_token'",
            List.of(
            "cluster:admin/xpack/security/token/*"
        )),
        SGS_MANAGE_TRANSFORM_CUSTOM(
            "SGS_MANAGE_TRANSFORM_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_transform'",
            List.of(
            "cluster:admin/data_frame/*",
            "cluster:monitor/data_frame/*",
            "cluster:monitor/transform/*",
            "cluster:admin/transform/*"
        )),
        SGS_MANAGE_WATCHER_CUSTOM(
            "SGS_MANAGE_WATCHER_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'manage_watcher'",
            List.of(
            "cluster:admin/xpack/watcher/*", 
            "cluster:monitor/xpack/watcher/*"
        )),
        SGS_MONITOR_ENRICH_CUSTOM(
            "SGS_MONITOR_ENRICH_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_enrich'",
            List.of(
            "cluster:monitor/xpack/enrich/*",
            "cluster:admin/xpack/enrich/get"
        )),
        SGS_MONITOR_ESQL_CUSTOM(
            "SGS_MONITOR_ESQL_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_esql'",
            List.of(
            "cluster:monitor/xpack/esql/*"
        )),
        SGS_MONITOR_INFERENCE_CUSTOM(
            "SGS_MONITOR_INFERENCE_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_inference'",
            List.of(
            "cluster:monitor/xpack/inference*",
            "cluster:monitor/xpack/ml/trained_models/deployment/infer"
        )),
        SGS_MONITOR_ML_CUSTOM(
            "SGS_MONITOR_ML_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_ml'",
            List.of(
            "cluster:monitor/xpack/ml/*"
        )),
        SGS_MONITOR_ROLLUP_CUSTOM(
            "SGS_MONITOR_ROLLUP_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_rollup'",
            List.of(
            "cluster:monitor/xpack/rollup/*"
        )),
        SGS_MONITOR_SNAPSHOT_CUSTOM(
            "SGS_MONITOR_SNAPSHOT_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_snapshot'",
            List.of(
            "cluster:admin/snapshot/status*",
            "cluster:admin/snapshot/get",
            "cluster:admin/snapshot/status",
            "cluster:admin/repository/get"
        )),
        SGS_MONITOR_STATS_CUSTOM(
            "SGS_MONITOR_STATS_CUSTOM",
            "Derived from X-Pack Security builtin privilege 'monitor_stats'",
            List.of(
            "cluster:monitor/stats*"
        )),
        SGS_MONITOR_TEXT_STRUCTURE_CUSTOM(
            "SGS_MONITOR_TEXT_STRUCTURE_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_text_structure'",
            List.of(
            "cluster:monitor/text_structure/*"
        )),
        SGS_MONITOR_TRANSFORM_CUSTOM(
            "SGS_MONITOR_TRANSFORM_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_transform'",
            List.of(
            "cluster:monitor/data_frame/*",
            "cluster:monitor/transform/*"
        )),
        SGS_MONITOR_WATCHER_CUSTOM(
            "SGS_MONITOR_WATCHER_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'monitor_watcher'",
            List.of(
            "cluster:monitor/xpack/watcher/*"
        )),
        SGS_READ_CCR_CUSTOM(
            "SGS_READ_CCR_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'read_ccr'",
            List.of(
            "cluster:monitor/state",
            "cluster:admin/xpack/security/user/has_privileges"
        )),
        SGS_READ_ILM_CUSTOM(
            "SGS_READ_ILM_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'read_ilm'",
            List.of(
            "cluster:admin/ilm/get",
            "cluster:admin/ilm/operation_mode/get"
        )),
        SGS_READ_PIPELINE_CUSTOM(
            "SGS_READ_PIPELINE_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'read_pipeline'",
            List.of(
            "cluster:admin/ingest/pipeline/get",
            "cluster:admin/ingest/pipeline/simulate"
        )),
        SGS_READ_SECURITY_CUSTOM(
            "SGS_READ_SECURITY_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'read_security'",
            List.of(
            "cluster:admin/xpack/security/api_key/get",
            "cluster:admin/xpack/security/api_key/query",
            "cluster:admin/xpack/security/privilege/builtin/get",
            "cluster:admin/xpack/security/privilege/get",
            "cluster:admin/xpack/security/profile/get",
            "cluster:admin/xpack/security/profile/has_privileges",
            "cluster:admin/xpack/security/profile/suggest",
            "cluster:admin/xpack/security/role/get",
            "cluster:admin/xpack/security/role/query",
            "cluster:admin/xpack/security/role_mapping/get",
            "cluster:admin/xpack/security/service_account/get",
            "cluster:admin/xpack/security/service_account/credential/get*",
            "cluster:admin/xpack/security/user/get",
            "cluster:admin/xpack/security/user/query",
            "cluster:admin/xpack/security/user/list_privileges",
            "cluster:admin/xpack/security/user/has_privileges",
            "cluster:admin/xpack/security/settings/get"
        )),
        SGS_TRANSPORT_CLIENT_CUSTOM(
            "SGS_TRANSPORT_CLIENT_CUSTOM", 
            "Derived from X-Pack Security builtin privilege 'transport_client'",
            List.of(
            "cluster:monitor/nodes/liveness",
            "cluster:monitor/state"
        ));

        private static final String TYPE = "cluster";

        private final String name;
        private final List<String> pattern;
        private final String description;

        CustomClusterActionGroup(String name, String description, List<String> pattern) {
            this.name = name;
            this.description = description;
            this.pattern = pattern;
        } 
        
        public String getName() { return name; }
        public List<String> getPattern() { return pattern; }

        public static CustomClusterActionGroup fromESPrivilege(String name) throws IllegalArgumentException {
            return from("SGS_" + name.toUpperCase() + "_CUSTOM");
        }

        private static CustomClusterActionGroup from(String name) throws IllegalArgumentException {
            for (var group : CustomClusterActionGroup.values()) {
                if (group.name.equals(name)) return group;
            }
            throw new IllegalArgumentException(name);
        }

        public ActionGroup toActionGroup() {
            return new ActionGroup(name, pattern, TYPE, description);
        }
    }


    // TODO: Combine with above class to reduce redundancy. Maybe add a third abstract class or interface?
    enum CustomIndexActionGroup {

        SGS_CREATE_DOC_CUSTOM(
                "SGS_CREATE_DOC_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'create_doc'",
                List.of("indices:data/write/index",
                        "indices:data/write/index[*",
                        "indices:data/write/index:op_type/create",
                        "indices:data/write/bulk*",
                        "indices:data/write/simulate/bulk*",
                        "indices:data/write/otlp/*")),

        SGS_CREATE_INDEX_CUSTOM(
                "SGS_CREATE_INDEX_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'create_index'",
                List.of("indices:admin/create", "indices:admin/auto_create", "indices:admin/data_stream/create")),

        SGS_CROSS_CLUSTER_REPLICATION_CUSTOM(
                "SGS_CROSS_CLUSTER_REPLICATION_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'cross_cluster_replication'",
                List.of(
                        "indices:data/read/xpack/ccr/shard_changes*",
                        "indices:monitor/stats*",
                        "indices:admin/seq_no/add_retention_lease*",
                        "indices:admin/seq_no/renew_retention_lease*",
                        "indices:admin/seq_no/remove_retention_lease*")),

        SGS_CROSS_CLUSTER_REPLICATION_INTERNAL_CUSTOM(
                "SGS_CROSS_CLUSTER_REPLICATION_INTERNAL_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'cross_cluster_replication_internal'",
                List.of("indices:internal/admin/ccr/restore/session/clear*",
                        "indices:internal/admin/ccr/restore/file_chunk/get*",
                        "indices:internal/admin/ccr/restore/session/put*",
                        "internal:transport/proxy/indices:internal/admin/ccr/restore/session/clear*",
                        "internal:transport/proxy/indices:internal/admin/ccr/restore/file_chunk/get*")),

        SGS_DELETE_INDEX_CUSTOM(
                "SGS_DELETE_INDEX_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'delete_index'",
                List.of(
                        "indices:admin/delete",
                        "indices:admin/data_stream/delete")),

        SGS_MAINTENANCE_CUSTOM(
                "SGS_MAINTENANCE_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'maintenance'",
                List.of("indices:admin/refresh*",
                        "indices:admin/flush*",
                        "indices:admin/synced_flush",
                        "indices:admin/forcemerge*")),

        SGS_MANAGE_DATA_STREAM_LIFECYCLE_CUSTOM(
                "SGS_MANAGE_DATA_STREAM_LIFECYCLE_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'manage_data_stream_lifecycle'",
                List.of("indices:admin/data_stream/lifecycle/*")),

        SGS_MANAGE_FAILURE_STORE_CUSTOM(
                "SGS_MANAGE_FAILURE_STORE_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'manage_failure_store'",
                List.of(
                    "indices:monitor/*",
                    "indices:admin/*",
                    "indices:data/read/field_caps*",
                    "indices:data/read/xpack/rollup/get/index/caps*"
                )),

        SGS_MANAGE_FOLLOW_INDEX_CUSTOM(
                "SGS_MANAGE_FOLLOW_INDEX_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'manage_follow_index'",
                List.of(
                        "indices:admin/xpack/ccr/put_follow",
                        "indices:admin/xpack/ccr/unfollow",
                        "indices:admin/close*",
                        "indices:admin/data_stream/promote",
                        "indices:admin/rollover")),

        SGS_MANAGE_ILM_CUSTOM(
                "SGS_MANAGE_ILM_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'manage_ilm'",
                List.of("indices:admin/ilm/*")),

        SGS_MANAGE_LEADER_INDEX_CUSTOM(
                "SGS_MANAGE_LEADER_INDEX_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'manage_leader_index'",
                List.of("indices:admin/xpack/ccr/forget_follower*")),

        SGS_READ_CROSS_CLUSTER_CUSTOM(
                "SGS_READ_CROSS_CLUSTER_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'read_cross_cluster'",
                List.of(
                        "internal:transport/proxy/indices:data/read/*",
                        "indices:admin/shards/search_shards",
                        "indices:admin/search/search_shards",
                        "indices:admin/resolve/cluster",
                        "indices:data/read/esql",
                        "indices:data/read/esql/compute")),

        SGS_READ_FAILURE_STORE_CUSTOM(
                "SGS_READ_FAILURE_STORE_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'read_failure_store'",
                List.of("indices:data/read/*", "indices:admin/resolve/index")),

        SGS_VIEW_INDEX_METADATA_CUSTOM(
                "SGS_VIEW_INDEX_METADATA_CUSTOM",
                "Derived from X-Pack Security builtin privilege 'view_index_metadata'",
                List.of(
                        "indices:admin/aliases/get",
                        "indices:admin/get",
                        "indices:admin/mappings/fields/get*",
                        "indices:admin/mappings/get",
                        "indices:admin/shards/search_shards",
                        "indices:admin/search/search_shards",
                        "indices:admin/validate/query*",
                        "indices:monitor/settings/get",
                        "indices:admin/ilm/explain",
                        "indices:admin/data_stream/lifecycle/get",
                        "indices:admin/data_stream/lifecycle/explain",
                        "indices:admin/data_stream/get",
                        "indices:admin/resolve/index",
                        "indices:admin/resolve/cluster",
                        "indices:data/read/field_caps*",
                        "indices:data/read/xpack/rollup/get/index/caps*",
                        "indices:monitor/transform/checkpoint*",
                        "indices:monitor/get/metering/stats", // serverless only
                        "indices:admin/get/metering/stats"// serverless only
                ));

        private static final String TYPE = "index";

        private final String name;
        private final List<String> pattern;
        private final String description;

        CustomIndexActionGroup(String name, String description, List<String> pattern) {
            this.name = name;
            this.description = description;
            this.pattern = pattern;
        } 
        
        public String getName() { return name; }
        public List<String> getPattern() { return pattern; }

        public static CustomIndexActionGroup fromESPrivilege(String name) throws IllegalArgumentException {
            return from("SGS_" + name.toUpperCase() + "_CUSTOM");
        }

        private static CustomIndexActionGroup from(String name) throws IllegalArgumentException {
            for (var group : CustomIndexActionGroup.values()) {
                if (group.name.equals(name)) return group;
            }
            throw new IllegalArgumentException(name);
        }

        public ActionGroup toActionGroup() {
            return new ActionGroup(name, pattern, TYPE, description);
        }
    }
}
