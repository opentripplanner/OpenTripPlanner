package org.opentripplanner.profile;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import java.util.Collections;
import java.util.List;

/**
 * Transforms transit stop names into a somewhat more normalized form, so string
 * distance calculations will be more meaningful.
 */
public class StopNameNormalizer {

    static final String[][] STREET_TYPES = {
        {"DR", "DRIVE"},
        {"RD", "ROAD"},
        {"ST", "STREET", "S"},
        {"AV", "AVE", "AVENUE"},
        {"BV", "BLVD", "BOULEVARD"},
        {"CL", "CI", "CIR", "CIRCLE"},
        {"CT", "COURT"},
        {"WY", "WAY"},
        {"TE", "TERRACE"},
        {"PL", "PLACE"},
        {"LN", "LA", "LANE"},
        {"PK", "PI", "PIKE"},
        {"PW", "PKWY", "PARKWAY"},
        {"RN", "RUN"},
        {"HW", "HWY", "HIGHWAY"}
    };

    static final String[][] QUADRANTS = {
        {"NW", "NORTHWEST"},
        {"NE", "NORTHEAST"},
        {"SW", "SOUTHWEST"},
        {"SE", "SOUTHEAST"},
        {"N", "NORTH"},
        {"S", "SOUTH"},
        {"E", "EAST"},
        {"W", "WEST"}
    };

    static final String[][] QUALIFIERS = {
        {"NB", "N/B", "NORTHBOUND"},
        {"SB", "S/B", "SOUTHBOUND"},
        {"EB", "E/B", "EASTBOUND"},
        {"WB", "W/B", "WESTBOUND"},
        {"NS", "N/S", "NEARSIDE"},
        {"FS", "F/S", "FARSIDE"},
        {"OPP", "OPPOSITE"}
    };

    public static String normalize (String name) {
        // Separate the two halves of an intersection. "AT" sometimes appears too.
        String[] parts = name.toUpperCase().split("[&@]", 2);
        List<String> normalizedParts = Lists.newArrayList();
        for (String part : parts) {
            String quadrant = null;
            String streetType = null;
            // We want to keep slashes since they appear within some abbreviations
            String[] words = part.split("[ ,.]");
            // Remove junk whitespace
            for (int i = 0; i < words.length; i++) {
                words[i] = words[i].trim();
            }

            // 1. Strip out quadrant
            QD: for (int i = 0; i < words.length; i++) {
                String word = words[i];
                for (String[] q : QUADRANTS) {
                    for (String qn : q) {
                        if (word.equals(qn)) {
                            quadrant = q[0];
                            words[i] = null;
                            break QD;
                        }
                    }
                }
            }

            // 2. Strip out road type
            ST: for (int i = 0; i < words.length; i++) {
                String word = words[i];
                for (String[] st : STREET_TYPES) {
                    for (String stn : st) {
                        if (stn.equals(word)) { // word may be null
                            streetType = st[0];
                            words[i] = null;
                            break ST;
                        }
                    }
                }
            }

            // 3. Remove all qualifiers
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                for (String[] qs : QUALIFIERS) {
                    for (String qn : qs) {
                        if (qn.equals(word)) { // word may be null
                            words[i] = null;
                            // do not break, more than one qualifier may be present
                        }
                    }
                }
            }

            // 4. Remove all "BAY N"
            for (int i = 0; i < words.length-1; i++) {
                String word = words[i];
                // TODO improve the below -- it doesn't catch lettered bays
                // Integer number = Ints.tryParse(words[i + 1]);
                if ("BAY".equals(word)) {
                    words[i] = null;
                    words[i+1] = null;
                }
            }

            // N. Replace ordinal abbreviations?
            // if length >= 3, begins with digit, ends with "1ST" "2ND" "3RD" "dTH" replace last 2 chars with ordinal symbol TH

            // 5. Place elements in a predictable order
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word != null) {
                    sb.append(word);
                    sb.append(' ');
                }
            }
            if (streetType != null) {
                sb.append(streetType);
                sb.append(' ');
            }
            if (quadrant != null) sb.append(quadrant);
            normalizedParts.add(sb.toString().trim());
        }
        // Make sure the two streets of an intersection always appear in the same order
        Collections.sort(normalizedParts); // overkill for a swap operation
        String result = Joiner.on(" & ").join(normalizedParts);
        return result;
    }

}
