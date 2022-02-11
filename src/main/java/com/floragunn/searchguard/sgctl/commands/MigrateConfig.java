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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.floragunn.codova.documents.DocNode;
import com.floragunn.codova.documents.DocReader;
import com.floragunn.codova.documents.DocWriter;
import com.floragunn.codova.documents.Document;
import com.floragunn.codova.documents.DocumentParseException;
import com.floragunn.codova.documents.UnexpectedDocumentStructureException;
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

@Command(name = "migrate-config", description = "Converts old-style sg_config.yml and kibana.yml into sg_authc.yml and sg_frontend_config.yml")
public class MigrateConfig implements Callable<Integer> {

    @Parameters
    List<String> parameters;

    @Option(names = { "-o", "--output-dir" }, description = "Directory where to write new configuration files")
    File outputDir;

    @Option(names = {
            "--target-platform" }, description = "Specifies the target platform. Possible values: es (Elasticsearch), os (Opensearch), es711 (Elasticsearch 7.11 or newer)")
    String targetPlatform;

    public Integer call() throws Exception {
        if (parameters == null) {
            System.err.println(
                    "You must specify the paths to the Search Guard configuration files sg_config.yml and optionally kibana.yml on the command line");
            return 1;
        }

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
            ConfigMigrator configMigrator = new ConfigMigrator(sgConfig, kibanaConfig, publicBaseUrlAvailable, dashboardConfigFileName);
            BackendUpdateInstructions backendUpdateInstructions = configMigrator.createBackendUpdateInstructions();
            FrontendUpdateInstructions frontendUpdateInstructions = configMigrator.createUpdateInstructions();

            if (configMigrator.oldKibanaConfigValidationErrors.hasErrors() || configMigrator.oldSgConfigValidationErrors.hasErrors()) {
                System.out.println(
                        "\nWARNING: We detected validation errors in the provided configuration files. We try to create the new configuration files anyway.\n"
                                + "However, you might want to review the validation errors and the generated files.\n");

                if (configMigrator.oldKibanaConfigValidationErrors.hasErrors()) {
                    System.out.println("Errors in " + kibanaConfig + "\n" + configMigrator.oldKibanaConfigValidationErrors + "\n");
                }

                if (configMigrator.oldSgConfigValidationErrors.hasErrors()) {
                    System.out.println("Errors in " + sgConfig + "\n" + configMigrator.oldSgConfigValidationErrors + "\n");
                }
            }

            if (backendUpdateInstructions != null) {
                if (outputDir != null) {
                    if (backendUpdateInstructions.sgAuthc != null) {
                        try {
                            Files.write(new File(outputDir, "sg_authc.yml").toPath(),
                                    DocWriter.yaml().writeAsString(backendUpdateInstructions.sgAuthc).getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, "sg_authc.yml"));
                            return 1;
                        }
                    }
                    
                    if (backendUpdateInstructions.sgAuthz != null) {
                        try {
                            Files.write(new File(outputDir, "sg_authz.yml").toPath(),
                                    DocWriter.yaml().writeAsString(backendUpdateInstructions.sgAuthz).getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, "sg_authz.yml"));
                            return 1;
                        }
                    }


                    if (backendUpdateInstructions.sgFrontendMultiTenancy != null) {
                        try {
                            Files.write(new File(outputDir, "sg_frontend_multi_tenancy.yml").toPath(),
                                    DocWriter.yaml().writeAsString(backendUpdateInstructions.sgFrontendMultiTenancy).getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, "sg_frontend_multi_tenancy.yml"));
                            return 1;
                        }
                    }

                    if (backendUpdateInstructions.sgLicense != null) {
                        try {
                            Files.write(new File(outputDir, "sg_license_key.yml").toPath(),
                                    DocWriter.yaml().writeAsString(backendUpdateInstructions.sgLicense).getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, "sg_license_key.yml"));
                            return 1;
                        }
                    }

                    if (backendUpdateInstructions.sgAuthTokenService != null) {
                        try {
                            Files.write(new File(outputDir, "sg_auth_token_service.yml").toPath(),
                                    DocWriter.yaml().writeAsString(backendUpdateInstructions.sgAuthTokenService).getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, "sg_auth_token_service.yml"));
                            return 1;
                        }
                    }
                }
            }

            if (frontendUpdateInstructions != null) {
                if (outputDir != null) {
                    if (frontendUpdateInstructions.sgFrontendConfig != null && !frontendUpdateInstructions.sgFrontendConfig.isEmpty()) {
                        try {
                            Files.write(new File(outputDir, "sg_frontend_config.yml").toPath(),
                                    DocWriter.yaml().writeAsString(frontendUpdateInstructions.sgFrontendConfig).getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, "sg_frontend_config.yml"));
                            return 1;
                        }
                    }

                    if (frontendUpdateInstructions.kibanaConfig != null) {
                        try {
                            Files.write(new File(outputDir, dashboardConfigFileName).toPath(),
                                    frontendUpdateInstructions.kibanaConfig.getBytes(Charsets.UTF_8));
                        } catch (Exception e) {
                            System.out.flush();
                            System.err.println("Error writing " + new File(outputDir, dashboardConfigFileName));
                            return 1;
                        }
                    }
                }
            }

            System.out.println("The update process consists of these steps:\n");
            System.out.println(
                    "- Update the Search Guard plugin for Elasticsearch on all nodes of your cluster. In this step, you do not yet need to modify the configuration.\n");
            System.out
                    .println("- After having updated the Search Guard Elasticsearch plugin, please upload the new configuration files with sgctl:\n");

            if (frontendUpdateInstructions != null && frontendUpdateInstructions.sgFrontendConfig != null
                    && !frontendUpdateInstructions.sgFrontendConfig.isEmpty()) {
                System.out.println("$ ./sgctl.sh update-config sg_authc.yml sg_frontend_config.yml\n");
                System.out.print("The files have been automatically generated from the settings in sg_config.yml and kibana.yml. ");
            } else {
                System.out.println("$ ./sgctl.sh update-config sg_authc.yml\n");
                System.out.print("The files have been automatically generated from the settings in sg_config.yml. ");
            }

            if (outputDir != null) {
                System.out.println(" The files are listed below and have been also put to " + outputDir + ".\n");
            } else {
                System.out.println(" The files are listed below. Use the -o switch of this tool to write the files to an output directory.\n");
            }

            if (frontendUpdateInstructions != null) {

                if (frontendUpdateInstructions.sgFrontendConfigInstructionsAdvanced != null) {
                    System.out.println(frontendUpdateInstructions.sgFrontendConfigInstructionsAdvanced);
                }

                if (frontendUpdateInstructions.sgFrontendConfigInstructionsReview != null) {
                    System.out.println(frontendUpdateInstructions.sgFrontendConfigInstructionsReview);
                }

            }

            System.out.println("\n----------------------------- sg_authc.yml --------------------------------------");
            System.out.println(DocWriter.yaml().writeAsString(backendUpdateInstructions.sgAuthc));
            System.out.println("\n---------------------------------------------------------------------------------\n");

            if (backendUpdateInstructions.sgAuthz != null) {
                System.out.println("\n----------------------------- sg_authz.yml --------------------------------------");
                System.out.println(DocWriter.yaml().writeAsString(backendUpdateInstructions.sgAuthz));
                System.out.println("\n---------------------------------------------------------------------------------\n");                
            }
            
            if (backendUpdateInstructions.sgFrontendMultiTenancy != null) {
                System.out.println("\n--------------------- sg_frontend_multi_tenancy.yml -------------------------------");
                System.out.println(DocWriter.yaml().writeAsString(backendUpdateInstructions.sgFrontendMultiTenancy));
                System.out.println("\n----------------------------------------------------------------------------------\n");
            }

            if (backendUpdateInstructions.sgLicense != null) {
                System.out.println("\n--------------------------- sg_license_key.yml ------------------------------------");
                System.out.println(DocWriter.yaml().writeAsString(backendUpdateInstructions.sgLicense));
                System.out.println("\n----------------------------------------------------------------------------------\n");
            }

            if (backendUpdateInstructions.sgAuthTokenService != null) {
                System.out.println("\n----------------------- sg_auth_token_service.yml --------------------------------");
                System.out.println(DocWriter.yaml().writeAsString(backendUpdateInstructions.sgAuthTokenService));
                System.out.println("\n----------------------------------------------------------------------------------\n");
            }

            if (frontendUpdateInstructions != null) {

                if (frontendUpdateInstructions.sgFrontendConfig != null && !frontendUpdateInstructions.sgFrontendConfig.isEmpty()) {

                    System.out.println("\n------------------------ sg_frontend_config.yml ---------------------------------");
                    System.out.println(DocWriter.yaml().writeAsString(frontendUpdateInstructions.sgFrontendConfig));
                    System.out.println("----------------------------------------------------------------------------------\n");

                } else {
                    System.out.println("- " + frontendUpdateInstructions.sgFrontendConfigInstructions);
                }

            }

            if (frontendUpdateInstructions != null) {

                if (frontendUpdateInstructions.sgFrontendConfigInstructionsAdvanced != null) {
                    System.out.println(frontendUpdateInstructions.sgFrontendConfigInstructionsAdvanced);
                }

                if (frontendUpdateInstructions.sgFrontendConfigInstructionsReview != null) {
                    System.out.println(frontendUpdateInstructions.sgFrontendConfigInstructionsReview);
                }

                System.out.println("\n- Afterwards, you need to update the Search Guard plugin for Kibana.\n  "
                        + frontendUpdateInstructions.kibanaConfigInstructions);

            }

            return 0;

        } catch (Exception e) {
            // TODO improve
            e.printStackTrace();
            return 1;
        }
    }

    public class ConfigMigrator {

        private final ValidationErrors oldSgConfigValidationErrors = new ValidationErrors();
        private final ValidationErrors oldKibanaConfigValidationErrors = new ValidationErrors();
        private final ValidatingDocNode oldSgConfig;
        private final ValidatingDocNode oldKibanaConfig;
        private final YamlRewriter kibanaConfigRewriter;
        private final boolean publicBaseUrlAvailable;
        private String dashboardConfigFileName;

        public ConfigMigrator(File legacySgConfig, File legacyKibanaConfig, boolean publicBaseUrlAvailable, String dashboardConfigFileName)
                throws FileNotFoundException, IOException, DocumentParseException, UnexpectedDocumentStructureException {
            this.oldSgConfig = new ValidatingDocNode(DocReader.yaml().readObject(legacySgConfig), oldSgConfigValidationErrors);
            this.oldKibanaConfig = new ValidatingDocNode(DocReader.yaml().readObject(legacyKibanaConfig), oldKibanaConfigValidationErrors);
            this.kibanaConfigRewriter = new YamlRewriter(legacyKibanaConfig);
            this.publicBaseUrlAvailable = publicBaseUrlAvailable;
            this.dashboardConfigFileName = dashboardConfigFileName;
        }

        public BackendUpdateInstructions createBackendUpdateInstructions() {
            BackendUpdateInstructions result = new BackendUpdateInstructions();
            result.sgAuthc = new SgAuthc();
            boolean anonymousAuth = oldSgConfig.get("sg_config.dynamic.http.anonymous_auth_enabled").withDefault(false).asBoolean();
            String license = oldSgConfig.get("sg_config.dynamic.license").asString();
            Map<String, Object> authTokenService = oldSgConfig.get("sg_config.dynamic.auth_token_provider").asMap();
            boolean doNotFailOnForbidden = oldSgConfig.get("sg_config.dynamic.do_not_fail_on_forbidden").withDefault(false).asBoolean();
            String fieldAnonymizationSalt2 = oldSgConfig.get("sg_config.dynamic.field_anonymization_salt2").asString();
            
            if (doNotFailOnForbidden == false || fieldAnonymizationSalt2 != null) {
                Map<String, Object> authzConfig = new LinkedHashMap<>();
                if (doNotFailOnForbidden == false) {
                    authzConfig.put("ignore_unauthorized_indices", false);                    
                }
                
                if (fieldAnonymizationSalt2 != null) {
                    authzConfig.put("field_anonymization.salt", fieldAnonymizationSalt2);
                }
                
                result.sgAuthz = DocNode.wrap(authzConfig);
            }

            if (oldSgConfig.get("sg_config.dynamic.http.xff.enabled").withDefault(false).asBoolean()) {
                result.sgAuthc.internalProxies = oldSgConfig.get("sg_config.dynamic.http.xff.internalProxies").asString();
                result.sgAuthc.remoteIpHeader = oldSgConfig.get("sg_config.dynamic.http.xff.remoteIpHeader").asString();
            }

            if (oldSgConfig.get("sg_config.dynamic.kibana.multitenancy_enabled").withDefault(false).asBoolean()) {
                String feMtServerUsername = oldSgConfig.get("sg_config.dynamic.kibana.server_username").asString();
                String feMtIndex = oldSgConfig.get("sg_config.dynamic.kibana.index").asString();
                result.sgFrontendMultiTenancy = DocNode.of("enabled", true, "index", feMtIndex, "server_user", feMtServerUsername);
            }

            DocNode authz = oldSgConfig.getDocumentNode().getAsNode("sg_config", "dynamic", "authz");
            List<UserInformationBackend> userInformationBackends = new ArrayList<>();

            if (!authz.isNull()) {
                for (Map.Entry<String, DocNode> entry : authz.toMapOfNodes().entrySet()) {
                    if (entry.getValue().hasNonNull("http_enabled") && Boolean.FALSE.equals(entry.getValue().get("http_enabled"))) {
                        continue;
                    }

                    AuthzDomain authzDomain = new AuthzDomain(entry.getValue());

                    userInformationBackends.addAll(authzDomain.toUserInformationBackends());

                    oldSgConfigValidationErrors.add("sg_config.dynamic.authz." + entry.getKey(), authzDomain.validationErrors);
                }
            }

            DocNode authc = oldSgConfig.getDocumentNode().getAsNode("sg_config", "dynamic", "authc");

            List<OldAuthDomain> oldAuthDomains = new ArrayList<>();

            for (Map.Entry<String, DocNode> entry : authc.toMapOfNodes().entrySet()) {
                if (entry.getValue().hasNonNull("http_enabled") && Boolean.FALSE.equals(entry.getValue().get("http_enabled"))) {
                    continue;
                }

                oldAuthDomains.add(new OldAuthDomain(entry.getKey(), entry.getValue()));
            }

            Collections.sort(oldAuthDomains);

            List<NewAuthDomain> newAuthDomains = new ArrayList<>();

            for (OldAuthDomain oldAuthDomain : oldAuthDomains) {
                newAuthDomains.addAll(oldAuthDomain.toNewAuthDomains(userInformationBackends));
                oldSgConfigValidationErrors.add("sg_config.dynamic.authc." + oldAuthDomain.id, oldAuthDomain.validationErrors);
            }

            if (anonymousAuth) {
                NewAuthDomain anonAuthDomain = new NewAuthDomain("anonymous", null, null, null, null, null);
                anonAuthDomain.userMappingUserName.put("static", "sg_anonymous");
                anonAuthDomain.userMappingRoles.put("static", "sg_anonymous_backendrole");
                newAuthDomains.add(anonAuthDomain);
            }

            result.sgAuthc.authDomains = newAuthDomains;

            if (license != null) {
                result.sgLicense = DocNode.of("license", license);
            }

            if (authTokenService != null && authTokenService.size() != 0) {
                result.sgAuthTokenService = DocNode.wrap(authTokenService);
            }

            return result;
        }

        public FrontendUpdateInstructions createUpdateInstructions() throws SgctlException {
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
                oldKibanaConfigValidationErrors
                        .add(new ValidationError("searchguard.auth.type", "The Kibana authentication type " + kibanaAuthType + " is not supported"));
                return null;

            }
        }

        public FrontendUpdateInstructions createSgFrontendConfigBasicAuth() {
            FrontendUpdateInstructions updateInstructions = new FrontendUpdateInstructions().mainInstructions(
                    "You have configured the Search Guard Kibana plugin to use basic authentication (user name and password based).");

            Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>(SG_META);

            String loginSubtitle = oldKibanaConfig.get("searchguard.basicauth.login.subtitle").asString();

            Map<String, Object> authczEntry = new LinkedHashMap<>();
            authczEntry.put("type", "basic");

            if (loginSubtitle != null) {
                authczEntry.put("message", loginSubtitle);
            }

            newSgFrontendConfig.put("auth_domains", Collections.singletonList(authczEntry));

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

        public FrontendUpdateInstructions createSgFrontendConfigSaml() throws SgctlException {
            Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>(SG_META);

            List<DocNode> samlAuthDomains = oldSgConfig.getDocumentNode().findNodesByJsonPath(
                    "$.sg_config.dynamic.authc.*[?(@.http_authenticator.type == 'saml' || @.http_authenticator.type == 'com.floragunn.dlic.auth.http.saml.HTTPSamlAuthenticator')]");

            String frontendBaseUrl = null;

            if (samlAuthDomains.isEmpty()) {
                return new FrontendUpdateInstructions().error(
                        "No auth domains of type 'saml' are defined in the provided sg_config.yml file, even though kibana.yml is configured to use SAML authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            List<DocNode> activeSamlAuthDomains = samlAuthDomains.stream().filter((node) -> node.get("http_enabled") != Boolean.FALSE)
                    .collect(toList());

            if (activeSamlAuthDomains.isEmpty()) {
                return new FrontendUpdateInstructions().error(
                        "All auth domains of type 'saml' defined in sg_config.yml are disabled, even though kibana.yml is configured to use SAML authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            FrontendUpdateInstructions updateInstructions = new FrontendUpdateInstructions();

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

            newSgFrontendConfig.put("auth_domains", newAuthDomains);

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

        public FrontendUpdateInstructions createSgFrontendConfigOidc() {
            Map<String, Object> newSgFrontendConfig = new LinkedHashMap<>(SG_META);

            List<DocNode> oidcAuthDomains = oldSgConfig.getDocumentNode()
                    .findNodesByJsonPath("$.sg_config.dynamic.authc.*[?(@.http_authenticator.type == 'openid')]");

            String frontendBaseUrl = oldKibanaConfig.get("searchguard.openid.base_redirect_url").asString();

            if (frontendBaseUrl == null) {
                frontendBaseUrl = getFrontendBaseUrlFromKibanaYaml();
            }

            if (oidcAuthDomains.isEmpty()) {
                return new FrontendUpdateInstructions().error(
                        "No auth domains of type 'openid' are defined in the provided sg_config.yml, even though kibana.yml is configured to use OIDC authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            List<DocNode> activeOidcAuthDomains = oidcAuthDomains.stream().filter((node) -> node.get("http_enabled") != Boolean.FALSE)
                    .collect(toList());

            if (activeOidcAuthDomains.isEmpty()) {
                return new FrontendUpdateInstructions().error(
                        "All auth domains of type 'openid' defined in sg_config.yml are disabled, even though kibana.yml is configured to use OIDC authentication. This is an invalid configuration. Please check if you have provided the correct configuration files.");
            }

            FrontendUpdateInstructions updateInstructions = new FrontendUpdateInstructions()
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

            newSgFrontendConfig.put("auth_domains", newAuthDomains);

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

        public FrontendUpdateInstructions createSgFrontendConfigJwt() throws SgctlException {

            // header is not used any more
            //String header = oldKibanaConfig.get("searchguard.jwt.header").asString();
            String urlParameter = oldKibanaConfig.get("searchguard.jwt.url_parameter").asString();
            String loginEndpoint = oldKibanaConfig.get("searchguard.jwt.login_endpoint").asString();

            FrontendUpdateInstructions updateInstructions = new FrontendUpdateInstructions();

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

                newSgFrontendConfig.put("auth_domains", Collections.singletonList(ImmutableMap.of("type", "link", "url", loginEndpoint)));

                updateInstructions.sgFrontendConfig(newSgFrontendConfig);

                return updateInstructions;
            } else {
                updateInstructions.sgFrontendConfigInstructions(
                        "In the current configuration, the Search Guard Kibana plugin does not provide a login form. The only way to login is opening a Kibana URL with the URL parameter "
                                + urlParameter
                                + ". Thus, the sg_frontend_config.yml file generated by this tool will also define no authenticators. If you want to have more login methods, you can add these to sg_frontend_config.yml.");

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

    static class BackendUpdateInstructions {
        private SgAuthc sgAuthc;
        private DocNode sgLicense;
        private DocNode sgAuthTokenService;
        private DocNode sgAuthz;
        private DocNode sgFrontendMultiTenancy;
        private Object sgAuthcTransport;

    }

    static class FrontendUpdateInstructions {

        private String mainInstructions;
        private String error;

        private String esPluginUpdateInstructions = "";

        // 1
        private String sgFrontendConfigInstructions = null;

        // 2
        private String sgFrontendConfigInstructionsAdvanced;

        // 3
        private String sgFrontendConfigInstructionsReview = "Please review the settings.";

        // unused
        private String sgFrontendConfigInstructionsTypeSpecific = null;

        private Map<String, Object> sgFrontendConfig;

        private String kibanaPluginUpdateInstructions = "After the new sg_frontend_config.yml has been successfully uploaded to Search Guard, you can update the Search Guard Kibana plugin.";
        private String kibanaConfigInstructions;
        private String kibanaConfig;

        public Map<String, Object> getSgFrontendConfig() {
            return sgFrontendConfig;
        }

        public FrontendUpdateInstructions sgFrontendConfig(Map<String, Object> sgFrontendConfig) {
            this.sgFrontendConfig = sgFrontendConfig;
            return this;
        }

        public String getKibanaConfig() {
            return kibanaConfig;
        }

        public FrontendUpdateInstructions kibanaConfig(String kibanaConfig) {
            this.kibanaConfig = kibanaConfig;
            return this;
        }

        public String getMainInstructions() {
            return mainInstructions;
        }

        public FrontendUpdateInstructions mainInstructions(String mainInstructions) {
            this.mainInstructions = mainInstructions;
            return this;

        }

        public String getSgFrontendConfigInstructions() {
            return sgFrontendConfigInstructions;
        }

        public FrontendUpdateInstructions sgFrontendConfigInstructions(String sgFrontendConfigInstructions) {
            this.sgFrontendConfigInstructions = sgFrontendConfigInstructions;
            return this;

        }

        public String getKibanaConfigInstructions() {
            return kibanaConfigInstructions;
        }

        public FrontendUpdateInstructions kibanaConfigInstructions(String kibanaConfigInstructions) {
            this.kibanaConfigInstructions = kibanaConfigInstructions;
            return this;

        }

        public String getError() {
            return error;
        }

        public FrontendUpdateInstructions error(String error) {
            this.error = error;
            return this;

        }

        public String getEsPluginUpdateInstructions() {
            return esPluginUpdateInstructions;
        }

        public FrontendUpdateInstructions esPluginUpdateInstructions(String esPluginUpdateInstructions) {
            this.esPluginUpdateInstructions = esPluginUpdateInstructions;
            return this;
        }

        public String getKibanaPluginUpdateInstructions() {
            return kibanaPluginUpdateInstructions;
        }

        public FrontendUpdateInstructions kibanaPluginUpdateInstructions(String kibanaPluginUpdateInstructions) {
            this.kibanaPluginUpdateInstructions = kibanaPluginUpdateInstructions;
            return this;
        }

        public String getSgFrontendConfigInstructionsAdvanced() {
            return sgFrontendConfigInstructionsAdvanced;
        }

        public FrontendUpdateInstructions sgFrontendConfigInstructionsAdvanced(String sgFrontendConfigInstructionsAdvanced) {
            this.sgFrontendConfigInstructionsAdvanced = sgFrontendConfigInstructionsAdvanced;
            return this;
        }

        public String getSgFrontendConfigInstructionsReview() {
            return sgFrontendConfigInstructionsReview;
        }

        public FrontendUpdateInstructions sgFrontendConfigInstructionsReview(String sgFrontendConfigInstructionsReview) {
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

    static class OldAuthDomain implements Comparable<OldAuthDomain>, Document<OldAuthDomain> {
        private final DocNode docNode;
        private final ValidatingDocNode vNode;
        private final ValidationErrors validationErrors = new ValidationErrors();
        private final String id;
        private int order;
        private String oldFrontendType;
        private String oldBackendType;
        private String newFrontendType;
        private String newBackendType;
        private List<String> skipUsers;
        private List<String> acceptIps;

        DocNode oldFrontendConfig;
        DocNode oldBackendConfig;

        OldAuthDomain(String id, DocNode docNode) {
            this.id = id;
            this.docNode = docNode;
            this.vNode = new ValidatingDocNode(docNode, validationErrors);
            this.order = vNode.get("order").withDefault(0).asInt();

            oldFrontendType = vNode.get("http_authenticator.type").asString();
            oldBackendType = vNode.get("authentication_backend.type").withDefault("intern").asString();

            if (oldBackendType.equals("noop")) {
                newBackendType = null;
            } else if (oldBackendType.equals("intern") || oldBackendType.equals("internal")) {
                newBackendType = "internal_users_db";
            } else {
                newBackendType = oldBackendType;
            }

            newFrontendType = oldFrontendType;

            oldFrontendConfig = vNode.get("http_authenticator.config").asDocNode();
            oldBackendConfig = vNode.get("authentication_backend.config").asDocNode();

            skipUsers = vNode.get("skip_users").asListOfStrings();
            acceptIps = vNode.get("enabled_only_for_ips").asListOfStrings();

        }

        List<NewAuthDomain> toNewAuthDomains(List<UserInformationBackend> userInformationBackends) {
            if (oldFrontendType.equals("saml") || oldFrontendType.equals("openid")) {
                return Collections.emptyList();
            }

            NewAuthDomain newFrontendConfig = toNewFrontendConfig();

            if ("ldap".equalsIgnoreCase(oldBackendType)) {
                return addNewLdapBackendConfig(newFrontendConfig, userInformationBackends);
            } else {
                return Collections.singletonList(addNewBackendConfig(newFrontendConfig, userInformationBackends));
            }

        }

        NewAuthDomain toNewFrontendConfig() {
            NewAuthDomain result = new NewAuthDomain(newFrontendType, newBackendType, skipUsers, acceptIps, null, null);
            switch (oldFrontendType.toLowerCase()) {
            case "kerberos":
            case "basic":
                return result;
            case "jwt": {
                LinkedHashMap<String, Object> newConfig = new LinkedHashMap<>();
                String signingKey = vNode.get("http_authenticator.config.signing_key").required().asString();

                if (signingKey != null) {
                    byte[] decoded;

                    try {
                        decoded = Base64.getDecoder()
                                .decode(signingKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("-----END PUBLIC KEY-----", "").trim());
                    } catch (Exception e) {
                        validationErrors.add(
                                new ValidationError("http_authenticator.config.signing_key", "Unsupported encoding: " + e.getMessage()).cause(e));
                        return result;
                    }
                    try {
                        getPublicKey(decoded, "RSA");
                        // This is an RSA key

                        newConfig.put("signing", ImmutableMap.of("rsa", ImmutableMap.of("public_key", signingKey)));
                    } catch (Exception e) {
                        try {
                            getPublicKey(decoded, "EC");
                            // This is an EC key

                            newConfig.put("signing", ImmutableMap.of("ec", ImmutableMap.of("public_key", signingKey)));

                        } catch (Exception e2) {
                            // This is an HMAC key. Using this makes no sense as it is not secure. No point in supporting it.

                            validationErrors.add(new ValidationError("http_authenticator.config.signing_key", "Unsupported key").cause(e2));
                        }
                    }
                }

                if (oldFrontendConfig.hasNonNull("jwt_header")) {
                    newConfig.put("header", oldFrontendConfig.get("jwt_header"));
                }

                if (oldFrontendConfig.hasNonNull("jwt_url_parameter")) {
                    newConfig.put("url_parameter", oldFrontendConfig.get("jwt_url_parameter"));
                }

                if (oldFrontendConfig.hasNonNull("required_audience")) {
                    newConfig.put("required_audience", oldFrontendConfig.get("required_audience"));
                }

                if (oldFrontendConfig.hasNonNull("required_issuer")) {
                    newConfig.put("required_issuer", oldFrontendConfig.get("required_issuer"));
                }

                if (oldFrontendConfig.hasNonNull("subject_key")) {
                    result.userMappingUserName.put("from", "$[\"jwt\"][\"" + oldFrontendConfig.get("subject_key") + "\"]");
                }

                if (oldFrontendConfig.hasNonNull("subject_path")) {
                    result.userMappingUserName.put("from", addPrefixToJsonPath("jwt", oldFrontendConfig.get("subject_path").toString()));
                }

                if (oldFrontendConfig.hasNonNull("roles_key")) {
                    result.userMappingRoles.put("from_comma_separated_string", "$[\"jwt\"][\"" + oldFrontendConfig.get("roles_key") + "\"]");
                }

                if (oldFrontendConfig.hasNonNull("roles_path")) {
                    result.userMappingRoles.put("from_comma_separated_string",
                            addPrefixToJsonPath("jwt", oldFrontendConfig.get("roles_path").toString()));
                }

                if (oldFrontendConfig.hasNonNull("map_claims_to_user_attrs")) {
                    LinkedHashMap<String, String> attrsFrom = new LinkedHashMap<>();

                    for (Map.Entry<String, ?> entry : oldFrontendConfig.getAsNode("map_claims_to_user_attrs").entrySet()) {
                        attrsFrom.put(entry.getKey(), addPrefixToJsonPath("jwt", entry.getValue().toString()));
                    }

                    result.userMappingAttributes.put("from", attrsFrom);
                }

                result.frontendConfig = newConfig;

                return result;
            }
            case "clientcert": {
                String userNameAttribute = oldFrontendConfig.getAsString("username_attribute");

                if (userNameAttribute != null) {
                    result.userMappingUserName.put("from", "clientcert.subject." + userNameAttribute);
                }

                return result;
            }
            case "proxy": {
                result.frontendType = "trusted_origin";

                if (oldFrontendConfig.hasNonNull("user_header")) {
                    result.userMappingUserName.put("from", "$.request.headers[\"" + oldFrontendConfig.getAsString("user_header") + "\"]");
                }

                if (oldFrontendConfig.hasNonNull("roles_header")) {
                    if (oldFrontendConfig.hasNonNull("roles_separator")) {
                        result.userMappingRoles.put("from",
                                ImmutableMap.of("json_path", "$.request.headers[\"" + oldFrontendConfig.getAsString("roles_header") + "\"]", "split",
                                        oldFrontendConfig.getAsString("roles_separator")));
                    } else {
                        result.userMappingRoles.put("from_comma_separated_string",
                                "$.request.headers[\"" + oldFrontendConfig.getAsString("roles_header") + "\"]");
                    }
                }
                return result;
            }
            case "proxy2": {
                if (oldFrontendConfig.hasNonNull("user_header")) {
                    result.userMappingUserName.put("from", "$.request.headers[\"" + oldFrontendConfig.getAsString("user_header") + "\"]");
                }

                if (oldFrontendConfig.hasNonNull("roles_header")) {
                    if (oldFrontendConfig.hasNonNull("roles_separator")) {
                        result.userMappingRoles.put("from",
                                ImmutableMap.of("json_path", "$.request.headers[\"" + oldFrontendConfig.getAsString("roles_header") + "\"]", "split",
                                        oldFrontendConfig.getAsString("roles_separator")));
                    } else {
                        result.userMappingRoles.put("from_comma_separated_string",
                                "$.request.headers[\"" + oldFrontendConfig.getAsString("roles_header") + "\"]");
                    }
                }

                String authMode = oldFrontendConfig.hasNonNull("auth_mode") ? oldFrontendConfig.getAsString("auth_mode").toLowerCase() : "both";

                switch (authMode) {
                case "ip": {
                    result.frontendType = "trusted_origin";
                    return result;
                }
                case "cert": {
                    result.frontendType = "clientcert";
                    return result;
                }
                case "both":
                case "either": {
                    validationErrors.add(new ValidationError("http_authenticator.config.type",
                            "The proxy2 authenticator cannot be automatically converted when auth_mode " + authMode
                                    + " is used. Please check the documentation."));
                    return result;
                }
                default: {
                    validationErrors.add(new ValidationError("http_authenticator.config.auth_mode", "Invalid auth_mode " + authMode));
                    return result;
                }
                }

            }
            default:
                validationErrors.add(new ValidationError("http_authenticator.type", "Unknown HTTP authenticator" + oldFrontendType));
                return result;
            }
        }

        NewAuthDomain addNewBackendConfig(NewAuthDomain result, List<UserInformationBackend> userInformationBackends) {
            switch (oldBackendType.toLowerCase()) {
            case "intern":
            case "internal": {
                result.backendType = "internal_users_db";
                result.userInformationBackends = userInformationBackends;
                LinkedHashMap<String, Object> newConfig = new LinkedHashMap<>();

                if (oldFrontendConfig.hasNonNull("map_db_attrs_to_user_attrs")) {
                    LinkedHashMap<String, String> attrsFrom = new LinkedHashMap<>();

                    for (Map.Entry<String, ?> entry : oldFrontendConfig.getAsNode("map_claims_to_user_attrs").entrySet()) {
                        attrsFrom.put(entry.getKey(), addPrefixToJsonPath("user_entry.attributes", entry.getValue().toString()));
                    }

                    result.userMappingAttributes.put("from", attrsFrom);
                }

                result.backendConfig = newConfig;

                if (userInformationBackends != null && userInformationBackends.size() != 0) {
                    List<String> roleMapping = UserInformationBackend.mergedRoleMappingFrom(userInformationBackends);

                    if (roleMapping.size() != 0) {
                        result.userMappingRoles.put("from", roleMapping);
                    }
                }

                return result;
            }
            case "noop": {
                result.backendType = null;
                result.userInformationBackends = userInformationBackends;

                if (userInformationBackends != null && userInformationBackends.size() != 0) {
                    List<String> roleMapping = UserInformationBackend.mergedRoleMappingFrom(userInformationBackends);

                    if (roleMapping.size() != 0) {
                        result.userMappingRoles.put("from", roleMapping);
                    }
                }

                return result;
            }
            default: {
                validationErrors.add(new ValidationError("authentication_backend.type", "Unknown authentication backend type" + oldBackendType));
                return result;
            }
            }

        }

        List<NewAuthDomain> addNewLdapBackendConfig(NewAuthDomain result, List<UserInformationBackend> userInformationBackends) {
            LinkedHashMap<String, Object> newConfig = new LinkedHashMap<>();
            LinkedHashMap<String, Object> idpConfig = new LinkedHashMap<>();

            boolean ssl = vNode.get("authentication_backend.config.enable_ssl").asBoolean();

            idpConfig.put("hosts", oldBackendConfig.getAsListOfStrings("hosts").stream()
                    .map(s -> ssl ? (s.startsWith("ldaps://") ? s : ("ldaps://" + s)) : s).collect(Collectors.toList()));

            if (oldBackendConfig.hasNonNull("bind_dn")) {
                idpConfig.put("bind_dn", oldBackendConfig.getAsListOfStrings("bind_dn"));
            }

            if (oldBackendConfig.hasNonNull("password")) {
                idpConfig.put("password", oldBackendConfig.getAsListOfStrings("password"));
            }

            LinkedHashMap<String, Object> tlsConfig = new LinkedHashMap<>();

            boolean startTls = vNode.get("authentication_backend.config.enable_start_tls").asBoolean();
            boolean clientAuthEnabled = vNode.get("authentication_backend.config.enable_ssl_client_auth").asBoolean();
            boolean verifyHostnames = vNode.get("authentication_backend.config.verify_hostnames").withDefault(true).asBoolean();
            String pemTrustedCasFile = vNode.get("authentication_backend.config.pemtrustedcas_filepath").asString();
            String pemTrustedCasContent = vNode.get("authentication_backend.config.pemtrustedcas_content").asString();
            String clientAuthKeyFile = vNode.get("authentication_backend.config.pemkey_filepath").asString();
            String clientAuthKeyPassword = vNode.get("authentication_backend.config.pemkey_password").asString();
            String clientAuthCertFile = vNode.get("authentication_backend.config.pemcert_filepath").asString();
            String clientAuthKeyContent = vNode.get("authentication_backend.config.pemkey_content").asString();
            String clientAuthCertContent = vNode.get("authentication_backend.config.pemcert_content").asString();

            if (startTls) {
                tlsConfig.put("start_tls", startTls);
            }

            if (!verifyHostnames) {
                tlsConfig.put("verify_hostnames", verifyHostnames);
            }

            if (pemTrustedCasContent != null) {
                tlsConfig.put("trusted_cas", pemTrustedCasContent);
            } else if (pemTrustedCasFile != null) {
                tlsConfig.put("trusted_cas", "#{file:" + pemTrustedCasFile + "}");
            }

            if (clientAuthEnabled) {
                LinkedHashMap<String, Object> clientAuth = new LinkedHashMap<>();

                if (clientAuthKeyFile != null) {
                    clientAuth.put("private_key", "#{file:" + clientAuthKeyFile + "}");
                } else if (clientAuthKeyContent != null) {
                    clientAuth.put("private_key", clientAuthKeyFile);
                }

                if (clientAuthCertFile != null) {
                    clientAuth.put("certificate", "#{file:" + clientAuthCertFile + "}");
                } else if (clientAuthCertContent != null) {
                    clientAuth.put("certificate", clientAuthCertContent);
                }

                if (clientAuthKeyPassword != null) {
                    clientAuth.put("private_key_password", clientAuthKeyPassword);
                }

                tlsConfig.put("client_auth", clientAuth);
            }

            if (!tlsConfig.isEmpty()) {
                idpConfig.put("tls", tlsConfig);
            }

            newConfig.put("idp", idpConfig);

            LinkedHashMap<String, Object> userSearch = new LinkedHashMap<>();

            userSearch.put("base_dn", oldBackendConfig.getAsString("userbase"));

            if (oldBackendConfig.hasNonNull("usersearch")) {
                userSearch.put("filter", ImmutableMap.of("raw", oldBackendConfig.getAsString("usersearch").replace("{0}", "${user.name}")));
            }

            newConfig.put("user_search", userSearch);

            result.backendConfig = newConfig;

            // TODO
            //if (oldBackendConfig.hasNonNull("username_attribute")) {
            //    result.userMappingUserName.
            //}

            if (oldBackendConfig.hasNonNull("map_ldap_attrs_to_user_attrs")) {
                LinkedHashMap<String, String> attrsFrom = new LinkedHashMap<>();

                for (Map.Entry<String, ?> entry : oldFrontendConfig.getAsNode("map_ldap_attrs_to_user_attrs").entrySet()) {
                    attrsFrom.put(entry.getKey(), addPrefixToJsonPath("ldap_user_entry", entry.getValue().toString()));
                }

                result.userMappingAttributes.put("from", attrsFrom);
            }

            if (oldBackendConfig.hasNonNull("users")) {
                return addNewLdapBackendConfigMultiBase(result, userInformationBackends);
            } else {
                if (userInformationBackends != null && userInformationBackends.size() > 0) {
                    List<String> roleMapping = UserInformationBackend.mergedRoleMappingFrom(userInformationBackends);

                    if (roleMapping.size() != 0) {
                        result.userMappingRoles.put("from", roleMapping);
                    }

                    List<UserInformationBackend> ldapUserInformationBackendsForMerging = userInformationBackends.stream()
                            .filter((b) -> b.type.equals("ldap")
                                    && (!b.backendConfig.containsKey("user_search") || b.backendConfig.get("user_search").equals(userSearch))
                                    && (b.backendConfig.containsKey("idp") && b.backendConfig.get("idp").equals(idpConfig)))
                            .collect(Collectors.toList());
                    List<UserInformationBackend> otherInformationBackends = userInformationBackends.stream()
                            .filter((b) -> !ldapUserInformationBackendsForMerging.contains(b)).collect(Collectors.toList());


                    if (ldapUserInformationBackendsForMerging.size() == 1) {
                        // Just merge the group search into this one

                        UserInformationBackend ldapGroupSearch = ldapUserInformationBackendsForMerging.get(0);

                        if (ldapGroupSearch.backendConfig.containsKey("group_search")) {
                            newConfig.put("group_search", ldapGroupSearch.backendConfig.get("group_search"));
                        }

                        result.userInformationBackends = otherInformationBackends;
                    } else {
                        result.userInformationBackends = userInformationBackends;
                    }

                }

                return Collections.singletonList(result);
            }
        }

        List<NewAuthDomain> addNewLdapBackendConfigMultiBase(NewAuthDomain baseAuthDomain, List<UserInformationBackend> userInformationBackends) {
            Map<String, DocNode> users = oldBackendConfig.getAsNode("users").toMapOfNodes();
            ArrayList<NewAuthDomain> result = new ArrayList<>();

            for (Map.Entry<String, DocNode> entry : users.entrySet()) {
                NewAuthDomain authDomain = baseAuthDomain.clone();
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> newConfig = (LinkedHashMap<String, Object>) Document.toDeepBasicObject(baseAuthDomain.backendConfig);
                @SuppressWarnings("unchecked")
                Map<String, Object> userSearch = (Map<String, Object>) newConfig.computeIfAbsent("user_search",
                        (k) -> new LinkedHashMap<String, Object>());

                userSearch.put("base_dn", entry.getValue().getAsString("base"));

                if (entry.getValue().hasNonNull("search")) {
                    userSearch.put("filter", ImmutableMap.of("raw", entry.getValue().getAsString("search").replace("{0}", "${user.name}")));
                }

                if (userInformationBackends != null && userInformationBackends.size() > 0) {
                    List<String> roleMapping = UserInformationBackend.mergedRoleMappingFrom(userInformationBackends);

                    if (roleMapping.size() != 0) {
                        authDomain.userMappingRoles.put("from", roleMapping);
                    }

                    List<UserInformationBackend> ldapUserInformationBackendsForMerging = userInformationBackends.stream()
                            .filter((b) -> b.type.equals("ldap")
                                    && (!b.backendConfig.containsKey("user_search") || b.backendConfig.get("user_search").equals(userSearch))
                                    && (b.backendConfig.containsKey("idp")
                                            && b.backendConfig.get("idp").equals(baseAuthDomain.backendConfig.get("idp"))))
                            .collect(Collectors.toList());
                    List<UserInformationBackend> otherInformationBackends = userInformationBackends.stream()
                            .filter((b) -> !ldapUserInformationBackendsForMerging.contains(b)).collect(Collectors.toList());

                    if (ldapUserInformationBackendsForMerging.size() == 1) {
                        // Just merge the group search into this one

                        UserInformationBackend ldapGroupSearch = ldapUserInformationBackendsForMerging.get(0);

                        if (ldapGroupSearch.backendConfig.containsKey("group_search")) {
                            newConfig.put("group_search", ldapGroupSearch.backendConfig.get("group_search"));
                        }

                        authDomain.userInformationBackends = otherInformationBackends;
                    } else {
                        authDomain.userInformationBackends = userInformationBackends;
                    }

                }

                authDomain.backendConfig = newConfig;
                result.add(authDomain);
            }

            return result;
        }

        private static String addPrefixToJsonPath(String prefix, String jsonPath) {
            if (jsonPath.startsWith("$.")) {
                return "$." + prefix + jsonPath.substring(1);
            } else if (jsonPath.startsWith("$[")) {
                return "$." + prefix + jsonPath.substring(1);
            } else {
                return "$." + prefix + "." + jsonPath;
            }
        }

        @Override
        public int compareTo(OldAuthDomain o) {
            return order - o.order;
        }

        @Override
        public Object toBasicObject() {
            return docNode;
        }

        private static PublicKey getPublicKey(final byte[] keyBytes, final String algo) throws NoSuchAlgorithmException, InvalidKeySpecException {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(algo);
            return kf.generatePublic(spec);
        }

    }

    static class NewAuthDomain implements Document<NewAuthDomain> {

        private String frontendType;
        private String backendType;
        private List<String> skipUsers;
        private List<String> acceptIps;
        Map<String, Object> frontendConfig;
        Map<String, Object> backendConfig;
        private LinkedHashMap<String, Object> userMappingUserName = new LinkedHashMap<>();
        private LinkedHashMap<String, Object> userMappingRoles = new LinkedHashMap<>();
        private LinkedHashMap<String, Object> userMappingAttributes = new LinkedHashMap<>();
        private List<UserInformationBackend> userInformationBackends;

        public NewAuthDomain(String frontendType, String backendType, List<String> skipUsers, List<String> acceptIps,
                Map<String, Object> frontendConfig, Map<String, Object> backendConfig) {
            this.frontendType = frontendType;
            this.backendType = backendType;
            this.skipUsers = skipUsers;
            this.acceptIps = acceptIps;
            this.frontendConfig = frontendConfig;
            this.backendConfig = backendConfig;
        }

        @Override
        public Object toBasicObject() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();

            result.put("type", backendType != null ? (frontendType + "/" + backendType) : frontendType);

            if (skipUsers != null && skipUsers.size() != 0) {
                result.put("skip", ImmutableMap.of("users", skipUsers));
            }

            if (acceptIps != null && acceptIps.size() != 0) {
                result.put("accept", ImmutableMap.of("ips", acceptIps));
            }

            if (frontendConfig != null && frontendConfig.size() != 0) {
                result.put(frontendType, frontendConfig);
            }

            if (backendConfig != null && backendConfig.size() != 0) {
                result.put(backendType, backendConfig);
            }

            if (userInformationBackends != null && userInformationBackends.size() != 0) {
                result.put("additional_user_information", userInformationBackends);
            }

            if (userMappingUserName.size() != 0 || userMappingRoles.size() != 0 || userMappingAttributes.size() != 0) {
                LinkedHashMap<String, Object> userMapping = new LinkedHashMap<String, Object>();

                if (userMappingUserName.size() != 0) {
                    userMapping.put("user_name", userMappingUserName);
                }

                if (userMappingRoles.size() != 0) {
                    userMapping.put("roles", userMappingRoles);
                }

                if (userMappingAttributes.size() != 0) {
                    userMapping.put("attributes", userMappingAttributes);
                }

                result.put("user_mapping", userMapping);

            }

            return result;
        }

        public NewAuthDomain clone() {
            NewAuthDomain result = new NewAuthDomain(frontendType, backendType, skipUsers, acceptIps, frontendConfig, backendConfig);
            result.userMappingUserName = new LinkedHashMap<>(userMappingUserName);
            result.userMappingRoles = new LinkedHashMap<>(userMappingRoles);
            result.userMappingAttributes = new LinkedHashMap<>(userMappingAttributes);
            result.userInformationBackends = userInformationBackends != null ? new ArrayList<>(userInformationBackends) : null;
            return result;
        }
    }

    static class AuthzDomain {
        private final ValidatingDocNode vNode;
        private final ValidationErrors validationErrors = new ValidationErrors();
        private DocNode config;
        private String oldType;

        AuthzDomain(DocNode config) {
            this.config = config;
            this.vNode = new ValidatingDocNode(config, validationErrors);
            this.oldType = vNode.get("authorization_backend.type").required().asString();
        }

        @SuppressWarnings("unchecked")
        List<UserInformationBackend> toUserInformationBackends() {

            if (oldType == null) {
                return Collections.emptyList();
            }

            switch (oldType) {
            case "intern":
            case "internal":
                return Collections.singletonList(new UserInformationBackend("internal_users_db"));
            case "ldap": {
                UserInformationBackend result = new UserInformationBackend("ldap");
                LinkedHashMap<String, Object> newConfig = new LinkedHashMap<>();
                LinkedHashMap<String, Object> idpConfig = new LinkedHashMap<>();

                boolean ssl = vNode.get("authorization_backend.config.enable_ssl").asBoolean();

                idpConfig.put("hosts", vNode.get("authorization_backend.config.hosts").required().asList().withEmptyListAsDefault().ofStrings()
                        .stream().map(s -> ssl ? (s.startsWith("ldaps://") ? s : ("ldaps://" + s)) : s).collect(Collectors.toList()));

                DocNode oldBackendConfig = config.getAsNode("authorization_backend", "config");

                if (oldBackendConfig.hasNonNull("bind_dn")) {
                    idpConfig.put("bind_dn", oldBackendConfig.getAsListOfStrings("bind_dn"));
                }

                if (oldBackendConfig.hasNonNull("password")) {
                    idpConfig.put("password", oldBackendConfig.getAsListOfStrings("password"));
                }

                LinkedHashMap<String, Object> tlsConfig = new LinkedHashMap<>();

                boolean startTls = vNode.get("authorization_backend.config.enable_start_tls").asBoolean();
                boolean clientAuthEnabled = vNode.get("authorization_backend.config.enable_ssl_client_auth").asBoolean();
                boolean verifyHostnames = vNode.get("authorization_backend.config.verify_hostnames").withDefault(true).asBoolean();
                String pemTrustedCasFile = vNode.get("authorization_backend.config.pemtrustedcas_filepath").asString();
                String pemTrustedCasContent = vNode.get("authorization_backend.config.pemtrustedcas_content").asString();
                String clientAuthKeyFile = vNode.get("authorization_backend.config.pemkey_filepath").asString();
                String clientAuthKeyPassword = vNode.get("authorization_backend.config.pemkey_password").asString();
                String clientAuthCertFile = vNode.get("authorization_backend.config.pemcert_filepath").asString();
                String clientAuthKeyContent = vNode.get("authorization_backend.config.pemkey_content").asString();
                String clientAuthCertContent = vNode.get("authorization_backend.config.pemcert_content").asString();

                if (startTls) {
                    tlsConfig.put("start_tls", startTls);
                }

                if (!verifyHostnames) {
                    tlsConfig.put("verify_hostnames", verifyHostnames);
                }

                if (pemTrustedCasContent != null) {
                    tlsConfig.put("trusted_cas", pemTrustedCasContent);
                } else if (pemTrustedCasFile != null) {
                    tlsConfig.put("trusted_cas", "#{file:" + pemTrustedCasFile + "}");
                }

                if (clientAuthEnabled) {
                    LinkedHashMap<String, Object> clientAuth = new LinkedHashMap<>();

                    if (clientAuthKeyFile != null) {
                        clientAuth.put("private_key", "#{file:" + clientAuthKeyFile + "}");
                    } else if (clientAuthKeyContent != null) {
                        clientAuth.put("private_key", clientAuthKeyFile);
                    }

                    if (clientAuthCertFile != null) {
                        clientAuth.put("certificate", "#{file:" + clientAuthCertFile + "}");
                    } else if (clientAuthCertContent != null) {
                        clientAuth.put("certificate", clientAuthCertContent);
                    }

                    if (clientAuthKeyPassword != null) {
                        clientAuth.put("private_key_password", clientAuthKeyPassword);
                    }

                    tlsConfig.put("client_auth", clientAuth);
                }

                if (!tlsConfig.isEmpty()) {
                    idpConfig.put("tls", tlsConfig);
                }

                newConfig.put("idp", idpConfig);

                LinkedHashMap<String, Object> userSearch = new LinkedHashMap<>();

                userSearch.put("base_dn", oldBackendConfig.getAsString("userbase"));

                if (oldBackendConfig.hasNonNull("usersearch")) {
                    userSearch.put("filter", ImmutableMap.of("raw", oldBackendConfig.getAsString("usersearch").replace("{0}", "${user.name}")));
                }

                newConfig.put("user_search", userSearch);

                if (oldBackendConfig.hasNonNull("userrolename")) {
                    ((List<String>) result.userMappingRoles.computeIfAbsent("from", (k) -> new ArrayList<>()))
                            .add("$.ldap_user_entry[\"" + oldBackendConfig.getAsString("userrolename") + "\"]");
                }

                LinkedHashMap<String, Object> groupSearch = new LinkedHashMap<>();

                if (oldBackendConfig.hasNonNull("rolebase")) {
                    groupSearch.put("base_dn", oldBackendConfig.getAsString("rolebase"));
                }

                if (oldBackendConfig.hasNonNull("rolesearch")) {
                    groupSearch.put("filter", ImmutableMap.of("raw", oldBackendConfig.getAsString("usersearch").replace("{0}", "${dn}")));
                }

                if (oldBackendConfig.hasNonNull("rolename")) {
                    groupSearch.put("role_name_attribute", oldBackendConfig.getAsString("rolename"));
                }

                LinkedHashMap<String, Object> recursive = new LinkedHashMap<>();

                if (oldBackendConfig.hasNonNull("resolve_nested_roles") && Boolean.TRUE.equals(oldBackendConfig.get("resolve_nested_roles"))) {
                    recursive.put("enabled", true);
                }

                if (oldBackendConfig.hasNonNull("nested_role_filter")) {
                    validationErrors.add(new ValidationError("authorization_backend.config.nested_role_filter",
                            "nested_role_filter is not directly supported any more. You can use group_search.recursive.enabled_for, which is the opposite: A pattern of group dns for which group search shall be performed"));
                }

                if (!recursive.isEmpty()) {
                    groupSearch.put("recursive", recursive);
                }

                if (!groupSearch.isEmpty()) {
                    newConfig.put("group_search", groupSearch);
                }

                newConfig.put("group_search", groupSearch);

                result.backendConfig = newConfig;

                if (oldBackendConfig.hasNonNull("users")) {
                    List<UserInformationBackend> multiResult = addNewLdapBackendConfigMultiUserBase(result);

                    if (oldBackendConfig.hasNonNull("roles")) {
                        ArrayList<UserInformationBackend> multiMultiResult = new ArrayList<>();

                        for (UserInformationBackend resultElement : multiResult) {
                            multiMultiResult.addAll(addNewLdapBackendConfigMultiGroupBase(resultElement));
                        }

                        return multiMultiResult;
                    } else {
                        return multiResult;
                    }

                } else {
                    if (oldBackendConfig.hasNonNull("roles")) {
                        return addNewLdapBackendConfigMultiGroupBase(result);
                    } else {
                        return Collections.singletonList(result);
                    }
                }
            }
            default:
                validationErrors.add(new ValidationError(null, "Unknown authorization backend " + oldType));
                return Collections.emptyList();
            }

        }

        List<UserInformationBackend> addNewLdapBackendConfigMultiUserBase(UserInformationBackend baseAuthDomain) {
            Map<String, DocNode> users = config.getAsNode("authorization_backend", "config", "users").toMapOfNodes();
            ArrayList<UserInformationBackend> result = new ArrayList<>();

            for (Map.Entry<String, DocNode> entry : users.entrySet()) {
                UserInformationBackend authDomain = baseAuthDomain.clone();
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> newConfig = (LinkedHashMap<String, Object>) Document.toDeepBasicObject(baseAuthDomain.backendConfig);
                @SuppressWarnings("unchecked")
                Map<String, Object> userSearch = (Map<String, Object>) newConfig.computeIfAbsent("user_search",
                        (k) -> new LinkedHashMap<String, Object>());

                userSearch.put("base_dn", entry.getValue().getAsString("base"));

                if (entry.getValue().hasNonNull("search")) {
                    userSearch.put("filter", ImmutableMap.of("raw", entry.getValue().getAsString("base").replace("{0}", "${user.name}")));
                }

                authDomain.backendConfig = newConfig;
                result.add(authDomain);
            }

            return result;
        }

        List<UserInformationBackend> addNewLdapBackendConfigMultiGroupBase(UserInformationBackend baseAuthDomain) {
            Map<String, DocNode> users = config.getAsNode("authorization_backend", "config", "roles").toMapOfNodes();

            ArrayList<UserInformationBackend> result = new ArrayList<>();

            for (Map.Entry<String, DocNode> entry : users.entrySet()) {
                UserInformationBackend authDomain = baseAuthDomain.clone();
                @SuppressWarnings("unchecked")
                LinkedHashMap<String, Object> newConfig = (LinkedHashMap<String, Object>) Document.toDeepBasicObject(baseAuthDomain.backendConfig);
                @SuppressWarnings("unchecked")
                Map<String, Object> userSearch = (Map<String, Object>) newConfig.computeIfAbsent("group_search",
                        (k) -> new LinkedHashMap<String, Object>());

                userSearch.put("base_dn", entry.getValue().getAsString("base"));

                if (entry.getValue().hasNonNull("search")) {
                    userSearch.put("filter", ImmutableMap.of("raw", entry.getValue().getAsString("search").replace("{0}", "${dn}")));
                }

                authDomain.backendConfig = newConfig;
                result.add(authDomain);
            }

            return result;
        }
    }

    static class UserInformationBackend implements Document<UserInformationBackend> {
        private String type;
        private Map<String, Object> backendConfig = new LinkedHashMap<>();
        private Map<String, Object> userMappingUserName = new LinkedHashMap<>();
        private Map<String, Object> userMappingRoles = new LinkedHashMap<>();
        private Map<String, Object> userMappingAttributes = new LinkedHashMap<>();

        UserInformationBackend(String type) {
            this.type = type;
        }

        @Override
        public Object toBasicObject() {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("type", type);
            result.put(type, backendConfig);
            return result;
        }

        public UserInformationBackend clone() {
            UserInformationBackend result = new UserInformationBackend(type);
            result.userMappingUserName.putAll(userMappingUserName);
            result.userMappingRoles.putAll(userMappingRoles);
            result.userMappingAttributes.putAll(userMappingAttributes);
            result.backendConfig.putAll(backendConfig);
            return result;
        }

        static List<String> mergedRoleMappingFrom(List<UserInformationBackend> backends) {
            if (backends == null || backends.isEmpty()) {
                return Collections.emptyList();
            }

            LinkedHashSet<String> result = new LinkedHashSet<>();

            for (UserInformationBackend backend : backends) {
                Object from = backend.userMappingRoles.get("from");

                if (from instanceof String) {
                    result.add((String) from);
                } else if (from instanceof List) {
                    ((List<?>) from).forEach((e) -> result.add(String.valueOf(e)));
                }
            }

            return new ArrayList<>(result);
        }
    }

    static class SgAuthc implements Document<SgAuthc> {
        private List<NewAuthDomain> authDomains;
        private String internalProxies = null;
        private String remoteIpHeader = null;

        @Override
        public Object toBasicObject() {
            Map<String, Object> result = new LinkedHashMap<>();

            result.put("auth_domains", authDomains);

            if (internalProxies != null || remoteIpHeader != null) {

                Map<String, Object> network = new LinkedHashMap<>();

                if (internalProxies != null) {
                    network.put("trusted_proxies_regex", internalProxies);
                }

                if (remoteIpHeader != null) {
                    network.put("http", ImmutableMap.of("remote_ip_header", remoteIpHeader));
                }

                result.put("network", network);
            }

            return result;
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

    private static final Map<String, Object> SG_META = ImmutableMap.of();
}
