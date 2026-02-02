package com.floragunn.searchguard.sgctl.config.xpack;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.codova.validation.errors.ValidationError;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.ImmutableMap;
import com.floragunn.searchguard.sgctl.config.trace.Traceable;
import com.floragunn.searchguard.sgctl.config.trace.TraceableDocNode;
import com.floragunn.searchguard.sgctl.config.xpack.RoleMappings.RoleMapping.Templates.Template;
import java.util.Objects;

/**
 * Contains named role mappings.
 *
 * @param mappings Role mappings by name
 */
public record RoleMappings(Traceable<ImmutableMap<String, Traceable<RoleMapping>>> mappings) {

  public RoleMappings {
    Objects.requireNonNull(mappings, "mappings must not be null");
  }

  /**
   * Role mappings define which roles are assigned to each user. Each mapping has rules that
   * identify users and a list of roles that are granted to those users.
   *
   * <p><a
   * href="https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-security-put-role-mapping">
   * Elasticsearch API Reference - put role mapping </a>
   */
  public sealed interface RoleMapping {

    /**
     * @return Whether the role mapping is enabled
     */
    Traceable<Boolean> enabled();

    /**
     * @return The rules that determine which users should be matched by the mapping
     */
    Traceable<Rule> rules();

    /**
     * @return Additional metadata that helps define which roles are assigned to each user. Within
     *     the metadata object, keys beginning with _ are reserved for system usage.
     */
    Traceable<ImmutableMap<String, Traceable<Object>>> metadata();

    /**
     * A role mapping that assigns roles to matching users.
     *
     * @param enabled Whether the role mapping is enabled
     * @param roles The roles to assign
     * @param rules The rules that determine which users should be matched by the mapping
     * @param metadata Additional metadata
     */
    record Roles(
        Traceable<Boolean> enabled,
        Traceable<ImmutableList<Traceable<String>>> roles,
        Traceable<Rule> rules,
        Traceable<ImmutableMap<String, Traceable<Object>>> metadata)
        implements RoleMapping {

      public Roles {
        Objects.requireNonNull(roles, "roles must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
      }
    }

    /**
     * A role mapping using Mustache templates that will be evaluated to determine the roles names
     * that should be granted to the users that match the role mapping rules.
     *
     * @param enabled Whether the role mapping is enabled
     * @param roleTemplates The role templates to evaluate
     * @param rules The rules that determine which users should be matched by the mapping
     * @param metadata Additional metadata
     */
    record Templates(
        Traceable<Boolean> enabled,
        Traceable<ImmutableList<Traceable<Template>>> roleTemplates,
        Traceable<Rule> rules,
        Traceable<ImmutableMap<String, Traceable<Object>>> metadata)
        implements RoleMapping {

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
      record Template(Traceable<Format> format, Traceable<Script> script) {

        public Template {
          Objects.requireNonNull(format, "format must not be null");
          Objects.requireNonNull(script, "script must not be null");
        }

        /**
         * A script
         *
         * @param source The script source
         * @param params The script parameters
         * @param lang The scripting language
         */
        record Script(
            Traceable<DocNode> source, // TODO: String or SearchRequestBody ?
            Traceable<DocNode> params,
            Traceable<Lang> lang) {

          public Script {
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(params, "params must not be null");
            Objects.requireNonNull(lang, "lang must not be null");
          }

          /** Scripting language */
          enum Lang {
            PAINLESS,
            EXPRESSION,
            MUSTACHE,
            JAVA
          }

          public static Script parse(TraceableDocNode tDoc) {
            return new Script(
                tDoc.get("source").required().asDocNode(),
                tDoc.get("params").required().asDocNode(),
                tDoc.get("lang").required().asEnum(Lang.class));
          }
        }

        /** Template format */
        enum Format {
          STRING,
          JSON
        }

        /**
         * Parses a template from the given document.
         *
         * @param tDoc The document
         * @return The parsed template
         */
        public static Template parse(TraceableDocNode tDoc) {
          return new Template(
              tDoc.get("format").required().asEnum(Format.class),
              tDoc.get("script").required().as(Script::parse));
        }
      }
    }

    /** Determines which users should be matched by the role mapping. */
    sealed interface Rule {

      /**
       * Requires that at least one of the given rules match
       *
       * @param rules The rules
       */
      record Any(Traceable<ImmutableList<Traceable<Rule>>> rules) implements Rule {

        public Any {
          Objects.requireNonNull(rules, "rules must not be null");
        }
      }

      /**
       * Requires that all the given rules match
       *
       * @param rules
       */
      record All(Traceable<ImmutableList<Traceable<Rule>>> rules) implements Rule {

        public All {
          Objects.requireNonNull(rules, "rules must not be null");
        }
      }

      /**
       * Negates the given rule
       *
       * @param rule The rule to negate
       */
      record Except(Traceable<Rule> rule) implements Rule {

        public Except {
          Objects.requireNonNull(rule, "rule must not be null");
        }
      }

      /**
       * Matches data
       *
       * @param match The data to match
       */
      record Field(Traceable<ImmutableMap<String, Traceable<Object>>> match) implements Rule {

        public Field {
          Objects.requireNonNull(match, "data must not be null");
        }
      }

      /**
       * Parses a rule from the given document.
       *
       * @param tDoc The document
       * @return The parsed rule
       */
      static Rule parse(TraceableDocNode tDoc) {
        var errors = tDoc.getErrors();
        if (tDoc.getAttributeCount() != 1)
          errors.add(
              new ValidationError(
                  null, "Rule must have exactly one of: [any, all, except, field]"));

        Rule rule = null;
        if (tDoc.hasNonNull("any")) {
          rule = new Any(tDoc.get("any").required().asListOf(Rule::parse));
        } else if (tDoc.hasNonNull("all")) {
          rule = new All(tDoc.get("all").required().asListOf(Rule::parse));
        } else if (tDoc.hasNonNull("except")) {
          rule = new Except(tDoc.get("except").required().as(Rule::parse));
        } else if (tDoc.hasNonNull("field")) {
          rule = new Field(tDoc.get("field").required().asMapOf(DocNode::toBasicObject));
        } else {
          errors.add(
              new ValidationError(
                  null, "Rule must have exactly one of: [any, all, except, field]"));
        }

        return rule;
      }
    }

    /**
     * Parses a role mapping from the given document.
     *
     * @param tDoc The document
     */
    static RoleMapping parse(TraceableDocNode tDoc) {
      var errors = tDoc.getErrors();

      var enabled = tDoc.get("enabled").asBoolean(true);
      var rules = tDoc.get("rules").required().as(Rule::parse);
      var metadata = tDoc.get("metadata").asMapOf(DocNode::toBasicObject, ImmutableMap.empty());

      Traceable<ImmutableList<Traceable<String>>> roles = null;
      Traceable<ImmutableList<Traceable<Template>>> templates = null;

      var hasRoles = tDoc.hasNonNull("roles");
      var hasTemplate = tDoc.hasNonNull("template");
      if (hasRoles && hasTemplate) {
        errors.add(
            new ValidationError("roles", "Role mapping cannot have both 'roles' and 'template'"));
      } else if (hasRoles) {
        roles = tDoc.get("roles").asListOfStrings(ImmutableList.empty());
      } else if (hasTemplate) {
        templates = tDoc.get("role_templates").asListOf(Template::parse, ImmutableList.empty());
      } else {
        errors.add(new MissingAttribute("roles"));
      }

      return roles != null
          ? new Roles(enabled, roles, rules, metadata)
          : new Templates(enabled, templates, rules, metadata);
    }
  }

  /**
   * Parses role mappings from the given document.
   *
   * @param tDoc The document
   * @return The parsed role mappings
   */
  public static RoleMappings parse(TraceableDocNode tDoc) {
    return new RoleMappings(tDoc.asAttribute().asMapOf(RoleMapping::parse));
  }
}
