package com.floragunn.searchguard.sgctl.util.mapping.writer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

public class ActionGroupConfigWriter implements Document<ActionGroupConfigWriter> {
    private List<ActionGroup> actionGroups;
    private IntermediateRepresentation ir;
    private MigrationReport report;

    private static final String FILE_NAME = "sg_action_groups.yml";


    public ActionGroupConfigWriter(IntermediateRepresentation ir) {
        this.actionGroups = new ArrayList<>();
        this.ir = ir;
        this.report = MigrationReport.shared;

        // Should action groups be inititialized with all possible custom groups, or should they only be added when needed in RoleConfigWriter?
        print(DocWriter.yaml().writeAsString(this));

    }

    public void addCustomActionGroups(Set<CustomClusterActionGroup> agSet ) {

        for (var ag : agSet) {
            this.actionGroups.add(ActionGroup.fromCustomClusterActionGroup(ag));
        }

    }

    public boolean contains(String name){
        if (actionGroups == null) return false;
        for (var ag : actionGroups){
            if (Objects.equals(ag.name, name)) return true;
        }
        return false;
    }

    @Override
    public Object toBasicObject() {
        var contents = new LinkedHashMap<String, ActionGroup>();
        for(var actionGroup : actionGroups){
            contents.put(actionGroup.name, actionGroup);
        }
        return contents;
    }

    static void print(Object line) {
        System.out.println(line);
    }

    static class ActionGroup implements Document<ActionGroup>{
        String name;
        List<String> allowedActions;
        String type; // must be "index", "cluster" or "kibana" 
        String description;

        public ActionGroup(String name, List<String> allowedActions, String type, String description) {
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

        static ActionGroup fromCustomClusterActionGroup(CustomClusterActionGroup cag) {
            var desc = "todo";
            return new ActionGroup(cag.getName(), cag.getPattern(), "cluster", desc);
        }

    }

    // TODO: better integration with above ActionGroup class, current implementation seems redundant
    enum CustomClusterActionGroup {

        // Might be cleaner to use static final patterns like in the elasticsearch repo
        SGS_MANAGE_SECURITY_CUSTOM("SGS_MANAGE_SECURITY_CUSTOM", List.of(
            "cluster:admin/xpack/security/*"    
        )),
        SGS_MANAGE_SERVICE_ACCOUNT_CUSTOM("SGS_MANAGE_SERVICE_ACCOUNT_CUSTOM", List.of(
            "cluster:admin/xpack/security/service_account/*"
        )),
        SGS_MANAGE_TOKEN_CUSTOM("SGS_MANAGE_TOKEN_CUSTOM", List.of(
            "cluster:admin/xpack/security/token/*"
        )),
        SGS_MANAGE_TRANSFORM_CUSTOM("SGS_MANAGE_TRANSFORM_CUSTOM", List.of(
            "cluster:admin/data_frame/*",
            "cluster:monitor/data_frame/*",
            "cluster:monitor/transform/*",
            "cluster:admin/transform/*"
        )),
        SGS_MANAGE_WATCHER_CUSTOM("SGS_MANAGE_WATCHER_CUSTOM", List.of(
            "cluster:admin/xpack/watcher/*", 
            "cluster:monitor/xpack/watcher/*"
        )),
        SGS_MONITOR_ENRICH_CUSTOM("SGS_MONITOR_ENRICH_CUSTOM", List.of(
            "cluster:monitor/xpack/enrich/*",
            "cluster:admin/xpack/enrich/get"
        )),
        SGS_MONITOR_ESQL_CUSTOM("SGS_MONITOR_ESQL_CUSTOM", List.of(
            "cluster:monitor/xpack/esql/*"
        )),
        SGS_MONITOR_INFERENCE_CUSTOM("SGS_MONITOR_INFERENCE_CUSTOM", List.of(
            "cluster:monitor/xpack/inference*",
            "cluster:monitor/xpack/ml/trained_models/deployment/infer"
        )),
        SGS_MONITOR_ML_CUSTOM("SGS_MONITOR_ML_CUSTOM", List.of(
            "cluster:monitor/xpack/ml/*"
        )),
        SGS_MONITOR_ROLLUP_CUSTOM("SGS_MONITOR_ROLLUP_CUSTOM", List.of(
            "cluster:monitor/xpack/rollup/*"
        )),
        SGS_MONITOR_SNAPSHOT_CUSTOM("SGS_MONITOR_SNAPSHOT_CUSTOM", List.of(
            "cluster:admin/snapshot/status*",
            "cluster:admin/snapshot/get",
            "cluster:admin/snapshot/status",
            "cluster:admin/repository/get"
        )),
        SGS_MONITOR_STATS_CUSTOM("SGS_MONITOR_STATS_CUSTOM", List.of(
            "cluster:monitor/stats*"
        )),
        SGS_MONITOR_TEXT_STRUCTURE_CUSTOM("SGS_MONITOR_TEXT_STRUCTURE_CUSTOM", List.of(
            "cluster:monitor/text_structure/*"
        )),
        SGS_MONITOR_TRANSFORM_CUSTOM("SGS_MONITOR_TRANSFORM_CUSTOM", List.of(
            "cluster:monitor/data_frame/*",
            "cluster:monitor/transform/*"
        )),
        SGS_MONITOR_WATCHER_CUSTOM("SGS_MONITOR_WATCHER_CUSTOM", List.of(
            "cluster:monitor/xpack/watcher/*"
        )),
        SGS_READ_CCR_CUSTOM("SGS_READ_CCR_CUSTOM", List.of(
            "cluster:monitor/state",
            "cluster:admin/xpack/security/user/has_privileges"
        )),
        SGS_READ_ILM_CUSTOM("SGS_READ_ILM_CUSTOM", List.of(
            "cluster:admin/ilm/get",
            "cluster:admin/ilm/operation_mode/get"
        )),
        SGS_READ_PIPELINE_CUSTOM("SGS_READ_PIPELINE_CUSTOM", List.of(
            "cluster:admin/ingest/pipeline/get",
            "cluster:admin/ingest/pipeline/simulate"
        )),
        SGS_READ_SECURITY_CUSTOM("SGS_READ_SECURITY_CUSTOM", List.of(
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
        SGS_TRANSPORT_CLIENT_CUSTOM("SGS_TRANSPORT_CLIENT_CUSTOM", List.of(
            "cluster:monitor/nodes/liveness",
            "cluster:monitor/state"
        ));

        private final String name;
        private final List<String> pattern;

        CustomClusterActionGroup(String name, List<String> pattern) {
            this.name = name;
            this.pattern = pattern;
        } 
        
        public String getName() {
            return name;
        } 
        public List<String> getPattern() {
            return pattern;
        }
 
        public static CustomClusterActionGroup from(String name) {
            for (CustomClusterActionGroup group : CustomClusterActionGroup.values()) {
                if (group.name.equals(name)) {
                    return group;
                }
            }
            return SGS_MANAGE_SECURITY_CUSTOM;
        }
    }
}
