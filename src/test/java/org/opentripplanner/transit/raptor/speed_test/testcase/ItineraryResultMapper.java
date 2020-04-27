package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.transit.raptor.speed_test.api.model.Itinerary;
import org.opentripplanner.transit.raptor.speed_test.api.model.Leg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opentripplanner.transit.raptor.speed_test.transit.SpeedTestItinerary.legsAsCompactString;

/**
 * Map an Itinerary to a result instance. We do this to normalize the Itinerary
 * for the purpose of testing, and serialization of the results.
 * <p/>
 * This way we do not need to change the Itinerary class to fit our needs and
 * we avoid the 'feature envy' anti pattern.
 */
class ItineraryResultMapper {
    private static final Map<String, String> AGENCY_NAMES_SHORT = new HashMap<>();

    static {
        AGENCY_NAMES_SHORT.put("Flytoget", "Fly");
        AGENCY_NAMES_SHORT.put("Hedmark Trafikk FKF", "Hed");
        AGENCY_NAMES_SHORT.put("Kringom", "Kring");
        AGENCY_NAMES_SHORT.put("Indre Namdal Trafikk A/S", "INam");
        AGENCY_NAMES_SHORT.put("Møre og Romsdal fylkeskommune", "M&R");
        AGENCY_NAMES_SHORT.put("Nettbuss Travel AS", "Nett");
        AGENCY_NAMES_SHORT.put("Nord-Trøndelag fylkeskommune", "NTrø");
        AGENCY_NAMES_SHORT.put("Norgesbuss Ekspress AS", "Norge");
        AGENCY_NAMES_SHORT.put("NOR-WAY Bussekspress", "NOR-WAY");
        AGENCY_NAMES_SHORT.put("Nordland fylkeskommune", "Nord");
        AGENCY_NAMES_SHORT.put("Opplandstrafikk", "Opp");
        AGENCY_NAMES_SHORT.put("Snelandia", "Sne");
        AGENCY_NAMES_SHORT.put("Troms fylkestrafikk", "Troms");
        AGENCY_NAMES_SHORT.put("Vestfold Kollektivtrafikk as", "Vest");
        AGENCY_NAMES_SHORT.put("Østfold fylkeskommune", "Øst");
    }

    static Collection<Result> map(final String testCaseId, Collection<Itinerary> itineraries) {
        return itineraries.stream().map(it -> map(testCaseId, it)).collect(Collectors.toList());
    }

    private static Result map(String testCaseId, Itinerary itinerary) {
        Result result = new Result(
                testCaseId,
                itinerary.transfers,
                itinerary.duration,
                (int)itinerary.weight,
                itinerary.walkDistance.intValue(),
                itinerary.startTimeAsStr(),
                itinerary.endTimeAsStr(),
                legsAsCompactString(itinerary)
        );

        for (Leg it : itinerary.legs) {
            if (it.isTransitLeg()) {
                result.agencies.add(AGENCY_NAMES_SHORT.getOrDefault(it.agencyName, it.agencyName));
                result.modes.add(it.mode);
                result.routes.add(it.routeShortName);
            }
        }
        return result;
    }
}
