package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

/**
 * Specifies a class of OSM tagged entities (e.g. ways) by a list of tags and their values (which may be wildcards).
 * The OSMSpecifier which matches the most tags on an OSM entity will win. In the event that several OSMSpecifiers
 * match the same number of tags, the one that does so using less wildcards will win. For example, if one OSMSpecifier
 * has the tags (highway=residential, cycleway=*) and another has (highway=residential, surface=paved) and a way has the
 * tags (highway=residential, cycleway=lane, surface=paved) the second OSMSpecifier will be applied to that way
 * (2 exact matches beats 1 exact match and a wildcard match).
 *
 * You can also use a logical OR condition to specify a match. This indented to be used with a safety mixin.
 *
 * For example if you specify "lcn=yes|rnc=yes|ncn=yes" then this will match if one of these tags matches.
 *
 * If you would add 3 separate matches that would mean that a way that is tagged with all of them would receive
 * too high a safety value leading to undesired detours.
 *
 * Logical ORs are only implemented for mixins without wildcards.
 */
public class OSMSpecifier {

    private List<P2<String>> logicalANDPairs = new ArrayList<>(3);
    private List<P2<String>> logicalORPairs = new ArrayList<>(3);

    public OSMSpecifier(String spec) {
        if(spec.contains("|") && spec.contains(";")) {
            throw new RuntimeException(String.format("You cannot mix logical AND (';') and logical OR ('|') in same OSM spec: '%s'", spec));
        }
        else if(spec.contains("|") && spec.contains("*")) {
            throw new RuntimeException(String.format("You cannot mix logical OR ('|') and wildcards ('*') in the same OSM spec: '%s'", spec));
        }
        else if(spec.contains("|")){
            logicalORPairs = getPairsFromString(spec, "\\|");
        } else {
            logicalANDPairs = getPairsFromString(spec, ";");
        }
    }

    private List<P2<String>> getPairsFromString(String spec, String separator) {
        return Arrays.stream(spec.split(separator))
                .filter(p -> !p.isEmpty())
                .map(pair -> {
                    var kv = pair.split("=");
                    return new P2<>(kv[0], kv[1]);
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculates a pair of scores expressing how well an OSM entity's tags match this specifier.
     *
     * Tags in this specifier are matched against those for the left and right side of the OSM way separately. See:
     * http://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right
     * TODO: we should probably support forward/backward as well.
     * TODO: simply count the number of full, partial, and wildcard matches instead of using a scoring system.
     *
     * @param match an OSM tagged object to compare to this specifier
     */
    public P2<Integer> matchScores(OSMWithTags match) {
        if(!logicalANDPairs.isEmpty()) {
            return computeANDScore(match);
        } else {
            return computeORScore(match);
        }
    }

    private P2<Integer> computeORScore(OSMWithTags match) {
        // not sure if we should calculate a proper score as it doesn't make a huge amount of sense to do it for
        // logical OR conditions
        var oneOfORPairMatches = logicalORPairs.stream().anyMatch(pair -> match.isTag(pair.first, pair.second));
        if (oneOfORPairMatches) {
            return new P2<>(1,1);
        } else return new P2<>(0,0);
    }

    private P2<Integer> computeANDScore(OSMWithTags match) {
        int leftScore = 0, rightScore = 0;
        int leftMatches = 0, rightMatches = 0;

        for (P2<String> pair : logicalANDPairs) {
            // TODO why are we repeatedly converting these to lower case every time they are used?
            // Probably because it used to be possible to set them from Spring XML.
            String tag = pair.first.toLowerCase();
            String value = pair.second.toLowerCase();
            String leftMatchValue = match.getTag(tag + ":left");
            String rightMatchValue = match.getTag(tag + ":right");
            String matchValue = match.getTag(tag);
            if (leftMatchValue == null) {
                leftMatchValue = matchValue;
            }
            if (rightMatchValue == null) {
                rightMatchValue = matchValue;
            }
            int leftTagScore = getTagScore(value, leftMatchValue);
            leftScore += leftTagScore;
            if (leftTagScore > 0) {
                leftMatches ++;
            }
            int rightTagScore = getTagScore(value, rightMatchValue);
            rightScore += rightTagScore;
            if (rightTagScore > 0) {
                rightMatches ++;
            }
        }


        int allMatchLeftBonus = (leftMatches == logicalANDPairs.size()) ? 10 : 0;
        leftScore += allMatchLeftBonus;
        int allMatchRightBonus = (rightMatches == logicalANDPairs.size()) ? 10 : 0;
        rightScore += allMatchRightBonus;
        return new P2<>(leftScore, rightScore);
    }

    /**
     * Calculates a score expressing how well an OSM entity's tags match this specifier.
     * This does exactly the same thing as matchScores but without regard for :left and :right.
     */
    public int matchScore(OSMWithTags match) {
        int score = 0;
        int matches = 0;
        for (P2<String> pair : logicalANDPairs) {
            String tag = pair.first.toLowerCase();
            String value = pair.second.toLowerCase();
            String matchValue = match.getTag(tag);
            int tagScore = getTagScore(value, matchValue);
            score += tagScore;
            if (tagScore > 0) {
                matches += 1;
            }
        }
        score += matches == logicalANDPairs.size() ? 10 : 0;
        return score;
    }

    /**
     * Calculates a score indicating how well an OSM tag value matches the given matchValue.
     * An exact match is worth 100 points, a partial match on the part of the value before a colon is worth 75 points,
     * and a wildcard match is worth only one point, to serve as a tiebreaker. A score of 0 means they do not match.
     */
    private int getTagScore(String value, String matchValue) {
        // either this matches on a wildcard, or it matches exactly
        if (value.equals("*") && matchValue != null) {
            return 1; // wildcard matches are basically tiebreakers
        } else if (value.equals(matchValue)) {
            return 100;
        } else {
            if (value.contains(":")) {
                // treat cases like cobblestone:flattened as cobblestone if a more-specific match
                // does not apply
                value = value.split(":", 2)[0];
                if (value.equals(matchValue)) {
                    return 75;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (P2<String> pair : logicalANDPairs) {
            builder.append(pair.first);
            builder.append("=");
            builder.append(pair.second);
            builder.append(";");
        }
        for (P2<String> pair : logicalORPairs) {
            builder.append(pair.first);
            builder.append("=");
            builder.append(pair.second);
            builder.append("|");
        }
        return builder.toString();
    }

    public boolean containsLogicalOr() {
        return !logicalORPairs.isEmpty();
    }
}
