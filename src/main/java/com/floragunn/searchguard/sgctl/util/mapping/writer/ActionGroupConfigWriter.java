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

    public void addCustomActionGroup(String name, String type, String description, String[] allowedActions){
        ActionGroup ag = new ActionGroup(name, List.of(allowedActions), type, description);
        // possibly write to report here?
        actionGroups.add(ag);
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
        
    }
}
