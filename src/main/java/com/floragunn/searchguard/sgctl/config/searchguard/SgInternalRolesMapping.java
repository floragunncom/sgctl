package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Arrays;

public record SgInternalRolesMapping(ImmutableMap<String, RoleMapping> mappings)
    implements NamedConfig<SgInternalRolesMapping>{

    @Override
    public String getFileName() {
        return "sg_roles_mapping.yml";
    }

    //fields in this record may be empty, eg if the RoleMapping is configured only for specific backend_roles
    //and user names aren't used for applying this role
    //fill with empty String "" if no rule was configured or found that fit
    public record RoleMapping(
            ImmutableList<String> users,
            ImmutableList<String> backend_roles,
            ImmutableList<String> hosts,
            ImmutableList<String> ips
    )  {
        public RoleMapping {
            Objects.requireNonNull(users, "users must not be NULL");
            Objects.requireNonNull(backend_roles, "backend_roles must not be NULL");
            Objects.requireNonNull(hosts, "hosts must not be NULL");
            Objects.requireNonNull(ips, "ips must not be NULL");
        }

        private static List<String> toBaseList(ImmutableList<String> list){
            List<String> result = new ArrayList<>();
            list.forEach((e) -> result.add(e));
           return result;
        }

        public Map<String, Object> toBasicObject(){
            Map<String, Object> result = new LinkedHashMap<>();
            if(!users.isEmpty()){
                result.put("users", toBaseList(users));
            }

            if(!backend_roles.isEmpty()){
                result.put("backend_roles", toBaseList(backend_roles));
            }

            if(!hosts.isEmpty()){
                result.put("hosts", toBaseList(hosts));
            }

            if(!ips.isEmpty()){
                result.put("ips", toBaseList(ips));
            }
            return result;
        }

    }

    @Override
    public Object toBasicObject(){
        Map<String, Object> result = new LinkedHashMap<>();
        mappings.forEach((k, v) -> result.put(k, v.toBasicObject()));

        return result;
    }

}



