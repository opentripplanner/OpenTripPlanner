package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.transit.raptor.speed_test.model.Itinerary;
import org.opentripplanner.transit.raptor.speed_test.model.Leg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
        AGENCY_NAMES_SHORT.put("Agder flyekspress", "AgderFly");
        AGENCY_NAMES_SHORT.put("Agder Kollektivtrafikk as", "Agder");
        AGENCY_NAMES_SHORT.put("AtB", "AtB");
        AGENCY_NAMES_SHORT.put("Avinor", "Avinor");
        AGENCY_NAMES_SHORT.put("Farte", "Farte");
        AGENCY_NAMES_SHORT.put("FlixBus", "FlexBus");
        AGENCY_NAMES_SHORT.put("Flybussen Norgesbuss", "Flybussen");
        AGENCY_NAMES_SHORT.put("Flytoget", "FLY");
        AGENCY_NAMES_SHORT.put("Flåmsbana", "FLÅ");
        AGENCY_NAMES_SHORT.put("Hedmark Trafikk", "HED");
        AGENCY_NAMES_SHORT.put("Møre og Romsdal fylkeskommune", "M&R");
        AGENCY_NAMES_SHORT.put("NOR-WAY Bussekspress", "N-W");
        AGENCY_NAMES_SHORT.put("Ruter", "RUT");
        AGENCY_NAMES_SHORT.put("SJ AB", "SJ");
        AGENCY_NAMES_SHORT.put("Skyss", "SKY");
        AGENCY_NAMES_SHORT.put("Snelandia", "Snelandia");
        AGENCY_NAMES_SHORT.put("Troms fylkestrafikk", "Troms");
        AGENCY_NAMES_SHORT.put("Unibuss Ekspress", "Unibuss");
        AGENCY_NAMES_SHORT.put("Vestfold Kollektivtrafikk", "VF");
        AGENCY_NAMES_SHORT.put("Vy", "Vy");
        AGENCY_NAMES_SHORT.put("Vy express", "VyEx");
        AGENCY_NAMES_SHORT.put("N/A", "DummyEUR");

        // Old agencies (2019)
        AGENCY_NAMES_SHORT.put("Hedmark Trafikk FKF", "HED");
        AGENCY_NAMES_SHORT.put("Nord-Trøndelag fylkeskommune", "NTrø");
        AGENCY_NAMES_SHORT.put("Nordland fylkeskommune", "Nordld");
        AGENCY_NAMES_SHORT.put("Norgesbuss Ekspress AS", "NorBuss");
        AGENCY_NAMES_SHORT.put("Opplandstrafikk", "OPP");
        AGENCY_NAMES_SHORT.put("Vestfold Kollektivtrafikk as", "VF");
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
                itinerary.startTime,
                itinerary.endTime,
                itinerary.details()
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
