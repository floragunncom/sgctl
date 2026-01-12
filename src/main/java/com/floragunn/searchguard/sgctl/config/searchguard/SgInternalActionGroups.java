package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;

import java.util.*;

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


      public ActionGroup{
          Objects.requireNonNull(type, "type must not be null");
          Objects.requireNonNull(allowed_actions, "allowed actions must not be null");
      }


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
