package org.opentripplanner.transit.speed_test.model.testcase;

import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.model.plan.UnknownTransitPathLeg;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * Map an Itinerary to a result instance. We do this to normalize the Itinerary for the purpose of
 * testing, and serialization of the results.
 * <p/>
 * This way we do not need to change the Itinerary class to fit our needs and we avoid the 'feature
 * envy' anti pattern.
 */
class ItineraryResultMapper {

  private static final Map<String, String> AGENCY_NAMES_SHORT = new HashMap<>();

  private final String testCaseId;

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
    AGENCY_NAMES_SHORT.put("Østfold fylkeskommune", "ØstFyl");
    AGENCY_NAMES_SHORT.put("Østfold kollektivtrafikk", "ØstKol");
  }

  private ItineraryResultMapper(String testCaseId) {
    this.testCaseId = testCaseId;
  }

  public static String details(Itinerary itin) {
    PathStringBuilder buf = new PathStringBuilder(Integer::toString);

    for (Leg leg : itin.legs()) {
      var fromStop = leg.getFrom().stop;
      if (fromStop != null) {
        buf.stop(formatStop(fromStop));
      }

      if (leg.isWalkingLeg()) {
        buf.walk((int) leg.getDuration().toSeconds());
      } else if (
        leg instanceof StreetLeg streetLeg && streetLeg.getFrom().vehicleRentalPlace != null
      ) {
        var name = streetLeg.getFrom().vehicleRentalPlace.getName().toString();
        buf.pickupRental(name, (int) leg.getDuration().toSeconds());
      } else if (leg instanceof TransitLeg transitLeg) {
        buf.transit(
          transitLeg.getMode().name() + " " + leg.getRoute().getShortName(),
          leg.getStartTime().get(ChronoField.SECOND_OF_DAY),
          leg.getEndTime().get(ChronoField.SECOND_OF_DAY)
        );
      } else if (leg instanceof UnknownTransitPathLeg d) {
        buf.text(d.description());
      }
    }
    return buf.toString();
  }

  static Collection<Result> map(
    final String testCaseId,
    Collection<org.opentripplanner.model.plan.Itinerary> itineraries
  ) {
    var mapper = new ItineraryResultMapper(testCaseId);
    return itineraries.stream().map(mapper::map).collect(Collectors.toList());
  }

  private static String formatStop(StopLocation s) {
    return s.getName() + "(" + s.getId().getId() + ")";
  }

  private static String agencyShortName(Agency agency) {
    return AGENCY_NAMES_SHORT.getOrDefault(agency.getName(), agency.getName());
  }

  private Result map(Itinerary itinerary) {
    List<String> agencies = new ArrayList<>();
    List<String> routes = new ArrayList<>();
    Set<TransitMode> modes = EnumSet.noneOf(TransitMode.class);
    List<String> stops = new ArrayList<>();

    for (Leg it : itinerary.legs()) {
      if (it instanceof TransitLeg trLeg) {
        agencies.add(agencyShortName(it.getAgency()));
        routes.add(it.getRoute().getName());
        modes.add(trLeg.getMode());
      }
      if (it.getTo().stop != null) {
        stops.add(it.getTo().stop.getId().toString());
      }
    }

    return new Result(
      testCaseId,
      itinerary.numberOfTransfers(),
      itinerary.totalDuration(),
      itinerary.generalizedCost(),
      itinerary
        .legs()
        .stream()
        .filter(Leg::isWalkingLeg)
        .mapToInt(l -> IntUtils.round(l.getDistanceMeters()))
        .sum(),
      itinerary.startTime().get(ChronoField.SECOND_OF_DAY),
      itinerary.endTime().get(ChronoField.SECOND_OF_DAY),
      agencies,
      List.copyOf(modes),
      routes,
      stops,
      details(itinerary)
    );
  }
}
