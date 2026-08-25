# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`sgctl` is a picocli-based Java CLI that remote-controls a Search Guard cluster over its REST API
(the replacement for the older `sgadmin`). Authentication is always via an admin client certificate.
See `README.md` for end-user command documentation.

## Build & Test

Java 17+ and Maven 3.8.1+ are enforced (`maven-enforcer-plugin`); compiler release is 17. CI builds
with JDK 25.

```shell
mvn clean install                       # full build -> target/releases/ (zip + executable sgctl.sh)
mvn clean install -DskipTests           # skip the (slow) test suite
mvn test -Dtest=ClonParserTest          # single test class
mvn test -Dtest=ClonParserTest#testOverrideException   # single test method
```

The project version is `${revision}` (default `master-SNAPSHOT`); releases are built with
`-Drevision=<version>` — GitLab CI derives it from a `sgctl-<version>` tag. Do not hardcode a
`<version>` into the POM.

JaCoCo instruments the test JVM (`prepare-agent`); its `report` goal is bound to `verify`, so the
HTML report under `target/site/jacoco/` only appears after `mvn verify`/`install`, not `mvn test`.

### Test suite characteristics

- Surefire uses `forkCount=1`, `reuseForks=false` (a fresh JVM per test class) with `-Xmx3072m`.
- `SgctlTest`, `MigrateConfigTest`, `EnableMultiTenancyTest`, `MoveSearchGuardIndexCommandTest`,
  `ClusterInitializationTest` and `DataMigrationCommandsTest` start a **real Elasticsearch node**
  via `LocalCluster` from `search-guard-flx-security:tests`. These pull `main-SNAPSHOT` artifacts
  (`sg.test-version`) from `maven.search-guard.com`, so they need network access and are slow.
- `RestCommandTest` and `UpdateSgLicenseCommandTest` use WireMock over HTTPS with generated
  `TestCertificates` instead of a cluster. `ClonParserTest` and `SearchGuardRestClientTest` are
  plain unit tests. Prefer these styles for new tests when a real cluster isn't required.
- Tests drive the CLI through `SgctlTool.exec(...)`, assert on the returned exit code, and capture
  `System.out`/`System.err` via `TeeOutputStream`. They pass `--sgctl-config-dir <temp dir>` so they
  never touch `~/.searchguard`.

## Architecture

### Command layer (picocli)

`SgctlTool` is the `@Command(name = "sgctl")` root and the shaded jar's main class. **Every
subcommand must be registered in its `subcommands = {...}` list**, or in
`commands/special/SpecialCommand.java` for the `sgctl special ...` namespace.

Three-level class hierarchy:

- `commands/BaseCommand` — config-dir resolution (`~/.searchguard`, overridable with
  `--sgctl-config-dir`), `-c/--cluster`, `--debug`, `--verbose`, a shared `ValidationErrors`
  accumulator, and `retryOnConcurrencyConflict(...)` (retries a `PreconditionFailedException` up to
  3 times).
- `commands/ConnectingCommand` — all TLS/host options plus `getClient()`. Its `getTlsConfig()`
  implements a deliberate 3-way merge: stored cluster config only, command line only, or both
  merged (command-line values win, and validation errors are re-keyed back to the CLI option names
  they came from). It also performs the initial `/_searchguard/authinfo` check unless
  `--skip-connection-check` is given, and translates SSL failures into human-readable advice.
- Concrete commands — `@Command class X extends ConnectingCommand implements Callable<Integer>`.
  The convention is: return `0` on success, print `e.getMessage()` to `System.err` and return `1`
  for each client exception type. Follow this rather than letting exceptions escape.

### Persisted connection state

`sgctl connect` writes `~/.searchguard/cluster_<clusterId>.yml` (server, port, serialized
`TLSConfig`) via `SgctlConfig.Cluster`, and records the active cluster id in
`~/.searchguard/sgctl-selected-config.txt`. Subsequent commands read both, which is why most
commands need no connection options.

### REST client

`client/SearchGuardRestClient` wraps Apache HttpClient 4 with typed Search Guard endpoints
(`authInfo`, `getConfigBulk`, `putConfigBulk`, `putUser`, `putConfigVar`, `getComponentState`, …)
plus generic `get/post/put/patch/delete`. Responses go through the inner `Response` class:
`.parseResponseBy(SomeResponse::new)` maps onto the DTOs in `client/api/`. Status codes are
translated into a fixed exception family — `UnauthorizedException`, `ServiceUnavailableException`,
`PreconditionFailedException` (412, optimistic-locking conflict), `InvalidResponseException`,
`FailedConnectionException`, `ApiException` (carries server-side `ValidationErrors`).

### Configuration documents & validation (codova)

External library `com.floragunn:codova` supplies the document and validation vocabulary used
throughout: `DocNode`/`DocReader`/`DocWriter`/`Format` for format-agnostic YAML/JSON handling,
`TLSConfig`, and the validation idiom — accumulate into `ValidationErrors` (via
`ValidatingDocNode`, `MissingAttribute`, `ValidationError`, …), then call
`validationErrors.throwExceptionForPresentErrors()` to raise a single `ConfigValidationException`.
New parsing code should follow this pattern instead of throwing on the first bad field.
`com.floragunn:fluent-collections` provides `ImmutableMap`/`ImmutableList`/`ImmutableSet`. Both are
declared with open version ranges (e.g. `[1.10.1,)`), so builds can pick up newer releases.

### Config type mapping and the header round-trip

`client/api/ConfigType` maps a config type to its API name and its `sg_*.yml` file name, and infers
the type from a file's `_sg_meta.type`, its `# sg_<type>` header comment, or its file name. The
enum constant order is load-bearing — names that are prefixes of other names must come **after**
them, because inference uses prefix matching.

`get-config` prepends a header line to each written file:
`# sg_<type> v:<version> cluster:<name> etag:<etag>`. `update-config` parses that header back out
to (a) send the etag for optimistic concurrency and (b) refuse to upload a file tagged for a
different cluster. `--force` skips both checks. Keep both sides in sync when touching either
command.

### Config migration

`commands/MigrateConfig` converts legacy `sg_config.yml` (+ optional `kibana.yml`) into the new
`sg_authc`/frontend configuration, with `util/YamlRewriter` producing the "what to change in your
yml" instructions. `MigrateConfigTest` is parameterized over every directory under
`src/test/resources/migrate_config/` that contains an `sg_config.yml` — **adding a fixture
directory adds a test case**, no Java change needed.

### CLON

`util/ClonParser` implements Command Line Object Notation (`key=value`, `names[]=x`,
`person[age]=20`, `k=[a,b]`), used by `sgctl rest ... --clon` to build request bodies. See the
README section for the full grammar.

## Packaging

`package` produces, all under `target/releases/`:

- a shaded, `minimizeJar`-ed executable jar (main class `SgctlTool`) turned into a self-executing
  `sgctl.sh` by `really-executable-jar-maven-plugin`;
- a zip assembly (`src/main/assemblies/sgctl.xml`) bundling `src/main/tools/sgctl.sh`, the
  dependencies under `deps/`, `LICENSE` and the generated `THIRD-PARTY.txt`.

Because the shade plugin runs with `minimizeJar`, classes only reached reflectively can be stripped
— if a new dependency is loaded by name at runtime, verify the packaged `sgctl.sh` actually works,
not just `mvn test`.

`docker/Dockerfile` does **not** build from source; it downloads a published `sgctl-<version>.sh`
release from `maven.search-guard.com`.

## CI

`.gitlab-ci.yml`: `build` on every push; `deploy_snapshot` publishes `b-<branch>-SNAPSHOT`; tagging
`sgctl-<version>` triggers `build_release`, `deploy_release` and the multi-arch Docker image push.
Deploy jobs use `settings.xml` with `ARTIFACTORY_USER`/`ARTIFACTORY_PASSWORD`.
