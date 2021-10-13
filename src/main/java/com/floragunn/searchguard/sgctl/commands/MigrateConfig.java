/*
 * Copyright 2021 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.floragunn.searchguard.sgctl.commands;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocParseException;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.validation.ValidatingDocNode;
import com.floragunn.codova.validation.ValidationErrors;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.codova.validation.errors.ValidationError;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.util.YamlRewriter;
import com.floragunn.searchguard.sgctl.util.YamlRewriter.Attribute;
import com.floragunn.searchguard.sgctl.util.YamlRewriter.RewriteException;
import com.floragunn.searchguard.sgctl.util.YamlRewriter.RewriteResult;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "migrate-config", description = "Converts old-style sg_config.yml and kibana.yml into sg_frontend_config.yml")
public class MigrateConfig implements Callable<Integer> {

    @Parameters
    List<String> parameters;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files")
    File outputDir;

    @Option(names = {
            "--target-platform" }, description = "Specifies the target platform. Possible values: es (Elasticsearch), os (Opensearch), es711 (Elasticsearch 7.11 or newer)")
    String targetPlatform;

    public Integer call() throws Exception {
        System.out.println("Welcome to the Search Guard config migration tool.\n\n"
                + "This tool converts legacy Search Guard configuration to configuration suitable for the next generation Search Guard release.\nThe tool also provides basic guidance for a seamless update process without outages.\n");

        File sgConfig = null;
        File kibanaConfig = null;

        for (String arg : parameters) {
            File file = new File(arg);

            if (file.getName().startsWith("sg_config") && file.getName().endsWith(".yml")) {
                sgConfig = file;
            } else if (arg.endsWith("kibana.yml")) {
                kibanaConfig = file;
            }
        }

        if (sgConfig == null) {
            System.out.flush();
            System.err.println("You must specify a path to a sg_config.yml on the command line");
            return 1;
        }

        if (kibanaConfig == null) {
            System.out.flush();
            System.err.println("You must specify a path to a kibana.yml on the command line");
            return 1;
        }

        if (!sgConfig.exists()) {
            System.out.flush();
            System.err.println("The file " + sgConfig + " does not exist");
            return 1;
        }

        if (!kibanaConfig.exists()) {
            System.out.flush();
            System.err.println("The file " + kibanaConfig + " does not exist");
            return 1;
        }

        boolean publicBaseUrlAvailable = false;
        String dashboardConfigFileName = "kibana.yml";

        if ("os".equalsIgnoreCase(targetPlatform) || "opensearch".equalsIgnoreCase(targetPlatform)) {
            dashboardConfigFileName = "opensearch_dashboard.yml";
        }

        if ("es711".equalsIgnoreCase(targetPlatform)) {
            publicBaseUrlAvailable = true;
        }

        try {
            SessionConfigMigrator sessionConfigMigrator = new SessionConfigMigrator(sgConfig, kibanaConfig, publicBaseUrlAvailable,
                    dashboardConfigFileName);
            UpdateInstructions updateInstructions = sessionConfigMigrator.createUpdateInstructions();

            if (updateInstructions.getError() != null) {
                System.out.flush();
                System.err.println(updateInstructions.getError());
                return 1;
            }

            if (outputDir != null) {
                if (updateInstructions.sgFrontendConfig != null && !updateInstructions.sgFrontendConfig.isEmpty()) {
                    try {
                        Files.write(new File(outputDir, "sg_frontend_config.yml").toPath(),
                                DocWriter.yaml().writeAsString(updateInstructions.sgFrontendConfig).getBytes(Charsets.UTF_8));
                    } catch (Exception e) {
                        System.out.flush();
                        System.err.println("Error writing " + new File(outputDir, "sg_frontend_config.yml"));
                        return 1;
                    }
                }

                if (updateInstructions.kibanaConfig != null) {
                    try {
                        Files.write(new File(outputDir, dashboardConfigFileName).toPath(), updateInstructions.kibanaConfig.getBytes(Charsets.UTF_8));
                    } catch (Exception e) {
                        System.out.flush();
                        System.err.println("Error writing " + new File(outputDir, dashboardConfigFileName));
                        return 1;
                    }
                }
            }

            if (sessionConfigMigrator.oldKibanaConfigValidationErrors.hasErrors() || sessionConfigMigrator.oldSgConfigValidationErrors.hasErrors()) {
                System.out.println(
                        "\nWARNING: We detected validation errors in the provided configuration files. We try to create the new configuration files anyway.\n"
                                + "However, you might want to review the validation errors and the generated files.\n");

                if (sessionConfigMigrator.oldKibanaConfigValidationErrors.hasErrors()) {
                    System.out.println("Errors in " + kibanaConfig + "\n" + sessionConfigMigrator.oldKibanaConfigValidationErrors + "\n");
                }

                if (sessionConfigMigrator.oldSgConfigValidationErrors.hasErrors()) {
                    System.out.println("Errors in " + sgConfig + "\n" + sessionConfigMigrator.oldSgConfigValidationErrors + "\n");
                }
            }

            System.out.println("The update process consists of these steps:\n");
            System.out.println(
                    "- Update the Search Guard plugin for Elasticsearch on all nodes of your cluster. In this step, you do not yet need to modify the configuration.\n");

            if (updateInstructions.sgFrontendConfig != null && !updateInstructions.sgFrontendConfig.isEmpty()) {
                System.out.println("- " + updateInstructions.sgFrontendConfigInstructions);

                if (outputDir != null) {
                    System.out.println("  It is listed below and has been also put to " + outputDir + ".");

                } else {
                    System.out.println("  It is listed below.");
                }

                //   if (updateInstructions.sgFrontendConfigInstructionsTypeSpecific != null) {
                //      System.out.println(updateInstructions.sgFrontendConfigInstructionsTypeSpecific);
                //  }

                if (updateInstructions.sgFrontendConfigInstructionsAdvanced != null) {
                    System.out.println("  " + updateInstructions.sgFrontendConfigInstructionsAdvanced);
                }

                if (updateInstructions.sgFrontendConfigInstructionsReview != null) {
                    System.out.println("  " + updateInstructions.sgFrontendConfigInstructionsReview);
                }

                System.out.println("\n---------------------------------------------------------------------------------");

                System.out.println(DocWriter.yaml().writeAsString(updateInstructions.sgFrontendConfig));

                System.out.println("---------------------------------------------------------------------------------\n");

            } else {
                System.out.println("- " + updateInstructions.sgFrontendConfigInstructions);
            }

            System.out.println(
                    "- Afterwards, you need to update the Search Guard plugin for Kibana.\n  " + updateInstructions.kibanaConfigInstructions);

            return 0;

        } catch (Exception e) {
            // TODO improve
            e.printStackTrace();
            return 1;
        }
    }

    public class SessionConfigMigrator {

        private final ValidationErrors oldSgConfigValidationErrors = new ValidationErrors();
        private final ValidationErrors oldKibanaConfigValidationErrors = new ValidationErrors();
        private final ValidatingDocNode oldSgConfig;
        private final ValidatingDocNode oldKibanaConfig;
        private final YamlRewriter kibanaConfigRewriter;
        private final boolean publicBaseUrlAvailable;
        private String dashboardConfigFileName;

        public SessionConfigMigrator(File legacySgConfig, File legacyKibanaConfig, boolean publicBaseUrlAvailable, String dashboardConfigFileName)
                throws FileNotFoundException, IOException, DocParseException {
            this.oldSgConfig = new ValidatingDocNode(DocReader.yaml().readObject(legacySgConfig), oldSgConfigValidationErrors);
            this.oldKibanaConfig = new ValidatingDocNode(DocReader.yaml().readObject(legacyKibanaConfig), oldKibanaConfigValidationErrors);
            this.kibanaConfigRewriter = new YamlRewriter(legacyKibanaConfig);
            this.publicBaseUrlAvailable = publicBaseUrlAvailable;
            this.dashboardConfigFileName = dashboardConfigFileName;
        }

        public UpdateInstructions createUpdateInstructions() throws SgctlException {
            KibanaAuthType kibanaAuthType = oldKibanaConfig.get("searchguard.auth.type").withDefault(KibanaAuthType.BASICAUTH)
                    .asEnum(KibanaAuthType.class);

            switch (kibanaAuthType) {
            case BASICAUTH:
                return createSgFrontendConfigBasicAuth();
            case SAML:
                return createSgFrontendConfigSaml();
            case OPENID:
                // Note: The name "OPENID" stems from the old config. This is actually a wrong name. Correct would be "oidc" instead. The new config will consistently use oidc.
                return createSgFrontendConfigOidc();
            case JWT:
                return createSgFrontendConfigJwt();
            case KERBEROS:
            case PROXY:
            default:
                throw new RuntimeException("Not implemented: " + kibanaAuthType);

            }

        }

        public UpdateInstructions createSgFrontendConfigBasicAuth() {
            UpdateInstructions updateInstructions = new UpdateInstructions().mainInstructions(
                    "You have configured the Search Guard Kibana plugin to use basic authentication (user name and password based).");

            Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>(SG_META);

            String loginSubtitle = oldKibanaConfig.get("searchguard.basicauth.login.subtitle").asString();

            Map<String, Object> authczEntry = new LinkedHashMap<>();
            authczEntry.put("type", "basic");

            if (loginSubtitle != null) {
                authczEntry.put("message", loginSubtitle);
            }

            newSgFrontendConfig.put("authcz", Collections.singletonList(authczEntry));

            String loadbalancerUrl = oldKibanaConfig.get("searchguard.basicauth.loadbalancer_url").asString();

            if (loadbalancerUrl != null) {
                String publicBaseUrl = oldKibanaConfig.get("server.publicBaseUrl").asString();

                if (publicBaseUrl == null) {
                    this.kibanaConfigRewriter.insertAtBeginning(
                            new Attribute(publicBaseUrlAvailable ? "server.publicBaseUrl" : "searchguard.frontend_base_url", loadbalancerUrl));
                } else if (!publicBaseUrl.equals(loadbalancerUrl)) {
                    oldKibanaConfigValidationErrors.add(new ValidationError("searchguard.basicauth.loadbalancer_url",
                            "server.publicBaseUrl and searchguard.basicauth.loadbalancer_url have different values. This is an unexpected configuration."));
                }
            }

            Boolean showBrandImage = oldKibanaConfig.get("searchguard.basicauth.login.showbrandimage").asBoolean();
            String brandImage = oldKibanaConfig.get("searchguard.basicauth.login.brandimage").asString();
            String loginTitle = oldKibanaConfig.get("searchguard.basicauth.login.title").asString();
            String buttonStyle = oldKibanaConfig.get("searchguard.basicauth.login.buttonstyle").asString();

            if (showBrandImage != null || brandImage != null || loginTitle != null || buttonStyle != null) {
                Map<String, Object> loginPageConfig = new LinkedHashMap<>();

                if (showBrandImage != null)
                    loginPageConfig.put("show_brand_image", showBrandImage);
                if (brandImage != null)
                    loginPageConfig.put("brand_image", brandImage);
                if (loginTitle != null)
                    loginPageConfig.put("title", loginTitle);
                if (buttonStyle != null)
                    loginPageConfig.put("button_style", buttonStyle);

                newSgFrontendConfig.put("login_page", loginPageConfig);
            }

            updateInstructions.sgFrontendConfig(newSgFrontendConfig);

            this.kibanaConfigRewriter.remove("searchguard.auth.type");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.loadbalancer_url");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.login.showbrandimage");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.login.brandimage");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.login.title");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.login.subtitle");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.login.buttonstyle");

            try {
                RewriteResult rewriteResult = this.kibanaConfigRewriter.rewrite();

                if (rewriteResult.isChanged()) {
                    updateInstructions.kibanaConfigInstructions("Before starting Kibana with the updated plugin, you need to update the file config/"
                            + dashboardConfigFileName + " in your Kibana installation. \n  The necessary changes are listed below. "
                            + (outputDir != null
                                    ? "An automatically updated " + dashboardConfigFileName + " file has been put by this tool to " + outputDir + "."
                                    : "")
                            + "\n\n" + "---------------------------------------------------------------------------------\n"
                            + kibanaConfigRewriter.getManualInstructions()
                            + "\n---------------------------------------------------------------------------------");
                    updateInstructions.kibanaConfig(rewriteResult.getYaml());
                } else {
                    updateInstructions.kibanaConfigInstructions("You do not need to update the Kibana configuration.");
                }
            } catch (RewriteException e) {
                updateInstructions.kibanaConfigInstructions(
                        "Before starting Kibana with the updated plugin, you need to update the file config/" + dashboardConfigFileName
                                + " in your Kibana installation.\n  Please perform the following updates:\n\n" + e.getManualInstructions());
            }

            return updateInstructions;
        }

        public UpdateInstructions createSgFrontendConfigSaml() throws SgctlException {
            Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>(SG_META);

            List<DocNode> samlAuthDomains = oldSgConfig.getDocumentNode().findNodesByJsonPath(
                    "$.sg_config.dynamic.authc.*[?(@.http_authenticator.type == 'saml' || @.http_authenticator.type == 'com.floragunn.dlic.auth.http.saml.HTTPSamlAuthenticator')]");

            String frontendBaseUrl = null;

            if (samlAuthDomains.isEmpty()) {
                return new UpdateInstructions().error(
                        "No auth domains of type 'saml' are defined in the provided sg_config.yml file, even though kibana.yml is configured to use SAML authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            List<DocNode> activeSamlAuthDomains = samlAuthDomains.stream().filter((node) -> node.get("http_enabled") != Boolean.FALSE)
                    .collect(toList());

            if (activeSamlAuthDomains.isEmpty()) {
                return new UpdateInstructions().error(
                        "All auth domains of type 'saml' defined in sg_config.yml are disabled, even though kibana.yml is configured to use SAML authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            UpdateInstructions updateInstructions = new UpdateInstructions();

            updateInstructions.setSgFrontendConfigInstructionsTypeSpecific(
                    "You have configured Search Guard to use SAML authentication. The SAML configuration was moved to sg_frontend_config.yml.");

            if (activeSamlAuthDomains.size() > 1) {
                updateInstructions.sgFrontendConfigInstructionsAdvanced(
                        "sg_config.yml defines more than one auth domain of type 'saml'. This is a non-standard advanced cofiguration. The new Search Guard Kibana plugin will use this configuration to present a list of all available SAML auth domains when logging in. The user can then choose from one of the auth domains.");
                updateInstructions.sgFrontendConfigInstructionsReview(
                        "Please review the settings. If one of the SAML auth domains is not necessary, you should remove it.");
            }

            List<Map<String, Object>> newAuthDomains = new ArrayList<>();

            for (DocNode samlAuthDomain : activeSamlAuthDomains) {
                Map<String, Object> newAuthDomain = new LinkedHashMap<>();
                newAuthDomains.add(newAuthDomain);

                newAuthDomain.put("type", "saml");

                if (activeSamlAuthDomains.size() > 1) {
                    newAuthDomain.put("label", samlAuthDomain.getKey());
                }

                ValidationErrors samlAuthDomainValidationErrors = new ValidationErrors();
                ValidatingDocNode vSamlAuthDomain = new ValidatingDocNode(samlAuthDomain, samlAuthDomainValidationErrors);

                String kibanaUrl = vSamlAuthDomain.get("http_authenticator.config.kibana_url").required().asString();

                if (frontendBaseUrl == null) {
                    frontendBaseUrl = kibanaUrl;
                } else if (kibanaUrl != null && !frontendBaseUrl.equals(kibanaUrl)) {
                    throw new SgctlException(
                            "You have two SAML auth domains for different Kibana URLs. This configuration is not supported by this tool. If you are running several Kibana instances, please check the Search Guard documentation on how to configure several Kibana instances.");
                }

                String idpMetadataUrl = vSamlAuthDomain.get("http_authenticator.config.idp.metadata_url").asString();
                String idpMetadataFile = vSamlAuthDomain.get("http_authenticator.config.idp.metadata_file").asString();

                if (idpMetadataFile == null && idpMetadataUrl == null) {
                    samlAuthDomainValidationErrors.add(new MissingAttribute("http_authenticator.config.idp.metadata_url"));
                }

                String idpEntityId = vSamlAuthDomain.get("http_authenticator.config.idp.entity_id").required().asString();

                Map<String, Object> idp = new LinkedHashMap<>();

                if (idpMetadataUrl != null) {
                    idp.put("metadata_url", idpMetadataUrl);
                }

                if (idpMetadataFile != null) {
                    idp.put("metadata_xml", "${file:" + idpMetadataFile + "}");
                }

                idp.put("entity_id", idpEntityId);

                Map<String, Object> tls = vSamlAuthDomain.get("http_authenticator.config.idp").asMap();

                if (tls != null) {
                    MigrationResult migrationResult = migrateTlsConfig(tls);

                    if (migrationResult != null) {
                        idp.put("tls", tls);
                        oldSgConfigValidationErrors.add("http_authenticator.config.idp", migrationResult.getSourceValidationErrors());
                    }
                }

                newAuthDomain.put("idp", idp);

                String spEntityId = vSamlAuthDomain.get("http_authenticator.config.sp.entity_id").required().asString();
                String spSignatureAlgorithm = vSamlAuthDomain.get("http_authenticator.config.sp.signature_algorithm").asString();
                String spSignaturePrivateKeyPassword = vSamlAuthDomain.get("http_authenticator.config.sp.signature_private_key_password").asString();
                String spSignaturePrivateKeyFilepath = vSamlAuthDomain.get("http_authenticator.config.sp.signature_private_key_filepath").asString();
                String spSignaturePrivateKey = vSamlAuthDomain.get("http_authenticator.config.sp.signature_private_key").asString();
                Boolean useForceAuth = vSamlAuthDomain.get("http_authenticator.config.sp.forceAuthn").asBoolean();

                Map<String, Object> sp = new LinkedHashMap<>();

                sp.put("entity_id", spEntityId);

                if (spSignatureAlgorithm != null) {
                    sp.put("signature_algorithm", spSignatureAlgorithm);
                }

                if (spSignaturePrivateKeyPassword != null) {
                    sp.put("signature_private_key_password", spSignaturePrivateKeyPassword);
                }

                if (spSignaturePrivateKeyFilepath != null) {
                    sp.put("signature_private_key_filepath", spSignaturePrivateKeyFilepath);
                }

                if (spSignaturePrivateKey != null) {
                    sp.put("signature_private_key", spSignaturePrivateKey);
                }

                if (useForceAuth != null) {
                    sp.put("forceAuthn", useForceAuth);
                }

                newAuthDomain.put("sp", sp);

                String subjectKey = vSamlAuthDomain.get("http_authenticator.config.subject_key").asString();

                if (subjectKey != null) {
                    newAuthDomain.put("user_mapping.subject", subjectKey);
                }

                String subjectPattern = vSamlAuthDomain.get("http_authenticator.config.subject_pattern").asString();

                if (subjectPattern != null) {
                    newAuthDomain.put("user_mapping.subject_pattern", subjectPattern);
                }

                String rolesKey = vSamlAuthDomain.get("http_authenticator.config.roles_key").required().asString();

                if (rolesKey != null) {
                    newAuthDomain.put("user_mapping.roles", rolesKey);
                }

                String rolesSeparator = vSamlAuthDomain.get("http_authenticator.config.roles_seperator").asString();

                if (rolesSeparator != null) {
                    newAuthDomain.put("user_mapping.roles_seperator", rolesKey);
                }

                Boolean checkIssuer = vSamlAuthDomain.get("http_authenticator.config.check_issuer").asBoolean();

                if (checkIssuer != null) {
                    newAuthDomain.put("check_issuer", checkIssuer);
                }

                Object validator = vSamlAuthDomain.get("http_authenticator.config.validator").asAnything();

                if (validator instanceof Map) {
                    newAuthDomain.put("validator", validator);
                }

                if (samlAuthDomainValidationErrors.hasErrors()) {
                    oldSgConfigValidationErrors.add("sg_config.dynamic.authc." + samlAuthDomain.getKey(), samlAuthDomainValidationErrors);
                }

            }

            newSgFrontendConfig.put("authcz", newAuthDomains);

            updateInstructions.sgFrontendConfig(newSgFrontendConfig);

            this.kibanaConfigRewriter.remove("searchguard.auth.type");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.loadbalancer_url");

            if (!this.oldKibanaConfig.hasNonNull("server.publicBaseUrl")) {
                this.kibanaConfigRewriter.insertAtBeginning(new YamlRewriter.Attribute(
                        publicBaseUrlAvailable ? "server.publicBaseUrl" : "searchguard.frontend_base_url", frontendBaseUrl));
            }

            try {
                RewriteResult rewriteResult = this.kibanaConfigRewriter.rewrite();

                if (rewriteResult.isChanged()) {
                    updateInstructions.kibanaConfigInstructions(
                            "Before starting Kibana with the updated plugin, you need to update the file config/kibana.yml in your Kibana installation. \n  The necessary changes are listed below. "
                                    + (outputDir != null ? "An automatically updated kibana.yml file has been put by this tool to " + outputDir + "."
                                            : "")
                                    + "\n\n" + "---------------------------------------------------------------------------------\n"
                                    + kibanaConfigRewriter.getManualInstructions()
                                    + "\n---------------------------------------------------------------------------------");
                    updateInstructions.kibanaConfig(rewriteResult.getYaml());
                } else {
                    updateInstructions.kibanaConfigInstructions("You do not need to update the Kibana configuration.");
                }
            } catch (RewriteException e) {
                updateInstructions.kibanaConfigInstructions(
                        "Before starting Kibana with the updated plugin, you need to update the file config/kibana.yml in your Kibana installation.\n  Please perform the following updates:\n\n"
                                + e.getManualInstructions());
            }

            return updateInstructions;
        }

        public UpdateInstructions createSgFrontendConfigOidc() {
            Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>(SG_META);

            List<DocNode> oidcAuthDomains = oldSgConfig.getDocumentNode()
                    .findNodesByJsonPath("$.sg_config.dynamic.authc.*[?(@.http_authenticator.type == 'openid')]");

            String frontendBaseUrl = oldKibanaConfig.get("searchguard.openid.base_redirect_url").asString();

            if (frontendBaseUrl == null) {
                frontendBaseUrl = getFrontendBaseUrlFromKibanaYaml();
            }

            if (oidcAuthDomains.isEmpty()) {
                return new UpdateInstructions().error(
                        "No auth domains of type 'openid' are defined in the provided sg_config.yml, even though kibana.yml is configured to use OIDC authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            List<DocNode> activeOidcAuthDomains = oidcAuthDomains.stream().filter((node) -> node.get("http_enabled") != Boolean.FALSE)
                    .collect(toList());

            if (activeOidcAuthDomains.isEmpty()) {
                return new UpdateInstructions().error(
                        "All auth domains of type 'openid' defined in sg_config.yml are disabled, even though kibana.yml is configured to use OIDC authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            UpdateInstructions updateInstructions = new UpdateInstructions()
                    .mainInstructions("You have configured Search Guard to use OIDC authentication.");

            if (activeOidcAuthDomains.size() > 1) {
                updateInstructions.mainInstructions(
                        "You have defined several OIDC authentication domains. The configuration will be converted in such a way that the user can choose from a list of authentication domains. If you are using a setup with multiple Kibana instances, please refer to the Search Guard documentation on how to configure such a setup.");
            }

            List<Map<String, Object>> newAuthDomains = new ArrayList<>();

            for (DocNode oidcAuthDomain : activeOidcAuthDomains) {
                Map<String, Object> newAuthDomain = new LinkedHashMap<>();
                newAuthDomains.add(newAuthDomain);

                newAuthDomain.put("type", "oidc");

                if (activeOidcAuthDomains.size() > 1) {
                    newAuthDomain.put("label", oidcAuthDomain.getKey());
                }

                ValidationErrors authDomainValidationErrors = new ValidationErrors();
                ValidatingDocNode vOidcAuthDomain = new ValidatingDocNode(oidcAuthDomain, authDomainValidationErrors);

                String openIdConnectUrl = vOidcAuthDomain.get("http_authenticator.config.openid_connect_url").required().asString();
                String kibanaYmlOpenIdConnectUrl = oldKibanaConfig.get("searchguard.openid.connect_url").asString();

                if (openIdConnectUrl != null && kibanaYmlOpenIdConnectUrl != null && !openIdConnectUrl.equals(kibanaYmlOpenIdConnectUrl)) {
                    authDomainValidationErrors.add(new ValidationError("http_authenticator.config.openid_connect_url",
                            "The openid_connect_url in sg_config.yml and kibana.yml must be equal. However, in the given configuration the URLs differ."));
                }

                String clientId = oldKibanaConfig.get("searchguard.openid.client_id").required().asString();
                String clientSecret = oldKibanaConfig.get("searchguard.openid.client_secret").required().asString();
                String scope = oldKibanaConfig.get("searchguard.openid.scope").asString();
                String logoutUrl = oldKibanaConfig.get("searchguard.openid.logout_url").asString();

                newAuthDomain.put("idp.openid_configuration_url", openIdConnectUrl);
                newAuthDomain.put("client_id", clientId);
                newAuthDomain.put("client_secret", clientSecret);

                if (scope != null) {
                    newAuthDomain.put("scope", scope);
                }

                if (logoutUrl != null) {
                    newAuthDomain.put("logout_url", logoutUrl);
                }

                String subjectKey = vOidcAuthDomain.get("http_authenticator.config.subject_key").asString();

                if (subjectKey != null) {
                    newAuthDomain.put("user_mapping.subject", "$['" + subjectKey + "']");
                }

                String subjectPath = vOidcAuthDomain.get("http_authenticator.config.subject_path").asString();

                if (subjectPath != null) {
                    newAuthDomain.put("user_mapping.subject", subjectPath);
                }

                String subjectPattern = vOidcAuthDomain.get("http_authenticator.config.subject_pattern").asString();

                if (subjectPattern != null) {
                    newAuthDomain.put("user_mapping.subject_pattern", subjectPattern);
                }

                String rolesKey = vOidcAuthDomain.get("http_authenticator.config.roles_key").asString();

                if (rolesKey != null) {
                    newAuthDomain.put("user_mapping.roles", "$['" + rolesKey + "']");
                }

                String rolesPath = vOidcAuthDomain.get("http_authenticator.config.roles_path").asString();

                if (rolesPath != null) {
                    newAuthDomain.put("user_mapping.roles", rolesPath);
                }

                Object claimsToUserAttrs = vOidcAuthDomain.get("http_authenticator.config.map_claims_to_user_attrs").asAnything();

                if (claimsToUserAttrs != null) {
                    newAuthDomain.put("user_mapping.attrs", claimsToUserAttrs);
                }

                Object proxy = vOidcAuthDomain.get("http_authenticator.config.proxy").asAnything();

                if (proxy != null) {
                    newAuthDomain.put("idp.proxy", proxy);
                }

                Map<String, Object> tls = vOidcAuthDomain.get("http_authenticator.config.openid_connect_idp").asMap();

                if (tls != null) {
                    MigrationResult migrationResult = migrateTlsConfig(tls);

                    if (migrationResult != null) {
                        newAuthDomain.put("idp.tls", tls);
                        oldSgConfigValidationErrors.add("http_authenticator.config.openid_connect_idp", migrationResult.getSourceValidationErrors());
                    }
                }

                migrateAttribute("idp_request_timeout_ms", vOidcAuthDomain, newAuthDomain);
                migrateAttribute("idp_queued_thread_timeout_ms", vOidcAuthDomain, newAuthDomain);
                migrateAttribute("refresh_rate_limit_time_window_ms", vOidcAuthDomain, newAuthDomain);
                migrateAttribute("refresh_rate_limit_count", vOidcAuthDomain, newAuthDomain);
                migrateAttribute("cache_jwks_endpoint", vOidcAuthDomain, newAuthDomain);

                if (authDomainValidationErrors.hasErrors()) {
                    oldSgConfigValidationErrors.add("sg_config.dynamic.authc." + oidcAuthDomain.getKey(), authDomainValidationErrors);
                }

            }

            newSgFrontendConfig.put("authcz", newAuthDomains);

            if (!this.oldKibanaConfig.hasNonNull("server.publicBaseUrl")) {
                this.kibanaConfigRewriter.insertAtBeginning(new YamlRewriter.Attribute(
                        publicBaseUrlAvailable ? "server.publicBaseUrl" : "searchguard.frontend_base_url", frontendBaseUrl));
            }

            updateInstructions.sgFrontendConfig(newSgFrontendConfig);

            this.kibanaConfigRewriter.remove("searchguard.auth.type");
            this.kibanaConfigRewriter.remove("searchguard.basicauth.loadbalancer_url");
            this.kibanaConfigRewriter.remove("searchguard.openid.connect_url");
            this.kibanaConfigRewriter.remove("searchguard.openid.client_id");
            this.kibanaConfigRewriter.remove("searchguard.openid.client_secret");
            this.kibanaConfigRewriter.remove("searchguard.openid.scope");
            this.kibanaConfigRewriter.remove("searchguard.openid.header");
            this.kibanaConfigRewriter.remove("searchguard.openid.base_redirect_url");
            this.kibanaConfigRewriter.remove("searchguard.openid.logout_url");

            try {
                RewriteResult rewriteResult = this.kibanaConfigRewriter.rewrite();

                if (rewriteResult.isChanged()) {
                    updateInstructions.kibanaConfigInstructions(
                            "Before starting Kibana with the updated plugin, you need to update the file config/kibana.yml in your Kibana installation. \n  The necessary changes are listed below. "
                                    + (outputDir != null ? "An automatically updated kibana.yml file has been put by this tool to " + outputDir + "."
                                            : "")
                                    + "\n\n" + kibanaConfigRewriter.getManualInstructions());
                    updateInstructions.kibanaConfig(rewriteResult.getYaml());
                } else {
                    updateInstructions.kibanaConfigInstructions("You do not need to update the Kibana configuration.");
                }
            } catch (RewriteException e) {
                updateInstructions.kibanaConfigInstructions(
                        "Before starting Kibana with the updated plugin, you need to update the file config/kibana.yml in your Kibana installation.\n  Please perform the following updates:\n\n"
                                + e.getManualInstructions());
            }

            return updateInstructions;

        }

        public UpdateInstructions createSgFrontendConfigJwt() throws SgctlException {

            // header is not used any more
            //String header = oldKibanaConfig.get("searchguard.jwt.header").asString();
            String urlParameter = oldKibanaConfig.get("searchguard.jwt.url_parameter").asString();
            String loginEndpoint = oldKibanaConfig.get("searchguard.jwt.login_endpoint").asString();

            UpdateInstructions updateInstructions = new UpdateInstructions();

            if (urlParameter != null) {
                updateInstructions.mainInstructions("You have configured Search Guard to use authentication using a JWT specified as URL parameter.");
            } else {
                throw new SgctlException(
                        "You have configured Search Guard to use authentication using a JWT provided as an Authorization header. This is an advanced configuration, usually only found in combination with a proxy which adds the Authorization header to HTTP requests. This configuration is not supported by this tool. Please refer to the Search Guard documentation for details.");
            }

            this.kibanaConfigRewriter.insertAfter("searchguard.auth.type", new YamlRewriter.Attribute("searchguard.auth.jwt.enabled", true));
            this.kibanaConfigRewriter.insertAfter("searchguard.auth.type",
                    new YamlRewriter.Attribute("searchguard.auth.jwt.url_parameter", urlParameter));

            this.kibanaConfigRewriter.remove("searchguard.auth.type");
            this.kibanaConfigRewriter.remove("searchguard.jwt.header");
            this.kibanaConfigRewriter.remove("searchguard.jwt.login_endpoint");
            this.kibanaConfigRewriter.remove("searchguard.jwt.url_parameter");

            try {
                RewriteResult rewriteResult = this.kibanaConfigRewriter.rewrite();

                if (rewriteResult.isChanged()) {
                    updateInstructions.kibanaConfigInstructions(
                            "Before starting Kibana with the updated plugin, you need to update the file config/kibana.yml in your Kibana installation. \n  The necessary changes are listed below. "
                                    + (outputDir != null ? "An automatically updated kibana.yml file has been put by this tool to " + outputDir + "."
                                            : "")
                                    + "\n\n" + kibanaConfigRewriter.getManualInstructions());
                    updateInstructions.kibanaConfig(rewriteResult.getYaml());
                } else {
                    updateInstructions.kibanaConfigInstructions("You do not need to update the Kibana configuration.");
                }
            } catch (RewriteException e) {
                updateInstructions.kibanaConfigInstructions(
                        "Before starting Kibana with the updated plugin, you need to update the file config/kibana.yml in your Kibana installation.\n  Please perform the following updates:\n\n"
                                + e.getManualInstructions());
            }

            if (loginEndpoint != null) {
                Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>();

                newSgFrontendConfig.put("authcz", Collections.singletonList(ImmutableMap.of("type", "link", "url", loginEndpoint)));

                updateInstructions.sgFrontendConfig(newSgFrontendConfig);

                return updateInstructions;
            } else {
                updateInstructions.sgFrontendConfigInstructions(
                        "In the current configuration, the Search Guard Kibana plugin does not provide a login form. The only way to login is opening a Kibana URL with the URL parameter "
                                + urlParameter
                                + ". Thus, the sg_frontend_config.yml file generated by this tFool will also define no authenticators. If you want to have more login methods, you can add these to sg_frontend_config.yml.");

                updateInstructions.sgFrontendConfig(Collections.emptyMap());

                return updateInstructions;
            }
        }

        private String getFrontendBaseUrlFromKibanaYaml() {
            boolean https = oldKibanaConfig.get("server.ssl.enabled").withDefault(false).asBoolean();
            String host = oldKibanaConfig.get("server.host").required().asString();
            int port = oldKibanaConfig.get("server.port").withDefault(-1).asInteger();
            String basePath = oldKibanaConfig.get("server.basepath").asString();

            if (port == 80 && !https) {
                port = -1;
            } else if (port == 443 && https) {
                port = -1;
            }

            try {
                return new URI(https ? "https" : "http", null, host, port, basePath, null, null).toString();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        private void migrateAttribute(String name, ValidatingDocNode source, Map<String, Object> target) {
            Object value = source.get("http_authenticator.config." + name).asAnything();

            if (value != null) {
                target.put(name, value);
            }
        }

        private MigrationResult migrateTlsConfig(Map<String, Object> config) {
            if (config == null) {
                return null;
            }

            ValidationErrors validationErrors = new ValidationErrors();
            ValidatingDocNode vNode = new ValidatingDocNode(config, validationErrors);

            if (!vNode.get("enable_ssl").withDefault(false).asBoolean()) {
                return null;
            }

            Map<String, Object> result = new LinkedHashMap<>();

            if (vNode.hasNonNull("pemtrustedcas_content")) {
                List<String> pems = vNode.get("pemtrustedcas_content").asListOfStrings();
                result.put("trusted_cas", pems.size() == 1 ? pems.get(0) : pems);
            } else if (vNode.hasNonNull("pemtrustedcas_filepath")) {
                String path = vNode.get("pemtrustedcas_filepath").asString();
                result.put("trusted_cas", "${file:" + path + "}");
            }

            if (vNode.get("enable_ssl_client_auth").withDefault(false).asBoolean()) {
                Map<String, Object> newClientAuthConfig = new LinkedHashMap<>();

                if (vNode.hasNonNull("pemcert_content")) {
                    List<String> pems = vNode.get("pemcert_content").asListOfStrings();
                    newClientAuthConfig.put("certificate", pems.size() == 1 ? pems.get(0) : pems);
                } else if (vNode.hasNonNull("pemcert_filepath")) {
                    String path = vNode.get("pemcert_filepath").asString();
                    newClientAuthConfig.put("certificate", "${file:" + path + "}");
                }

                if (vNode.hasNonNull("pemkey_content")) {
                    List<String> pems = vNode.get("pemkey_content").asListOfStrings();
                    newClientAuthConfig.put("private_key", pems.size() == 1 ? pems.get(0) : pems);
                } else if (vNode.hasNonNull("pemkey_filepath")) {
                    String path = vNode.get("pemkey_filepath").asString();
                    newClientAuthConfig.put("private_key", "${file:" + path + "}");
                }

                if (vNode.hasNonNull("pemkey_password")) {
                    String password = vNode.get("pemkey_password").asString();
                    newClientAuthConfig.put("private_key_password", password);
                }

                if (newClientAuthConfig.size() != 0) {
                    result.put("client_auth", newClientAuthConfig);
                }

            }

            if (vNode.hasNonNull("enabled_ssl_protocols")) {
                result.put("enabled_protocols", vNode.get("enabled_ssl_protocols").asListOfStrings());
            }

            if (vNode.hasNonNull("enabled_ssl_ciphers")) {
                result.put("enabled_ciphers", vNode.get("enabled_ssl_ciphers").asListOfStrings());
            }

            if (vNode.hasNonNull("trust_all")) {
                result.put("trust_all", vNode.get("trust_all").asBoolean());
            }

            if (vNode.hasNonNull("verify_hostnames")) {
                result.put("verify_hostnames", vNode.get("verify_hostnames").asBoolean());
            }

            return new MigrationResult(result, validationErrors);
        }

    }

    public static enum KibanaAuthType {
        BASICAUTH, JWT, OPENID, PROXY, KERBEROS, SAML
    }

    static class UpdateInstructions {

        private String mainInstructions;
        private String error;

        private String esPluginUpdateInstructions = "";
        private String sgFrontendConfigInstructions = "After having updated the Search Guard Elasticsearch plugin, please upload the new sg_frontend_config.yml with sgadmin. Do not modify other configuration files.\n"
                + "  The file sg_frontend_config.yml has been automatically generated from the settings in sg_config.yml and kibana.yml.";
        private String sgFrontendConfigInstructionsAdvanced;
        private String sgFrontendConfigInstructionsTypeSpecific = null;

        private String sgFrontendConfigInstructionsReview = "Please review the settings.";
        private Map<String, Object> sgFrontendConfig;

        private String kibanaPluginUpdateInstructions = "After the new sg_frontend_config.yml has been successfully uploaded to Search Guard, you can update the Search Guard Kibana plugin.";
        private String kibanaConfigInstructions;
        private String kibanaConfig;

        public Map<String, Object> getSgFrontendConfig() {
            return sgFrontendConfig;
        }

        public UpdateInstructions sgFrontendConfig(Map<String, Object> sgFrontendConfig) {
            this.sgFrontendConfig = sgFrontendConfig;
            return this;
        }

        public String getKibanaConfig() {
            return kibanaConfig;
        }

        public UpdateInstructions kibanaConfig(String kibanaConfig) {
            this.kibanaConfig = kibanaConfig;
            return this;
        }

        public String getMainInstructions() {
            return mainInstructions;
        }

        public UpdateInstructions mainInstructions(String mainInstructions) {
            this.mainInstructions = mainInstructions;
            return this;

        }

        public String getSgFrontendConfigInstructions() {
            return sgFrontendConfigInstructions;
        }

        public UpdateInstructions sgFrontendConfigInstructions(String sgFrontendConfigInstructions) {
            this.sgFrontendConfigInstructions = sgFrontendConfigInstructions;
            return this;

        }

        public String getKibanaConfigInstructions() {
            return kibanaConfigInstructions;
        }

        public UpdateInstructions kibanaConfigInstructions(String kibanaConfigInstructions) {
            this.kibanaConfigInstructions = kibanaConfigInstructions;
            return this;

        }

        public String getError() {
            return error;
        }

        public UpdateInstructions error(String error) {
            this.error = error;
            return this;

        }

        public String getEsPluginUpdateInstructions() {
            return esPluginUpdateInstructions;
        }

        public UpdateInstructions esPluginUpdateInstructions(String esPluginUpdateInstructions) {
            this.esPluginUpdateInstructions = esPluginUpdateInstructions;
            return this;
        }

        public String getKibanaPluginUpdateInstructions() {
            return kibanaPluginUpdateInstructions;
        }

        public UpdateInstructions kibanaPluginUpdateInstructions(String kibanaPluginUpdateInstructions) {
            this.kibanaPluginUpdateInstructions = kibanaPluginUpdateInstructions;
            return this;
        }

        public String getSgFrontendConfigInstructionsAdvanced() {
            return sgFrontendConfigInstructionsAdvanced;
        }

        public UpdateInstructions sgFrontendConfigInstructionsAdvanced(String sgFrontendConfigInstructionsAdvanced) {
            this.sgFrontendConfigInstructionsAdvanced = sgFrontendConfigInstructionsAdvanced;
            return this;
        }

        public String getSgFrontendConfigInstructionsReview() {
            return sgFrontendConfigInstructionsReview;
        }

        public UpdateInstructions sgFrontendConfigInstructionsReview(String sgFrontendConfigInstructionsReview) {
            this.sgFrontendConfigInstructionsReview = sgFrontendConfigInstructionsReview;
            return this;

        }

        public String getSgFrontendConfigInstructionsTypeSpecific() {
            return sgFrontendConfigInstructionsTypeSpecific;
        }

        public void setSgFrontendConfigInstructionsTypeSpecific(String sgFrontendConfigInstructionsTypeSpecific) {
            this.sgFrontendConfigInstructionsTypeSpecific = sgFrontendConfigInstructionsTypeSpecific;
        }

    }

    static class MigrationResult {
        private final Map<String, Object> config;

        private final ValidationErrors sourceValidationErrors;

        MigrationResult(Map<String, Object> config, ValidationErrors sourceValidationErrors) {
            this.config = config;
            this.sourceValidationErrors = sourceValidationErrors;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public ValidationErrors getSourceValidationErrors() {
            return sourceValidationErrors;
        }

    }

    private static final Map<String, Object> SG_META = ImmutableMap.of("_sg_meta", ImmutableMap.of("type", "frontend_config", "config_version", 2));
}
