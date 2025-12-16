package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record SgInternalActionGroups(ImmutableMap<String, ActionGroup> actionGroups)
  implements NamedConfig<SgInternalActionGroups>{

  @Override
  public String getFileName() {
        return "sg_action_groups.yml";
  }

  public record ActionGroup(
    Optional<Boolean> reserved,
    Optional<String> description,
    String type,
    ImmutableList<String> allowed_actions) {

      public Map<String, Object> toBasicObject(){
        Map<String, Object> result = new HashMap<>();
        reserved.ifPresent(res -> result.put("reserved", res));
        description.ifPresent(desc -> result.put("description", desc));
        result.put("type", type);
        result.put("allowed_actions", allowed_actions);
        return result;
      }

    }
    @Override
    public Map<String, Object> toBasicObject(){
        Map<String, Object> result = new LinkedHashMap<>();
        actionGroups.forEach((k, v) -> result.put(k, v.toBasicObject()));

        return result;
    }

}
