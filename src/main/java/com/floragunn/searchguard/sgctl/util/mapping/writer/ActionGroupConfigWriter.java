package com.floragunn.searchguard.sgctl.util.mapping.writer;

import java.util.LinkedHashMap;
import java.util.List;

import com.floragunn.codova.documents.Document;

public class ActionGroupConfigWriter implements Document<ActionGroupConfigWriter> {
    List<ActionGroup> actionGroups;

    public void addCustomActionGroup(String name, String type, String description, String[] allowedActions){
        ActionGroup ag = new ActionGroup(name, List.of(allowedActions), type, description);
        actionGroups.add(ag);
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
        String name;
        List<String> allowedActions;
        String type;
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
