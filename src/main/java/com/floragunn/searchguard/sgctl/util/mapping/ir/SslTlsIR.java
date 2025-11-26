package com.floragunn.searchguard.sgctl.util.mapping.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SslTlsIR {

    public Tls transport;
    public Tls http;


    Map<String, List<String>> profileAllowedIPs = new HashMap<>(); // transport only: List of IP addresses (value) to allow for this profile (key)
    Map<String, List<String>> profileDeniedIPs = new HashMap<>(); // transport only: List of IP addresses (value) to deny for this profile (key)

    public SslTlsIR() {
        transport = new Tls();
        http = new Tls();
    }

    public void handleOptions(String optionName, Object optionValue) {
        boolean error = false;

        if (IntermediateRepresentation.assertType(optionValue, List.class)) {
            List<?> value = (List<?>) optionValue;

            if (value.isEmpty()) {
                return;
            }

            // Regex for profileAllowedIPs/profileDeniedIPs with optionName profiles.$PROFILE.xpack.security.filter. allow or deny
            Pattern profileIPsPattern = Pattern.compile("^profiles\\.([^.]+)\\.xpack\\.security\\.filter\\.(allow|deny)$");
            Matcher mProfileIPs = profileIPsPattern.matcher(optionName);

            if (!(value.get(0) instanceof String)) {
                error = true;
            } else if(mProfileIPs.find()) {
                String profile = mProfileIPs.group(1);
                String action  = mProfileIPs.group(2);

                if ("allow".equals(action)) {
                    profileAllowedIPs
                        .computeIfAbsent(profile, k -> new ArrayList<>())
                        .addAll((List<String>) value);

                } else if ("deny".equals(action)) {
                    profileDeniedIPs
                        .computeIfAbsent(profile, k -> new ArrayList<>())
                        .addAll((List<String>) value);
                }
            } else {
                switch (optionName) {
                    default:
                        error = true;
                }
            }
        }

        if (error) {
            System.out.println("Invalid option of type " + optionValue.getClass() + ": " + optionName + " = " + optionValue);
        }
    }
}
