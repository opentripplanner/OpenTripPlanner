package org.opentripplanner.ext.fares.impl;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum NycFareState {
  INIT,
  SUBWAY_PRE_TRANSFER,
  SUBWAY_PRE_TRANSFER_WALKED,
  SUBWAY_POST_TRANSFER,
  SIR_PRE_TRANSFER,
  SIR_POST_TRANSFER_FROM_SUBWAY,
  SIR_POST_TRANSFER_FROM_BUS,
  EXPENSIVE_EXPRESS_BUS,
  BUS_PRE_TRANSFER,
  CANARSIE,
}

enum NycRideClassifier {
  SUBWAY,
  SIR,
  LOCAL_BUS,
  EXPRESS_BUS,
  EXPENSIVE_EXPRESS_BUS,
  WALK,
}

/**
 * This handles the New York City MTA's baroque fare rules for subways and buses with the following
 * limitations: (1) the two hour limit on transfers is not enforced (2) the b61/b62 special case is
 * not handled (3) MNR, LIRR, and LI Bus are not supported -- only subways and buses
 * <p>
 * I have not yet tested this on NY data since we switched to OTP2 (Raptor). It may need to be
 * fixed. The only thing I've changed is how we produce rides from PathLegs instead of AStar states.
 * The actual fare calculation logic remains exactly the same except for one thing: thanks to
 * switching to typesafe enums, I fixed one bug where we were adding the enum value instead of the
 * fare to the total cost.
 */
public class NycFareServiceImpl implements FareService {

  private static final Logger LOG = LoggerFactory.getLogger(NycFareServiceImpl.class);

  private static final float ORDINARY_FARE = 2.75f;

  private static final float EXPRESS_FARE = 6.50f;

  private static final float EXPENSIVE_EXPRESS_FARE = 7.50f; // BxM4C only

  private static final List<FeedScopedId> SIR_PAID_STOPS = makeMtaStopList("S31", "S30");

  private static final List<FeedScopedId> SUBWAY_FREE_TRANSFER_STOPS = makeMtaStopList(
    "R11",
    "B08",
    "629"
  );

  private static final List<FeedScopedId> SIR_BONUS_STOPS = makeMtaStopList(
    "140",
    "420",
    "419",
    "418",
    "M22",
    "M23",
    "R27",
    "R26"
  );

  private static final List<FeedScopedId> SIR_BONUS_ROUTES = makeMtaStopList(
    "M5",
    "M20",
    "M15-SBS"
  );

  private static final List<FeedScopedId> CANARSIE = makeMtaStopList("L29", "303345");

  // List of NYC agencies to set fares for
  private static final List<String> AGENCIES = Arrays.asList("MTABC", "MTA NYCT");

  public NycFareServiceImpl() {}

  @Override
  public ItineraryFares getCost(Itinerary itinerary) {
    // Use custom ride-categorizing method instead of the usual mapper from default fare service.
    List<Ride> rides = createRides(itinerary);

    // There are no rides, so there's no fare.
    if (rides.size() == 0) {
      return null;
    }

    NycFareState state = NycFareState.INIT;
    boolean lexFreeTransfer = false;
    boolean canarsieFreeTransfer = false;
    boolean siLocalBus = false;
    boolean sirBonusTransfer = false;
    float totalFare = 0;
    for (Ride ride : rides) {
      FeedScopedId firstStopId = null;
      FeedScopedId lastStopId = null;
      if (ride.firstStop != null) {
        firstStopId = ride.firstStop.getId();
        lastStopId = ride.lastStop.getId();
      }
      switch (state) {
        case INIT:
          lexFreeTransfer = siLocalBus = canarsieFreeTransfer = false;
          if (ride.classifier.equals(NycRideClassifier.WALK)) {
            // walking keeps you in init
          } else if (ride.classifier.equals(NycRideClassifier.SUBWAY)) {
            state = NycFareState.SUBWAY_PRE_TRANSFER;
            totalFare += ORDINARY_FARE;
            if (SUBWAY_FREE_TRANSFER_STOPS.contains(ride.lastStop.getId())) {
              lexFreeTransfer = true;
            }
            if (CANARSIE.contains(ride.lastStop.getId())) {
              canarsieFreeTransfer = true;
            }
          } else if (ride.classifier.equals(NycRideClassifier.SIR)) {
            state = NycFareState.SIR_PRE_TRANSFER;
            if (SIR_PAID_STOPS.contains(firstStopId) || SIR_PAID_STOPS.contains(lastStopId)) {
              totalFare += ORDINARY_FARE;
            }
          } else if (ride.classifier.equals(NycRideClassifier.LOCAL_BUS)) {
            state = NycFareState.BUS_PRE_TRANSFER;
            totalFare += ORDINARY_FARE;
            if (CANARSIE.contains(ride.lastStop.getId())) {
              canarsieFreeTransfer = true;
            }
            siLocalBus = ride.route.getId().startsWith("S");
          } else if (ride.classifier.equals(NycRideClassifier.EXPRESS_BUS)) {
            state = NycFareState.BUS_PRE_TRANSFER;
            totalFare += EXPRESS_FARE;
          } else if (ride.classifier.equals(NycRideClassifier.EXPENSIVE_EXPRESS_BUS)) {
            state = NycFareState.EXPENSIVE_EXPRESS_BUS;
            totalFare += EXPENSIVE_EXPRESS_FARE;
          }
          break;
        case SUBWAY_PRE_TRANSFER_WALKED:
          if (ride.classifier.equals(NycRideClassifier.SUBWAY)) {
            // subway-to-subway transfers are verbotten except at
            // lex and 59/63
            if (!(lexFreeTransfer && SUBWAY_FREE_TRANSFER_STOPS.contains(ride.firstStop.getId()))) {
              totalFare += ORDINARY_FARE;
            }

            lexFreeTransfer = canarsieFreeTransfer = false;
            if (SUBWAY_FREE_TRANSFER_STOPS.contains(ride.lastStop.getId())) {
              lexFreeTransfer = true;
            }
            if (CANARSIE.contains(ride.lastStop.getId())) {
              canarsieFreeTransfer = true;
            }
          }
        /* FALL THROUGH */
        case SUBWAY_PRE_TRANSFER:
          // it will always be possible to transfer from the first subway
          // trip to anywhere,
          // since no sequence of subway trips takes greater than two
          // hours (if only just)
          if (ride.classifier.equals(NycRideClassifier.WALK)) {
            state = NycFareState.SUBWAY_PRE_TRANSFER_WALKED;
          } else if (ride.classifier.equals(NycRideClassifier.SIR)) {
            state = NycFareState.SIR_POST_TRANSFER_FROM_SUBWAY;
          } else if (ride.classifier.equals(NycRideClassifier.LOCAL_BUS)) {
            if (CANARSIE.contains(ride.firstStop.getId()) && canarsieFreeTransfer) {
              state = NycFareState.BUS_PRE_TRANSFER;
            } else {
              state = NycFareState.INIT;
            }
          } else if (ride.classifier.equals(NycRideClassifier.EXPRESS_BUS)) {
            // need to pay the upgrade cost
            totalFare += EXPRESS_FARE - ORDINARY_FARE;
          } else if (ride.classifier.equals(NycRideClassifier.EXPENSIVE_EXPRESS_BUS)) {
            totalFare += EXPENSIVE_EXPRESS_FARE; // no transfers to the
            // BxMM4C
          }
          break;
        case BUS_PRE_TRANSFER:
          if (ride.classifier.equals(NycRideClassifier.SUBWAY)) {
            if (CANARSIE.contains(ride.firstStop.getId()) && canarsieFreeTransfer) {
              state = NycFareState.SUBWAY_PRE_TRANSFER;
            } else {
              state = NycFareState.INIT;
            }
          } else if (ride.classifier.equals(NycRideClassifier.SIR)) {
            if (siLocalBus) {
              // SI local bus to SIR, so it is as if we started on the
              // SIR (except that when we enter the bus or subway system we need to do
              // so at certain places)
              sirBonusTransfer = true;
              state = NycFareState.SIR_PRE_TRANSFER;
            } else {
              //transfers exhausted
              state = NycFareState.INIT;
            }
          } else if (ride.classifier.equals(NycRideClassifier.LOCAL_BUS)) {
            state = NycFareState.INIT;
          } else if (ride.classifier.equals(NycRideClassifier.EXPRESS_BUS)) {
            // need to pay the upgrade cost
            totalFare += EXPRESS_FARE - ORDINARY_FARE;
            state = NycFareState.INIT;
          } else if (ride.classifier.equals(NycRideClassifier.EXPENSIVE_EXPRESS_BUS)) {
            totalFare += EXPENSIVE_EXPRESS_FARE;
            // no transfers to the BxMM4C
          }

          break;
        case SIR_PRE_TRANSFER:
          if (ride.classifier.equals(NycRideClassifier.SUBWAY)) {
            if (sirBonusTransfer && !SIR_BONUS_STOPS.contains(ride.firstStop.getId())) {
              //we were relying on the bonus transfer to be in the "pre-transfer state",
              //but the bonus transfer does not apply here
              totalFare += ORDINARY_FARE;
            }
            if (CANARSIE.contains(ride.lastStop.getId())) {
              canarsieFreeTransfer = true;
            }
            state = NycFareState.SUBWAY_POST_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.SIR)) {
            /* should not happen, and unhandled */
            LOG.warn("Should not transfer from SIR to SIR");
          } else if (ride.classifier.equals(NycRideClassifier.LOCAL_BUS)) {
            if (!SIR_BONUS_ROUTES.contains(ride.route)) {
              totalFare += ORDINARY_FARE;
            }
            state = NycFareState.BUS_PRE_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.EXPRESS_BUS)) {
            totalFare += EXPRESS_FARE;
            state = NycFareState.BUS_PRE_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.EXPENSIVE_EXPRESS_BUS)) {
            totalFare += EXPENSIVE_EXPRESS_FARE;
            state = NycFareState.BUS_PRE_TRANSFER;
          }
          break;
        case SIR_POST_TRANSFER_FROM_SUBWAY:
          if (ride.classifier.equals(NycRideClassifier.SUBWAY)) {
            /* should not happen */
            totalFare += ORDINARY_FARE;
            state = NycFareState.SUBWAY_PRE_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.SIR)) {
            /* should not happen, and unhandled */
            LOG.warn("Should not transfer from SIR to SIR");
          } else if (ride.classifier.equals(NycRideClassifier.LOCAL_BUS)) {
            if (!ride.route.getId().startsWith("S")) {
              totalFare += ORDINARY_FARE;
              state = NycFareState.BUS_PRE_TRANSFER;
            } else {
              state = NycFareState.INIT;
            }
          } else if (ride.classifier.equals(NycRideClassifier.EXPRESS_BUS)) {
            // need to pay the full cost
            totalFare += EXPRESS_FARE;
            state = NycFareState.INIT;
          } else if (ride.classifier.equals(NycRideClassifier.EXPENSIVE_EXPRESS_BUS)) {
            /* should not happen */
            // no transfers to the BxMM4C
            totalFare += EXPENSIVE_EXPRESS_FARE;
            state = NycFareState.BUS_PRE_TRANSFER;
          }
          break;
        case SUBWAY_POST_TRANSFER:
          if (ride.classifier.equals(NycRideClassifier.WALK)) {
            if (!canarsieFreeTransfer) {
              /* note: if we end up walking to another subway after alighting
               * at Canarsie, we will mistakenly not be charged, but nobody
               * would ever do this */
              state = NycFareState.INIT;
            }
          } else if (ride.classifier.equals(NycRideClassifier.SIR)) {
            totalFare += ORDINARY_FARE;
            state = NycFareState.SIR_PRE_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.LOCAL_BUS)) {
            if (!(CANARSIE.contains(ride.firstStop.getId()) && canarsieFreeTransfer)) {
              totalFare += ORDINARY_FARE;
            }
            state = NycFareState.INIT;
          } else if (ride.classifier.equals(NycRideClassifier.SUBWAY)) {
            //walking transfer
            totalFare += ORDINARY_FARE;
            state = NycFareState.SUBWAY_PRE_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.EXPRESS_BUS)) {
            totalFare += EXPRESS_FARE;
            state = NycFareState.BUS_PRE_TRANSFER;
          } else if (ride.classifier.equals(NycRideClassifier.EXPENSIVE_EXPRESS_BUS)) {
            totalFare += EXPENSIVE_EXPRESS_FARE;
            state = NycFareState.BUS_PRE_TRANSFER;
          }
      }
    }

    Currency currency = Currency.getInstance("USD");
    ItineraryFares fare = ItineraryFares.empty();
    fare.addFare(
      FareType.regular,
      new Money(
        currency,
        (int) Math.round(totalFare * Math.pow(10, currency.getDefaultFractionDigits()))
      )
    );
    return fare;
  }

  private static List<Ride> createRides(Itinerary itinerary) {
    return itinerary
      .getLegs()
      .stream()
      .map(leg -> mapToRide(itinerary, leg))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private static Ride mapToRide(Itinerary itinerary, Leg leg) {
    // It seems like we should do something more sophisticated than just ignore
    // agency IDs we don't recognize.
    if (!AGENCIES.contains(leg.getAgency().getId().getFeedId())) {
      return null;
    } else if (isTransferLeg(leg, itinerary)) {
      Ride ride = new Ride();
      ride.classifier = NycRideClassifier.WALK;
      return ride;
    } else if (leg instanceof TransitLeg transitLeg) {
      Ride ride = rideForTransitPathLeg(transitLeg);
      Route route = leg.getRoute();
      int routeType = route.getGtfsType();

      // Note the old implementation directly used the ints as classifiers here.
      if (routeType == 1) {
        ride.classifier = NycRideClassifier.SUBWAY;
      } else if (routeType == 2) {
        // All rail is Staten Island Railway? This won't work for LIRR and MNRR.
        ride.classifier = NycRideClassifier.SIR;
      } else if (routeType == 3) {
        ride.classifier = NycRideClassifier.LOCAL_BUS;
      }
      String shortName = route.getShortName();
      if (shortName == null) {
        ride.classifier = NycRideClassifier.SUBWAY;
      } else if (shortName.equals("BxM4C")) {
        ride.classifier = NycRideClassifier.EXPENSIVE_EXPRESS_BUS;
      } else if (
        shortName.startsWith("X") ||
        shortName.startsWith("BxM") ||
        shortName.startsWith("QM") ||
        shortName.startsWith("BM")
      ) {
        ride.classifier = NycRideClassifier.EXPRESS_BUS;
      }
      return ride;
    }
    return null;
  }

  private static boolean isTransferLeg(Leg leg, Itinerary itinerary) {
    return (
      !itinerary.firstLeg().equals(leg) && !itinerary.lastLeg().equals(leg) && leg.isWalkingLeg()
    );
  }

  private static List<FeedScopedId> makeMtaStopList(String... stops) {
    ArrayList<FeedScopedId> out = new ArrayList<>();
    for (String stop : stops) {
      out.add(new FeedScopedId("MTA NYCT", stop));
      out.add(new FeedScopedId("MTA NYCT", stop + "N"));
      out.add(new FeedScopedId("MTA NYCT", stop + "S"));
    }
    return out;
  }

  private static Ride rideForTransitPathLeg(TransitLeg transitLeg) {
    Ride ride = new Ride();
    ride.firstStop = transitLeg.getFrom().stop;
    ride.lastStop = transitLeg.getTo().stop;

    ride.startZone = ride.firstStop.getFirstZoneAsString();
    ride.endZone = ride.lastStop.getFirstZoneAsString();

    var zones = transitLeg
      .getIntermediateStops()
      .stream()
      .map(stopArrival -> stopArrival.place.stop.getFirstZoneAsString())
      .collect(Collectors.toSet());

    zones.addAll(
      Stream.of(ride.startZone, ride.endZone).filter(Objects::nonNull).collect(Collectors.toSet())
    );

    ride.zones = zones;
    ride.agency = transitLeg.getRoute().getAgency().getId();
    ride.route = transitLeg.getRoute().getId();
    ride.trip = transitLeg.getTrip().getId();

    ride.startTime = transitLeg.getStartTime();
    ride.endTime = transitLeg.getEndTime();

    // In the default fare service, we classify rides by mode.
    ride.classifier = transitLeg.getMode();
    return ride;
  }

  /**
   * A set of edges on a single route, with associated information. Used only in calculating fares.
   */
  private static class Ride {

    FeedScopedId agency; // route agency

    FeedScopedId route;

    FeedScopedId trip;

    Set<String> zones;

    String startZone;

    String endZone;

    ZonedDateTime startTime;

    ZonedDateTime endTime;

    // in DefaultFareServiceImpl classifier is just the TraverseMode
    // it can be used differently in custom fare services
    public Object classifier;

    public StopLocation firstStop;

    public StopLocation lastStop;

    public Ride() {
      zones = new HashSet<>();
    }

    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Ride");
      if (startZone != null) {
        builder.append("(from zone ");
        builder.append(startZone);
      }
      if (endZone != null) {
        builder.append(" to zone ");
        builder.append(endZone);
      }
      builder.append(" on route ");
      builder.append(route);
      if (zones.size() > 0) {
        builder.append(" through zones ");
        boolean first = true;
        for (String zone : zones) {
          if (first) {
            first = false;
          } else {
            builder.append(",");
          }
          builder.append(zone);
        }
      }
      builder.append(" at ");
      builder.append(startTime);
      if (classifier != null) {
        builder.append(", classified by ");
        builder.append(classifier.toString());
      }
      builder.append(")");
      return builder.toString();
    }
  }
}
