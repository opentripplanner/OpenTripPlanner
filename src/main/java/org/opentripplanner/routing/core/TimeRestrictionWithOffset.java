package org.opentripplanner.routing.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class TimeRestrictionWithOffset {
    private final TimeRestriction timeRestriction;
    private final long offsetInSecondsFromStartOfSearch;
}
