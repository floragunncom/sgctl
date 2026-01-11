package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.*;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;
import com.floragunn.searchguard.sgctl.util.mapping.ir.security.Role;
import com.floragunn.searchguard.sgctl.util.mapping.writer.ActionGroupConfigWriter.CustomClusterActionGroup;
import com.floragunn.searchguard.sgctl.util.mapping.writer.ActionGroupConfigWriter.CustomIndexActionGroup;
import com.sun.jdi.InvalidTypeException;
import org.jspecify.annotations.NonNull;

import java.security.InvalidKeyException;
import java.util.*;

/**
 * Writes Search Guard role definitions derived from the intermediate representation.
 */
public class RoleConfigWriter implements Document<RoleConfigWriter> {
    final private IntermediateRepresentation ir;
    final private MigrationReport report;
    final private List<SGRole> roles;
    final private MigrateConfig.SgAuthc sgAuthc;
    final private ActionGroupConfigWriter agWriter;
    final private Set<String> userMappingAttributes = new HashSet<>();

    static final String FILE_NAME = "sg_roles.yml";
    static final Set<String> noEquivalentClusterActionGroupKeys = Set.of(
            "cancel_task", "create_snapshot", "cross_cluster_replication", "cross_cluster_search", "grant_api_key", "manage", "manage_api_key",
            "manage_autoscaling", "manage_ccr", "manage_data_frame_transforms", "manage_data_stream_global_retention", "manage_enrich", "manage_inference",
            "manage_logstash_pipelines", "manage_ml", "manage_oidc", "manage_own_api_key", "manage_rollup", "manage_saml", "manage_search_application",
            "manage_search_query_rules", "manage_search_synonyms", "manage_security", "manage_service_account", "manage_slm", "manage_token", "manage_transform",
            "manage_watcher", "monitor_data_stream_global_retention", "monitor_enrich", "monitor_esql", "monitor_inference", "monitor_ml", "monitor_rollup",
            "monitor_snapshot", "monitor_stats", "monitor_text_structure", "monitor_transform", "monitor_watcher", "read_ccr", "read_pipeline", "read_slm",
            "read_security", "transport_client"
            );
            static final Set<String> noEquivalentIndexActionGroupKeys = Set.of(
                "create_doc",
                "create_index",
                "cross_cluster_replication",
                "cross_cluster_replication_internal",
                "delete_index",
                "maintenance",
                "manage_data_stream_lifecycle",
                "manage_failure_store",
                "manage_follow_index",
                "manage_ilm",
                "manage_leader_index",
                "read_cross_cluster",
                "read_failure_store",
                "view_index_metadata"
        );

    private static final Set<String> validQueryKeys = Set.of(
            // Full-text queries https://www.elastic.co/docs/reference/query-languages/query-dsl/full-text-queries
            "match", "match_phrase", "match_phrase_prefix", "match_bool_prefix", "multi_match", "intervals", "query_string", "simple_query_string",
            // Term-level queries https://www.elastic.co/docs/reference/query-languages/query-dsl/term-level-queries
            "term", "terms", "terms_set", "range", "exists", "prefix", "wildcard", "regexp", "fuzzy", "ids",
            // Compound queries https://www.elastic.co/docs/reference/query-languages/query-dsl/compound-queries
            "bool", "boosting", "constant_score", "dis_max", "function_score",
            // Joining queries https://www.elastic.co/docs/reference/query-languages/query-dsl/joining-queries
            "nested", "has_child", "has_parent", "parent_id",
            // Geo queries https://www.elastic.co/docs/reference/query-languages/query-dsl/geo-queries
            "geo_bounding_box", "geo_distance", "geo_grid", "geo_polygon", "geo_shape",
            // Vector queries https://www.elastic.co/docs/reference/query-languages/query-dsl/vector-queries
            "knn", "sparse_vector", "semantic",
            // Specialized https://www.elastic.co/docs/reference/query-languages/query-dsl/specialized-queries
            "distance_feature", "more_like_this", "percolate", "rank_feature", "script", "script_score", "wrapper", "pinned", "rule",
            // Span queries https://www.elastic.co/docs/reference/query-languages/query-dsl/span-queries
            "span_containing", "span_field_masking", "span_first", "span_multi", "span_near", "span_not", "span_or", "span_term", "span_within",
            // Other queries https://www.elastic.co/docs/reference/query-languages/query-dsl/
            "match_all", "shape", "template", "source"
    );


    public RoleConfigWriter(IntermediateRepresentation ir, MigrateConfig.SgAuthc sgAuthc, ActionGroupConfigWriter agWriter) {
        this.ir = ir;
        this.report = MigrationReport.shared;
        this.roles = new ArrayList<>();
        this.sgAuthc = sgAuthc;
        this.agWriter = agWriter;
        createSGRoles();
        addToAuthc();
    }

    private void createSGRoles() {
        for (var role : ir.getRoles()) {
            if ((role.getRemoteClusters() != null && !role.getRemoteClusters().isEmpty()) ||
                    (role.getRemoteIndices() != null && !role.getRemoteIndices().isEmpty())) {
                report.addWarning(FILE_NAME, role.getName(),
                        "Remote indices and clusters are not supported in Search Guard. The role can not be migrated.");
                continue;
            }
            if (role.getRunAs() != null && !role.getRunAs().isEmpty()) {
                report.addWarning(FILE_NAME, role.getName(),
                        "There is no equivalent to 'run as' in Search Guard. Run as is therefor ignored.");
            }
            if (role.getApplications() != null && !role.getApplications().isEmpty()) {
                report.addWarning(FILE_NAME, role.getName(),
                        "There is no equivalent to 'application' in Search Guard. All its entries are therefor ignored.");
            }
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
            var indexPatterns = toSGIndexPattern(index.getNames(), role);
            var indexPermissions = toSGIndexPrivileges(index.getPrivileges(), role);
            var fls = toSGFLS(index, role);
            var dls = toSGDLS(index, role);
            var sgIndex = new SGRole.SGIndex(indexPatterns, indexPermissions, dls, fls);
            sgIndices.add(sgIndex);
        }
        return sgIndices;
    }

    private ArrayList<String> toSGIndexPattern(List<String> indices, Role role) {
        var sgIndices = new ArrayList<String>(indices.size());
        for (var index : indices) {
            try {
                sgIndices.add(LuceneRegexParser.toJavaRegex(index));
            } catch (Exception e) {
                report.addManualAction(FILE_NAME, role.getName() + "->" + index,
                        "An error occurred while trying to convert a Lucene regex to a Java regex: " + e.getMessage());
            }
        }
        return sgIndices;
    }

    private List<String> toSGFLS(Role.Index index, Role role) {
        if (index.getFieldSecurity() == null) return Collections.emptyList();
        var grants = index.getFieldSecurity().getGrant();
        var fls = grants == null ? new ArrayList<String>() : new ArrayList<>(grants);
        for (var iterator = fls.iterator(); iterator.hasNext(); ) {
            var field = iterator.next();
            if (field.isEmpty()) continue;
            if (field.charAt(0) == '~') {
                report.addManualAction(FILE_NAME,
                        role.getName() + "->index->field_security->" + field,
                        "There is a '~' at the start of a field security key. In SG this is used to mark an exclusion.");
                iterator.remove();
            }
        }
        if (index.getFieldSecurity().getExcept() == null) return fls;
        for (var except : index.getFieldSecurity().getExcept()) {
            fls.add("~" + except);
        }
        return fls;
    }

    private void addToAuthc() {
        final var frontendType = "basic/internal_user_db";
        var contents = new LinkedHashMap<String, String>();
        for (var attribute : userMappingAttributes) {
            contents.put(attribute, "user_entry.attributes." + attribute);
        }
        var map = new LinkedHashMap<String, Object>();
        map.put("user_mapping.attributes.from", contents);
        // TODO: Add to sgAuthc
        new MigrateConfig.NewAuthDomain(frontendType, null, null, null, map, null);
    }

    //region Query Migration
    private String parseQueryMap(LinkedHashMap<?, ?> queryMap, String origin) throws InvalidTypeException, InvalidKeyException {
        for (var entry : queryMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new InvalidTypeException("Encountered a key of type: '" + entry.getKey().getClass().getName() + "' for entry: '" + entry +"'.");
            }
            var removeKey = (key.equals("template") || key.equals("source"));
            if (entry.getValue() instanceof LinkedHashMap<?, ?> valueMap) {
                if (!validQueryKeys.contains(key)) {
                    report.addWarning(FILE_NAME, origin,
                            "Unknown key: '" + key + "' found while parsing DSL query. Review this value.");
                }
                if (removeKey) {
                    return parseQueryMap(valueMap, origin);
                }
                return "{\""+ key + "\": " + parseQueryMap(valueMap, origin) + "}";
            } else if (entry.getValue() instanceof String value) {
                if (removeKey) {
                    return parseQueryString(value, origin);
                }
                return "{\"" + key + "\": \"" + parseQueryString(value, origin) + "\"}";
            } else if (entry.getValue() instanceof ArrayList<?> value) {
                if (removeKey) {
                    return parseQueryArray(value, origin);
                }
                return "{\"" + key + "\": " + parseQueryArray(value, origin) + "}";
            } else {
                print(entry.getValue().getClass());
                return "{\""+ key + "\": " + entry.getValue().toString() + "}";
            }
        }
        return "";
    }

    private String parseQueryArray(ArrayList<?> array, String origin) throws InvalidTypeException, InvalidKeyException {
        var strRep = new StringBuilder("[");
        for (var i = 0; i < array.size(); i++) {
            var element = array.get(i);
            if (element instanceof LinkedHashMap<?,?> map) {
                strRep.append(parseQueryMap(map, origin));
            } else if (element instanceof String elementStr) {
                strRep.append(parseQueryString(elementStr, origin));
            } else if (element instanceof ArrayList<?> elementArray) {
                strRep.append(parseQueryArray(elementArray, origin));
            } else {
                strRep.append(element);
            }
            if (i < array.size()-1) strRep.append(", ");
        }

        return strRep + "]";
    }

    private String parseQueryString(String value, String origin) throws InvalidKeyException {
        if (value.matches(".*\\{\\{([#^]).+?}}.+?\\{\\{/.+?}}.*")) {
            report.addWarning(FILE_NAME, origin,
                    "Suspected incompatible Mustache template syntax. The part: '" + value + "' probably needs to be adjusted or removed.");
            throw new InvalidKeyException();
        } else if (value.strip().matches("^\\{\\{.+}}")) {
            return parseTemplate(value.strip(), origin);
        }
        return value;
    }

    private String parseTemplate(String template, String origin) throws InvalidKeyException {
        final var fallback = template;
        var sgTemplate = "${";
        template = template.substring(2, template.length()-2);
        if (template.matches("^_user\\..*")) {
            sgTemplate += "user.";
            template = template.substring(6);
            if (template.equals("username")) {
                sgTemplate += "name";
            } else if (template.equals("roles")) {
                sgTemplate += "roles";
            } else if (template.equals("full_name")) {
                sgTemplate += "attrs.full_name";
                userMappingAttributes.add("full_name");
            } else if (template.equals("email")) {
                sgTemplate += "attrs.email";
                userMappingAttributes.add("full_name");
            } else if (template.matches("^metadata\\..*")) {
                template = template.substring(9);
                sgTemplate += "attrs." + template;
                userMappingAttributes.add(template);
            } else {
                report.addWarning(FILE_NAME, origin,
                        "Encountered an unsupported value for _user in the variable substitution. Review the part: '" + fallback + "'.");
                throw new InvalidKeyException();
            }
        } else {
            report.addWarning(FILE_NAME, origin,
                    "Encountered an unsupported value in a variable substitution field. Review the part: '" + fallback + "'.");
            throw new InvalidKeyException();
        }
        return sgTemplate + "}";
    }
    //endregion

    private String toSGDLS(Role.Index index, Role role) {
        var query = index.getQuery();
        if (query == null) return null;
        try {
            var queryJSON = DocReader.json().read(query);
            if (queryJSON instanceof LinkedHashMap<?,?> queryMap) {
                var parsedQuery = parseQueryMap(queryMap, role.getName() + "->indices->query");
                print(query);
                print(parsedQuery);
                return parsedQuery;
            }
        } catch (DocumentParseException e) {
            report.addManualAction(FILE_NAME,
                    role.getName() + "->indices->query",
                    "The error '" + e.getMessage() + "' occurred while trying to parse the string: '" + query + "' to a JSON object.");
        } catch (InvalidTypeException e) {
            report.addWarning(FILE_NAME,
                    role.getName() + "->indices->query",
                    e.getMessage() + "Please review the query '" + query + "'.");
        } catch (InvalidKeyException e) {
            return null;
        }
        return null;
    }

    private List<String> toSGClusterPrivileges(Role role) {
        var privileges = role.getCluster();
        var sgPrivileges = new ArrayList<String>();

        for (var privilege : privileges) {
            if (privilege.matches("^((cluster)|(indices)):((admin)|(monitor))/.+$")) {
                sgPrivileges.add(privilege);
                continue;
            }
            if (noEquivalentClusterActionGroupKeys.contains(privilege)) {
                try {
                    agWriter.addActionGroup(CustomClusterActionGroup.fromESPrivilege(privilege));
                } catch (IllegalArgumentException e) {
                    report.addWarning(FILE_NAME, role.getName() + "->cluster",
                            "Unexpectedly failed to create custom Action Group. This must be an issue in the migration code and not your input file. Received value: " + e.getMessage());
                    continue;
                }
                sgPrivileges.add("SGS_" + privilege.toUpperCase() + "_CUSTOM");
                continue;
            }

            var agName = matchesStandardSGRole(privilege);

            if (agName == null) {
                report.addManualAction(
                        FILE_NAME,
                        role.getName() + "->cluster_permissions",
                        "The privilege: " + privilege + " is unknown and can not be automatically mapped."
                );
            } else {
                sgPrivileges.add(agName);
            }
        }
        return sgPrivileges;
    }

    private static String matchesStandardSGRole(String privilege) {
        return switch (privilege) {
            case "all" ->  "SGS_CLUSTER_ALL";
            case "manage_ilm" -> "SGS_CLUSTER_MANAGE_ILM";
            case "manage_index_templates" -> "SGS_CLUSTER_MANAGE_INDEX_TEMPLATES";
            case "manage_ingest_pipelines", "manage_pipeline" -> "SGS_CLUSTER_MANAGE_PIPELINES";
            case "monitor" -> "SGS_CLUSTER_MONITOR";
            case "read_ilm" -> "SGS_CLUSTER_READ_ILM";
            default -> null;
        };
    }

    private List<String> toSGIndexPrivileges(List<String> privileges, Role role) {
        var sgPrivileges = new ArrayList<String>();

        for (var privilege : privileges) {
            if (privilege.matches("^((cluster)|(indices)):((admin)|(monitor))/.+$")) {
                sgPrivileges.add(privilege);
                continue;
            }
            if (noEquivalentIndexActionGroupKeys.contains(privilege)) {
                try {
                  agWriter.addActionGroup(CustomIndexActionGroup.fromESPrivilege(privilege));
                } catch (IllegalArgumentException e) {
                  report.addWarning(FILE_NAME, role.getName() + "->index",
                          "Unexpectedly failed to create custom Action Group. This must be an issue in the migration code and not your input file. Received value: " + e.getMessage());
                  continue;
                }

                sgPrivileges.add("SGS_" + privilege.toUpperCase() + "_CUSTOM");
                continue;
            }

            var agName = matchesStandardSGIndexRole(privilege);

            if (agName == null) {
                report.addManualAction(
                      FILE_NAME,
                      role.getName() + "->index_permissions",
                      "The privilege: " + privilege + " is unknown and can not be automatically mapped."
                );
            } else {
                sgPrivileges.add(agName);
            }
        }
        return sgPrivileges;
    }

    private static String matchesStandardSGIndexRole(String privilege) {
        return switch (privilege) {
            case "all" -> "SGS_INDICES_ALL";
            case "create" -> "SGS_CREATE_INDEX";
            case "delete" -> "SGS_DELETE";
            case "index", "write" -> "SGS_WRITE";
            case "manage" -> "SGS_MANAGE";
            case "monitor" -> "SGS_INDICES_MONITOR";
            case "read" -> "SGS_READ";
            default -> null;
        };
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

    static void print(Object line) { System.out.println(line); }
}
