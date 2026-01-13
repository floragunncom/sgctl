package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.codova.documents.Document;
import com.floragunn.fluent.collections.ImmutableList;
import com.floragunn.fluent.collections.OrderedImmutableMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record SgAuthC(ImmutableList<AuthDomain<?>> authDomains) implements NamedConfig<SgAuthC> {

  public SgAuthC {
    Objects.requireNonNull(authDomains, "authDomains must not be null");
  }

  @Override
  public String getFileName() {
    return "sg_authc.yml";
  }

  @Override
  public Object toBasicObject() {
    var builder = new OrderedImmutableMap.Builder<String, Object>();
    builder.put("auth_domains", authDomains);
    return builder.build();
  }

  public interface AuthDomain<T> extends Document<T> {

    record Internal() implements AuthDomain<Internal> {

      @Override
      public Object toBasicObject() {
        return OrderedImmutableMap.of("type", "basic/internal_users_db");
      }
    }

    /**
     * Authentication via LDAP.
     *
     * @param identityProvider Configuration of the LDAP identity provider.
     * @param userSearch Configuration of user search.
     * @param groupSearch Configuration of group search.
     */
    public record Ldap(
        IdentityProvider identityProvider,
        Optional<UserSearch> userSearch,
        Optional<GroupSearch> groupSearch)
        implements AuthDomain<Ldap> {

      public Ldap {
        Objects.requireNonNull(identityProvider, "identityProvider must not be null");
        Objects.requireNonNull(userSearch, "userSearch must not be null");
        Objects.requireNonNull(groupSearch, "groupSearch must not be null");
      }

      @Override
      public Object toBasicObject() {
        var builder = new OrderedImmutableMap.Builder<String, Object>();
        builder.put("type", "basic/ldap");
        builder.put("idp", identityProvider);
        userSearch.ifPresent(us -> builder.put("user_search", us));
        groupSearch.ifPresent(gs -> builder.put("group_search", gs));
        return builder.build();
      }

      /**
       * Controls connections to the LDAP server.
       *
       * @param hosts An arbitrary number of hosts to connect to.
       * @param connectionStrategy The strategy to use when connecting to multiple hosts.
       * @param minPoolSize Minimum size of LDAP connection pool. Default: 3.
       * @param maxPoolSize Maximum size of LDAP connection pool. Default: 10.
       */
      public record IdentityProvider(
          ImmutableList<String> hosts,
          Optional<ConnectionStrategy> connectionStrategy,
          Optional<String> bindDn,
          Optional<String> password,
          Optional<Integer> minPoolSize,
          Optional<Integer> maxPoolSize)
          implements Document<IdentityProvider> {

        public IdentityProvider {
          Objects.requireNonNull(hosts, "hosts must not be null");
          Objects.requireNonNull(connectionStrategy, "connectionStrategy must not be null");
          Objects.requireNonNull(minPoolSize, "minPoolSize must not be null");
          Objects.requireNonNull(maxPoolSize, "maxPoolSize must not be null");
        }

        @Override
        public Object toBasicObject() {
          var builder = new OrderedImmutableMap.Builder<String, Object>();
          builder.put("hosts", hosts);
          bindDn.ifPresent(bd -> builder.put("bind_dn", bd));
          password.ifPresent(pw -> builder.put("password", pw));
          connectionStrategy.ifPresent(
              cs -> builder.put("connection_strategy", cs.name().toLowerCase()));
          minPoolSize.ifPresent(mps -> builder.put("min_pool_size", mps));
          maxPoolSize.ifPresent(mps -> builder.put("max_pool_size", mps));
          return builder.build();
        }

        /** The behavior when which host will be connected to (if there are multiple). * */
        public enum ConnectionStrategy {
          /** cycles through all specified hosts * */
          ROUNDROBIN,
          /** Prefers the topmost entry * */
          FAILOVER,
          /** Tries to choose the fasted host from all specified hosts * */
          FASTEST,
          /** Chooses the host with the fewest connections * */
          FEWEST
        }
      }

      /**
       * LDAP user search configuration.
       *
       * @param baseDn Root of the directory tree under which users shall be searched. Default: The
       *     empty dn.
       * @param scope Defines the scope of the search. Default: {@link SearchScope#SUB}
       * @param filter The filter to use for searching users.
       */
      public record UserSearch(
          Optional<String> baseDn, Optional<SearchScope> scope, Optional<Filter> filter)
          implements Document<UserSearch> {

        public UserSearch {
          Objects.requireNonNull(baseDn, "baseDn must not be null");
          Objects.requireNonNull(scope, "scope must not be null");
          Objects.requireNonNull(filter, "filter must not be null");
        }

        @Override
        public Object toBasicObject() {
          var builder = new OrderedImmutableMap.Builder<String, Object>();
          baseDn.ifPresent(dn -> builder.put("base_dn", dn));
          scope.ifPresent(s -> builder.put("scope", s.name().toLowerCase(Locale.ROOT)));
          filter.ifPresent(f -> builder.put("filter." + f.key(), f.value()));
          return builder.build();
        }
      }

      /**
       * LDAP group search configuration.
       *
       * @param baseDn Root of the directory tree under which users shall be searched.
       * @param scope Defines the scope of the search. The default is {@code sub}, which specifies
       *     that the tree below {@code base_dn} is searched to any depth. one specifies that only
       *     directly subordinated entries shall be searched.
       * @param recursive Configuration of recursive group search
       * @param cache Configuration of group search caching
       */
      public record GroupSearch(
          String baseDn,
          Optional<SearchScope> scope,
          Optional<Recursive> recursive,
          Optional<Cache> cache)
          implements Document<GroupSearch> {

        @Override
        public Object toBasicObject() {
          var builder = new OrderedImmutableMap.Builder<String, Object>();
          builder.put("base_dn", baseDn);
          scope.ifPresent(s -> builder.put("scope", s.name().toLowerCase(Locale.ROOT)));
          recursive.ifPresent(r -> builder.put("recursive", r.toBasicObject()));
          cache.ifPresent(c -> builder.put("cache", c.toBasicObject()));
          return builder.build();
        }

        /**
         * @param enabled If true, also search for group membership of the groups that have already
         *     been found during group search. Default: false
         * @param enabledFor A regular expression pattern to allow recursion only for DNs which
         *     match the specified pattern
         * @param filter Filter to apply when searching for groups
         * @param maxDepth Maximum recursion depth. Default: 30
         */
        public record Recursive(
            boolean enabled,
            Optional<String> enabledFor,
            Optional<Filter> filter,
            Optional<Integer> maxDepth)
            implements Document<Recursive> {

          @Override
          public Object toBasicObject() {
            var builder = new OrderedImmutableMap.Builder<String, Object>();
            builder.put("enabled", enabled);
            enabledFor.ifPresent(ef -> builder.put("enabled_for", ef));
            filter.ifPresent(f -> builder.put("filter." + f.key(), f.value()));
            maxDepth.ifPresent(md -> builder.put("max_depth", md));
            return builder.build();
          }
        }

        /**
         * @param enabled If true, caching is enabled for group search results. Default: true
         * @param expireAfterWrite Duration string defining how long an entry is cached after being
         *     written. Default: 2m
         * @param maxSize Maximum number of entries to keep in the cache. Default: 1000 entries
         */
        public record Cache(
            boolean enabled, Optional<String> expireAfterWrite, Optional<Integer> maxSize)
            implements Document<Cache> {

          @Override
          public Object toBasicObject() {
            var builder = new OrderedImmutableMap.Builder<String, Object>();
            builder.put("enabled", enabled);
            expireAfterWrite.ifPresent(eaw -> builder.put("expire_after_write", eaw));
            maxSize.ifPresent(ms -> builder.put("max_size", ms));
            return builder.build();
          }
        }
      }

      public sealed interface Filter {

        /**
         * @return The value of this filter *
         */
        String value();

        /**
         * @return The key that this filter uses in the config *
         */
        String key();

        /**
         * Filters the subtree by the specify attribute which must match the user name which was
         * extracted by the authentication frontend
         *
         * @param value The attribute to filter by
         */
        public record ByAttribute(String value) implements Filter {

          @Override
          public String key() {
            return "by_attribute";
          }
        }

        /**
         * Raw LDAP search filter supporting the following placeholders:
         *
         * <ul>
         *   <li>{@code ${dn}} refers to the distinguished name of the LDAP entry found by the user
         *       search phase.
         *   <li>{@code ${user.name}} refers to the user name extracted by the authentication
         *       frontend.
         *   <li>A placeholder like {@code ${ldap_user_entry.x}} can be used to refer to any
         *       attribute of the LDAP entry found by the user search phase.
         * </ul>
         *
         * @param value The raw filter
         */
        public record Raw(String value) implements Filter {

          @Override
          public String key() {
            return "raw";
          }
        }
      }

      public enum SearchScope {
        /** The tree below {@code base_dn} is searched to any depth. */
        SUB,
        /** Only directly subordinated entries shall be searched. */
        ONE
      }
    }
  }
}
