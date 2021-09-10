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

import java.io.File;
import java.net.SocketException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpHost;

import com.floragunn.codova.config.net.TLSConfig;
import com.floragunn.codova.validation.ConfigValidationException;
import com.floragunn.codova.validation.errors.MissingAttribute;
import com.floragunn.searchguard.sgctl.SgctlConfig;
import com.floragunn.searchguard.sgctl.SgctlException;
import com.floragunn.searchguard.sgctl.client.ApiException;
import com.floragunn.searchguard.sgctl.client.FailedConnectionException;
import com.floragunn.searchguard.sgctl.client.InvalidResponseException;
import com.floragunn.searchguard.sgctl.client.SearchGuardRestClient;
import com.floragunn.searchguard.sgctl.client.ServiceUnavailableException;
import com.floragunn.searchguard.sgctl.client.UnauthorizedException;
import com.floragunn.searchguard.sgctl.client.api.AuthInfoResponse;

import picocli.CommandLine.Option;

public abstract class ConnectingCommand extends BaseCommand {

    @Option(names = { "-h", "--host" }, description = "Hostname of the node to connect to")
    String host;

    @Option(names = { "-E", "--cert" }, description = "Client certificate for admin authentication")
    File clientCert;

    @Option(names = { "-p", "--port" }, description = "REST port to connect to. Default: 9200")
    Integer serverPort;

    @Option(names = { "--key" }, description = "Private key for admin authentication")
    File clientKey;

    @Option(names = { "--key-pass" }, description = "Password for private key", arity = "0..1", interactive = true)
    String clientKeyPass;

    @Option(names = { "--ca-cert" }, description = "Trusted certificates")
    File caCert;

    @Option(names = { "-k", "--insecure" }, arity = "0..1", description = "Do not verify the hostname when connecting to the cluster")
    Boolean insecure;

    @Option(names = { "--ciphers" }, description = "The ciphers to be allowed for the TLS connection to the cluster", arity = "0..*")
    List<String> ciphers;

    @Option(names = { "--tls" }, description = "The TLS version to use when connecting to the cluster")
    String tls;

    public SearchGuardRestClient getClient() throws SgctlException {

        try {
            SgctlConfig.Cluster clusterConfig = getSelectedClusterConfig();
            TLSConfig tlsConfig = getTlsConfig(clusterConfig);

            String server = getHost();
            Integer serverPort = this.serverPort;

            if (clusterConfig != null && server == null) {
                server = clusterConfig.getServer();
            }

            if (clusterConfig != null && serverPort == null) {
                serverPort = clusterConfig.getPort();
            }

            if (server == null) {
                throw new SgctlException("You must specify the server on the command line");
            }

            if (serverPort == null) {
                serverPort = 9200;
            }

            if (verbose) {
                System.out.println("Connecting to " + server + ":" + serverPort + " with certificate "
                        + getCertificateInfo(tlsConfig.getClientCertAuthConfig().getCertificateChain()));
            }

            try {
                SearchGuardRestClient client = new SearchGuardRestClient(new HttpHost(server, serverPort, "https"), tlsConfig);
                client.debug(debug);

                AuthInfoResponse authInfoResponse = client.authInfo();
                System.out.println("Successfully connected to " + server + " as user " + authInfoResponse.getUserName());
                return client;
            } catch (FailedConnectionException e) {
                throw new SgctlException(getHumanReadableErrorMessage(e), e);
            } catch (InvalidResponseException | ApiException e) {
                throw new SgctlException("Invalid response from server: " + e.getMessage(), e);
            } catch (ServiceUnavailableException e) {
                throw new SgctlException("Server is unavailable: " + e.getMessage(), e);
            } catch (UnauthorizedException e) {
                throw new SgctlException("Server rejected request as unauthorized. Please check the client certificate.", e);
            }

        } catch (ConfigValidationException e) {
            throw new SgctlException("Connection settings are invalid:\n" + e.getValidationErrors(), e).debugDetail(e.toDebugString());
        }
    }

    protected TLSConfig getTlsConfig(SgctlConfig.Cluster clusterConfig) throws ConfigValidationException {
        if (clusterConfig != null && clientCert == null && clientKey == null && clientKeyPass == null && caCert == null && insecure == null) {
            // 1st case: No command line config -> Use existing cluster config

            return clusterConfig.getTlsConfig();
        } else if (clusterConfig == null) {
            // 2nd case: Only command line config -> Use this
            
            if (clientCert == null) {
                validationErrors.add(new MissingAttribute("--cert"));
            }
            
            if (clientKey == null) {
                validationErrors.add(new MissingAttribute("--key"));                
            }
            
            validationErrors.throwExceptionForPresentErrors();

            try {
                return new TLSConfig.Builder().clientCert(clientCert, clientKey, clientKeyPass).trust(caCert)
                        .trustAll(insecure != null ? insecure.booleanValue() : false).build();
            } catch (ConfigValidationException e) {
                validationErrors.add(null, e);
            }

            validationErrors.throwExceptionForPresentErrors();
        } else if (clusterConfig != null) {
            // 3rd case: Both. We have to merge the stuff

            Map<String, Object> config = clusterConfig.getTlsConfig().toMap();
            Map<String, Object> clientAuthConfig = clusterConfig.getTlsConfig().getClientCertAuthConfig() != null
                    ? clusterConfig.getTlsConfig().getClientCertAuthConfig().toMap()
                    : new LinkedHashMap<>();

            if (clientCert != null) {
                clientAuthConfig.put("certificate", "${file:" + clientCert.getAbsolutePath() + "}");
            }

            if (clientKey != null) {
                clientAuthConfig.put("private_key", "${file:" + clientKey.getAbsolutePath() + "}");
            }

            if (clientKeyPass != null) {
                clientAuthConfig.put("private_key_password", clientKeyPass);
            }

            if (caCert != null) {
                config.put("trusted_cas", "${file:" + caCert.getAbsolutePath() + "}");
            }

            if (insecure != null) {
                config.put("trust_all", insecure);
            }

            if (ciphers != null) {
                config.put("enabled_ciphers", ciphers);
            }

            config.put("client_auth", clientAuthConfig);

            try {
                return TLSConfig.parse(config);
            } catch (ConfigValidationException e) {
                validationErrors.add(null, e);
            }

            validationErrors.throwExceptionForPresentErrors();

        }

        return null;
    }

    private String getHumanReadableErrorMessage(FailedConnectionException e) {
        if (e.getCause() instanceof SSLHandshakeException) {
            if (e.getMessage().contains("unable to find valid certification path to requested target")) {
                return "Could not validate server certificate using current CA settings. Please verify that you are using the correct CA certificates. You can specify custom CA certificates using the --ca-cert option.";
            }
        } else if (e.getCause() instanceof SSLException) {
            if (e.getCause().getCause() instanceof SocketException) {
                return "Connection failed: " + e.getCause().getCause().getMessage();
            }
        }

        return e.getMessage();
    }

    private String getCertificateInfo(Collection<? extends Certificate> certificateChain) {
        StringBuilder result = new StringBuilder();

        for (Certificate certificate : certificateChain) {
            if (result.length() > 0) {
                result.append(" / ");
            }

            if (certificate instanceof X509Certificate) {
                X509Certificate x509c = (X509Certificate) certificate;
                result.append(x509c.getSubjectDN());
            } else if (certificate != null) {
                result.append(certificate.getClass().getName());
            } else {
                result.append("null");
            }
        }

        return result.toString();
    }
    
    protected String getHost() {
        return this.host;
    }
}
