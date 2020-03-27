package org.opentripplanner.transit.raptor.speed_test;

import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public enum SpeedTestProfile {
    std_range_raptor(
            "rr",
            "Standard Range Raptor, super fast [ transfers, arrival time, travel time ].",
            RaptorProfile.STANDARD,
            SearchDirection.FORWARD
    ),
    std_range_raptor_reverse(
            "rrr",
            "Reverse Standard Range Raptor",
            RaptorProfile.STANDARD,
            SearchDirection.REVERSE
    ),
    std_best_time(
            "bt",
            "Best Time Range Raptor, super fast. Arrival time only, no path.",
            RaptorProfile.BEST_TIME,
            SearchDirection.FORWARD
    ),
    std_best_time_reverse(
            "btr",
            "Reverse Best Time Range Raptor",
            RaptorProfile.BEST_TIME,
            SearchDirection.REVERSE
    ),
    no_wait_std(
            "ws",
            "Standard Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_STD,
            SearchDirection.FORWARD
    ),
    no_wait_std_reverse(
            "wsr",
            "Reverse Standard Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_STD,
            SearchDirection.REVERSE
    ),
    no_wait_best_time(
            "wt",
            "Best Time Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_BEST_TIME,
            SearchDirection.FORWARD
    ),
    no_wait_best_time_reverse(
            "wtr",
            "Reverse Best Time Range Raptor without waiting time.",
            RaptorProfile.NO_WAIT_BEST_TIME,
            SearchDirection.REVERSE
    ),
    mc_range_raptor(
            "mc",
            "Multi-Criteria Range Raptor [ transfers, arrival time, travel time, cost ].",
            RaptorProfile.MULTI_CRITERIA,
            SearchDirection.FORWARD
    ),
    mc_destination(
            "md",
            "Multi-Criteria Range Raptor with check on destination arrival.",
            RaptorProfile.MULTI_CRITERIA,
            SearchDirection.FORWARD,
            Optimization.PARETO_CHECK_AGAINST_DESTINATION
    );

    final String shortName;
    final String description;
    final RaptorProfile raptorProfile;
    final SearchDirection direction;
    final List<Optimization> optimizations;

    SpeedTestProfile(String shortName, String description, RaptorProfile profile, SearchDirection direction, Optimization... optimizations) {
        this.shortName = shortName;
        this.description = description;
        this.raptorProfile = profile;
        this.direction = direction;
        this.optimizations = Arrays.asList(optimizations);
    }

    public static SpeedTestProfile[] parse(String profiles) {
        return Arrays.stream(profiles.split(",")).map(SpeedTestProfile::parseOne).toArray(SpeedTestProfile[]::new);
    }

    public static List<String> options() {
        return Arrays.stream(values()).map(SpeedTestProfile::description).collect(Collectors.toList());
    }


    /* private methods */

    private static SpeedTestProfile parseOne(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignore) {
            for (SpeedTestProfile it : values()) {
                if (it.shortName.equalsIgnoreCase(value)) {
                    return it;
                }
            }
            throw new IllegalArgumentException(
                    "Profile is not valid: '" + value + "'\nProfiles:\n\t" +
                            Arrays.stream(values())
                                    .map(SpeedTestProfile::description)
                                    .collect(Collectors.joining("\n\t"))
                                    .replace('.', ' ')
            );
        }
    }

    private String description() {
        StringBuilder text;

        if (name().equals(shortName)) {
            text = new StringBuilder(String.format("%s : %s", name(), description));
        } else {
            text = new StringBuilder(String.format("%s, %s : %s", shortName, name(), description));
        }

        if (raptorProfile != null) {
            text.append(String.format("\n·%22s%s", "", raptorProfile));
            text.append(", ").append(direction.name());
            for (Optimization it : optimizations) {
                text.append(", ").append(it.name());
            }
        }
        return text.toString();
    }
}