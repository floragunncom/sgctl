package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.Parser;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.codova.validation.errors.ValidationError;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Templates.Template;

import java.util.Objects;

/**
 * Contains named role mappings.
 *
 * @param mappings Role mappings by name
 */
public record RoleMappings(
    ImmutableMap<String, RoleMapping> mappings
) {

    public RoleMappings {
        Objects.requireNonNull(mappings, "mappings must not be null");
    }
    
    /**
     * Role mappings define which roles are assigned to each user.
     * Each mapping has rules that identify users and a list of roles that are granted to those users.
     * <p>
     * <a href="https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-security-put-role-mapping">
     * Elasticsearch API Reference - put role mapping
     * </a>
     */
    public sealed interface RoleMapping {

        /**
         * @return Whether the role mapping is enabled
         */
        boolean enabled();

        /**
         * @return The rules that determine which users should be matched by the mapping
         */
        Rule rules();

        /**
         * @return Additional metadata that helps define which roles are assigned to each user.
         * Within the metadata object, keys beginning with _ are reserved for system usage.
         */
        DocNode metadata();

        /**
         * A role mapping that assigns roles to matching users.
         *
         * @param enabled  Whether the role mapping is enabled
         * @param roles    The roles to assign
         * @param rules    The rules that determine which users should be matched by the mapping
         * @param metadata Additional metadata
         */
        record Roles(
            boolean enabled,
            ImmutableList<String> roles,
            Rule rules,
            DocNode metadata
        ) implements RoleMapping {
            
            public Roles {
                Objects.requireNonNull(roles, "roles must not be null");
                Objects.requireNonNull(rules, "rules must not be null");
                Objects.requireNonNull(metadata, "metadata must not be null");
            }
            
        }

        /**
         * A role mapping using Mustache templates that will be evaluated to determine
         * the roles names that should be granted to the users that match the role mapping rules.
         *
         * @param enabled       Whether the role mapping is enabled
         * @param roleTemplates The role templates to evaluate
         * @param rules         The rules that determine which users should be matched by the mapping
         * @param metadata      Additional metadata
         */
        record Templates(
            boolean enabled,
            ImmutableList<Template> roleTemplates,
            Rule rules,
            DocNode metadata
        ) implements RoleMapping {
            
            public Templates {
                Objects.requireNonNull(roleTemplates, "roleTemplates must not be null");
                Objects.requireNonNull(rules, "rules must not be null");
                Objects.requireNonNull(metadata, "metadata must not be null");
            }

            /**
             * A role template
             *
             * @param format The format
             * @param script The script
             */
            record Template(
                Format format,
                Script script
            ) {
                
                public Template {
                    Objects.requireNonNull(format, "format must not be null");
                    Objects.requireNonNull(script, "script must not be null");
                }

                /**
                 * A script
                 *
                 * @param source The script source
                 * @param params The script parameters
                 * @param lang   The scripting language
                 */
                record Script(
                    DocNode source, // TODO: String or SearchRequestBody ?
                    DocNode params,
                    Lang lang
                ) {
                    
                    public Script {
                        Objects.requireNonNull(source, "source must not be null");
                        Objects.requireNonNull(params, "params must not be null");
                        Objects.requireNonNull(lang, "lang must not be null");
                    }

                    /**
                     * Scripting language
                     */
                    enum Lang {
                        PAINLESS, EXPRESSION, MUSTACHE, JAVA
                    }

                    /**
                     * Parses a script from the given document.
                     *
                     * @param doc     The document
                     * @param context The parser context
                     * @return The parsed script
                     * @throws ConfigValidationException If validation fails
                     */
                    static Script parse(DocNode doc, Parser.Context context) throws ConfigValidationException {
                        var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
                        var source = vDoc.get("source").asDocNode();
                        var params = vDoc.get("params").asDocNode();
                        var lang = vDoc.get("lang").asEnum(Lang.class);

                        vDoc.throwExceptionForPresentErrors();

                        return new Script(source, params, lang);
                    }

                }

                /**
                 * Template format
                 */
                enum Format {
                    STRING, JSON
                }

                /**
                 * Parses a template from the given document.
                 *
                 * @param doc     The document
                 * @param context The parser context
                 * @return The parsed template
                 * @throws ConfigValidationException If validation fails
                 */
                static Template parse(DocNode doc, Parser.Context context) throws ConfigValidationException {
                    var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);
                    var format = vDoc.get("format").asEnum(Format.class);
                    var script = vDoc.get("script").by(Script::parse);

                    vDoc.throwExceptionForPresentErrors();

                    return new Template(format, script);
                }

            }

        }

        /**
         * Determines which users should be matched by the role mapping.
         */
        sealed interface Rule {

            /**
             * Requires that at least one of the given rules match
             *
             * @param rules The rules
             */
            record Any(ImmutableList<Rule> rules) implements Rule {
                
                public Any {
                    Objects.requireNonNull(rules, "rules must not be null");
                }
                
            }

            /**
             * Requires that all the given rules match
             *
             * @param rules
             */
            record All(ImmutableList<Rule> rules) implements Rule {
                
                public All {
                    Objects.requireNonNull(rules, "rules must not be null");
                }
                
            }

            /**
             * Negates the given rule
             *
             * @param rule The rule to negate
             */
            record Except(Rule rule) implements Rule {
                
                public Except {
                    Objects.requireNonNull(rule, "rule must not be null");
                }
                
            }

            /**
             * Matches data
             *
             * @param data The data to match
             */
            record Field(DocNode data) implements Rule {
                
                public Field {
                    Objects.requireNonNull(data, "data must not be null");
                }
                
            }

            /**
             * Parses a rule from the given document.
             *
             * @param doc     The document
             * @param context The parser context
             * @return The parsed rule
             * @throws ConfigValidationException If validation fails
             */
            static Rule parse(DocNode doc, Parser.Context context) throws ConfigValidationException {
                var errors = new ValidationErrors();
                var vDoc = new ValidatingDocNode(doc, errors, context);
                if (doc.size() != 1)
                    errors.add(new ValidationError(null, "Rule must have exactly one of: [any, all, except, field]", doc));

                Rule rule = null;
                if (doc.hasNonNull("any")) {
                    rule = new Any(vDoc.get("any").asList().ofObjectsParsedBy(Rule::parse));
                } else if (doc.hasNonNull("all")) {
                    rule = new All(vDoc.get("all").asList().ofObjectsParsedBy(Rule::parse));
                } else if (doc.hasNonNull("except")) {
                    rule = new Except(vDoc.get("except").by(Rule::parse));
                } else if (doc.hasNonNull("field")) {
                    rule = new Field(vDoc.get("field").asDocNode());
                } else {
                    errors.add(new ValidationError(null, "Rule must have exactly one of: [any, all, except, field]", doc));
                }

                errors.throwExceptionForPresentErrors();

                return rule;
            }

        }

        /**
         * Parses a role mapping from the given document.
         *
         * @param doc     The document
         * @param context The parser context
         * @return The parsed role mapping
         * @throws ConfigValidationException If validation fails
         */
        static RoleMapping parse(DocNode doc, Parser.Context context) throws ConfigValidationException {
            var errors = new ValidationErrors();
            var vDoc = new ValidatingDocNode(doc, errors, context);

            var enabled = vDoc.get("enabled").withDefault(true).asBoolean();
            var rules = vDoc.get("rules").by(Rule::parse);
            var metadata = vDoc.get("metadata").required().asDocNode();

            ImmutableList<String> roles = null;
            ImmutableList<Template> templates = null;

            var hasRoles = vDoc.hasNonNull("roles");
            var hasTemplate = vDoc.hasNonNull("template");
            if (hasRoles && hasTemplate) {
                errors.add(new ValidationError("roles", "Role mapping cannot have both 'roles' and 'template'", doc));
            } else if (hasRoles) {
                roles = vDoc.get("roles").asList().withEmptyListAsDefault().ofStrings();
            } else if (hasTemplate) {
                templates = vDoc.get("role_templates").asList().withEmptyListAsDefault().ofObjectsParsedBy(Template::parse);
            } else {
                errors.add(new MissingAttribute("roles", doc));
            }

            vDoc.throwExceptionForPresentErrors();

            return roles != null
                ? new Roles(enabled, roles, rules, metadata)
                : new Templates(enabled, templates, rules, metadata);
        }

    }

    /**
     * Parses role mappings from the given document.
     *
     * @param doc     The document
     * @param context The parser context
     * @return The parsed role mappings
     * @throws ConfigValidationException If validation fails
     */
    public static RoleMappings parse(DocNode doc, Parser.Context context) throws ConfigValidationException {
        var vDoc = new ValidatingDocNode(doc, new ValidationErrors(), context);

        var builder = new ImmutableMap.Builder<String, RoleMapping>(doc.size());
        for (var name : doc.keySet()) {
            builder.with(name, vDoc.get(name).by(RoleMapping::parse));
        }

        vDoc.throwExceptionForPresentErrors();

        return new RoleMappings(builder.build());
    }

}
