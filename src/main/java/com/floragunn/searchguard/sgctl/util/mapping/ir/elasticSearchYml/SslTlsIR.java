/*
 * Copyright 2025-2026 floragunn GmbH
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


package com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml;

import com.floragunn.searchguard.sgctl.util.mapping.MigrationReport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intermediate representation for TLS settings and transport profile IP filters.
 */
public class SslTlsIR {

    private static final String FILE_NAME = "elasticsearch.yml";
    private static final Pattern PROFILE_IPS_PATTERN = Pattern.compile("^profiles\\.([^.]+)\\.xpack\\.security\\.filter\\.(allow|deny)$");

    private final MigrationReport report = MigrationReport.shared;

    private final Tls transport;
    private final Tls http;

    // transport only: List of IP addresses (value) to allow/deny for this profile (key)
    private final Map<String, List<String>> profileAllowedIPs = new HashMap<>();
    private final Map<String, List<String>> profileDeniedIPs = new HashMap<>();
    private final Map<String, List<String>> profileAllowedIPsView = Collections.unmodifiableMap(profileAllowedIPs);
    private final Map<String, List<String>> profileDeniedIPsView = Collections.unmodifiableMap(profileDeniedIPs);

    /** @return TLS settings for the transport layer. */
    public Tls getTransport() { return transport; }
    /** @return TLS settings for the HTTP layer. */
    public Tls getHttp() { return http; }
    /** @return per-profile allow lists for transport IP filtering. */
    public Map<String, List<String>> getProfileAllowedIPs() { return profileAllowedIPsView; }
    /** @return per-profile deny lists for transport IP filtering. */
    public Map<String, List<String>> getProfileDeniedIPs() { return profileDeniedIPsView; }

    public SslTlsIR() {
        transport = new Tls();
        http = new Tls();
    }

    public void handleOptions(String optionName, Object optionValue, String keyPrefix, File configFile) {
        if (!(optionValue instanceof List<?> rawList)) {
            report.addInvalidType(FILE_NAME, keyPrefix + optionName, List.class, optionValue);
            return;
        }

        if (rawList.isEmpty()) {
            return;
        }

        var profileMatch = PROFILE_IPS_PATTERN.matcher(optionName);
        var values = asStringList(rawList, keyPrefix + optionName);
        if (values == null) {
            return;
        }

        if (profileMatch.matches()) {
            String profile = profileMatch.group(1);
            String action  = profileMatch.group(2);

            Map<String, List<String>> target = "allow".equals(action) ? profileAllowedIPs : profileDeniedIPs;
            target.merge(profile, freezeList(values), (oldList, newList) -> {
                if (oldList.isEmpty()) {
                    return newList;
                }
                ArrayList<String> merged = new ArrayList<>(oldList);
                merged.addAll(newList);
                return Collections.unmodifiableList(merged);
            });
            return;
        }

        report.addUnknownKey(FILE_NAME, keyPrefix + optionName, configFile.getPath());
    }

    private List<String> asStringList(List<?> rawList, String path) {
        var result = new ArrayList<String>(rawList.size());
        for (Object element : rawList) {
            if (!(element instanceof String s)) {
                report.addInvalidType(FILE_NAME, path, String.class, element);
                return null;
            }
            result.add(s);
        }
        return result;
    }

    private List<String> freezeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }
}
