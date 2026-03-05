package org.opentripplanner.ext.ojp.mapping;

import static org.opentripplanner.ext.ojp.mapping.JaxbElementMapper.jaxbElement;
import static org.opentripplanner.ext.ojp.mapping.TextMapper.internationalText;

import de.vdv.ojp20.ContinuousLegStructure;
import de.vdv.ojp20.ContinuousServiceStructure;
import de.vdv.ojp20.LegAlightStructure;
import de.vdv.ojp20.LegBoardStructure;
import de.vdv.ojp20.LegIntermediateStructure;
import de.vdv.ojp20.LegStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.OJPTripDeliveryStructure;
import de.vdv.ojp20.PersonalModesOfOperationEnumeration;
import de.vdv.ojp20.PlaceRefStructure;
import de.vdv.ojp20.ServiceArrivalStructure;
import de.vdv.ojp20.ServiceDepartureStructure;
import de.vdv.ojp20.TimedLegStructure;
import de.vdv.ojp20.TripResultStructure;
import de.vdv.ojp20.TripStructure;
import jakarta.xml.bind.JAXBElement;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.LegCallTime;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StopArrival;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.ojp.time.XmlDateTime;
import org.opentripplanner.routing.api.response.RoutingResponse;

public class TripResponseMapper {

  public static final String TRIP_RESPONSE_CONTEXT = "TripResponseContext";

  public enum OptionalFeature {
    INTERMEDIATE_STOPS,
  }

  private final StopRefMapper stopPointRefMapper;
  private final Set<OptionalFeature> optionalFeatures;
  private final TripResponseContextMapper contextMapper;
  private final DatedJourneyMapper journeyMapper;

  public TripResponseMapper(FeedScopedIdMapper idMapper, Set<OptionalFeature> optionalFeatures) {
    this.stopPointRefMapper = new StopRefMapper(idMapper);
    this.optionalFeatures = optionalFeatures;
    this.contextMapper = new TripResponseContextMapper(stopPointRefMapper);
    this.journeyMapper = new DatedJourneyMapper(idMapper);
  }

  public OJP mapTripPlan(RoutingResponse otpResponse, ZonedDateTime timestamp) {
    List<JAXBElement<?>> tripResults = otpResponse
      .getTripPlan()
      .itineraries.stream()
      .map(this::mapItinerary)
      .map(JaxbElementMapper::jaxbElement)
      .collect(Collectors.toList());

    var context = jaxbElement(contextMapper.map(otpResponse.getTripPlan()), TRIP_RESPONSE_CONTEXT);
    var tripDelivery = new OJPTripDeliveryStructure()
      .withResponseTimestamp(XmlDateTime.truncatedToMillis(timestamp))
      .withRest(context)
      .withRest(tripResults);

    var serviceDelivery = ServiceDeliveryMapper.serviceDelivery(
      timestamp
    ).withAbstractFunctionalServiceDelivery(jaxbElement(tripDelivery));

    return new OJP()
      .withVersion("2.0")
      .withOJPResponse(new OJPResponseStructure().withServiceDelivery(serviceDelivery));
  }

  private TripResultStructure mapItinerary(Itinerary itinerary) {
    return new TripResultStructure()
      .withId(tripId(itinerary))
      .withTrip(
        new TripStructure()
          .withId(tripId(itinerary))
          .withDuration(Duration.between(itinerary.startTime(), itinerary.endTime()))
          .withTransfers(itinerary.legs().size() - 1)
          .withStartTime(new XmlDateTime(itinerary.startTime()))
          .withEndTime(new XmlDateTime(itinerary.endTime()))
          .withLeg(mapLegs(itinerary))
      );
  }

  private List<LegStructure> mapLegs(Itinerary itinerary) {
    var legs = new ArrayList<LegStructure>();
    for (int i = 0; i < itinerary.legs().size(); i++) {
      var leg = itinerary.legs().get(i);
      legs.add(mapLeg(i, leg));
    }
    return legs;
  }

  private LegStructure mapLeg(int index, Leg leg) {
    return switch (leg) {
      case ScheduledTransitLeg tl -> mapTransitLeg(index, tl);
      case StreetLeg sl -> mapStreetLeg(index, sl);
      default -> throw new IllegalStateException(
        "Unexpected leg type : " + leg.getClass().getSimpleName()
      );
    };
  }

  private LegStructure mapStreetLeg(int index, StreetLeg sl) {
    return baseLeg(index, sl).withContinuousLeg(
      new ContinuousLegStructure()
        .withLegStart(placeRef(sl.from()))
        .withLegEnd(placeRef(sl.to()))
        .withTimeWindowStart(new XmlDateTime(sl.startTime()))
        .withTimeWindowEnd(new XmlDateTime(sl.endTime()))
        .withDuration(sl.duration())
        .withLength((int) sl.distanceMeters())
        .withService(mapContinuousService(sl))
    );
  }

  private static ContinuousServiceStructure mapContinuousService(StreetLeg sl) {
    return new ContinuousServiceStructure()
      .withPersonalModeOfOperation(PersonalModesOfOperationEnumeration.OWN)
      .withPersonalMode(PersonalModeMapper.mapToOjp(sl.getMode()));
  }

  private LegStructure mapTransitLeg(int index, ScheduledTransitLeg tl) {
    var scheduledDeparture = new XmlDateTime(tl.start().scheduledTime());
    var realtimeDeparture = estimatedTime(tl.start());
    var scheduledArrival = new XmlDateTime(tl.end().scheduledTime());
    var realtimeArrival = estimatedTime(tl.end());

    var timedLeg = new TimedLegStructure()
      .withLegBoard(
        new LegBoardStructure()
          .withStopPointRef(stopPointRefMapper.stopPointRef(tl.from().stop))
          .withStopPointName(internationalText(tl.from().stop.getName()))
          .withServiceDeparture(
            new ServiceDepartureStructure()
              .withTimetabledTime(scheduledDeparture)
              .withEstimatedTime(realtimeDeparture)
          )
      )
      .withLegAlight(
        new LegAlightStructure()
          .withStopPointRef(stopPointRefMapper.stopPointRef(tl.to().stop))
          .withStopPointName(internationalText(tl.to().stop.getName()))
          .withServiceArrival(
            new ServiceArrivalStructure()
              .withTimetabledTime(scheduledArrival)
              .withEstimatedTime(realtimeArrival)
          )
      )
      .withService(journeyMapper.datedJourney(tl.trip(), tl.tripPattern(), tl.serviceDate()));
    if (optionalFeatures.contains(OptionalFeature.INTERMEDIATE_STOPS)) {
      timedLeg.withLegIntermediate(mapIntermediateStops(tl.listIntermediateStops()));
    }
    return baseLeg(index, tl).withTimedLeg(timedLeg);
  }

  private List<LegIntermediateStructure> mapIntermediateStops(List<StopArrival> sas) {
    return sas
      .stream()
      .map(sa ->
        new LegIntermediateStructure()
          .withStopPointRef(stopPointRefMapper.stopPointRef(sa.place.stop))
          .withStopPointName(internationalText(sa.place.stop.getName()))
          .withServiceArrival(
            new ServiceArrivalStructure().withTimetabledTime(
              new XmlDateTime(sa.arrival.scheduledTime())
            )
          )
          .withServiceDeparture(
            new ServiceDepartureStructure().withTimetabledTime(
              new XmlDateTime(sa.departure.scheduledTime())
            )
          )
      )
      .toList();
  }

  private PlaceRefStructure placeRef(Place place) {
    var ref = new PlaceRefStructure().withName(internationalText(place.name));
    if (place.stop != null) {
      return ref.withStopPointRef(stopPointRefMapper.stopPointRef(place.stop));
    } else {
      return ref.withGeoPosition(LocationMapper.map(place.coordinate));
    }
  }

  @Nullable
  private static XmlDateTime estimatedTime(@Nullable LegCallTime time) {
    if (time.estimated() == null) {
      return null;
    } else {
      return new XmlDateTime(time.estimated().time());
    }
  }

  private static String tripId(Itinerary itinerary) {
    String buf = itinerary.startTime().toString() + itinerary.endTime().toString();
    return UUID.nameUUIDFromBytes(buf.getBytes(StandardCharsets.UTF_8)).toString();
  }

  private static LegStructure baseLeg(int index, Leg leg) {
    return new LegStructure().withDuration(leg.duration()).withId(String.valueOf(index));
  }
}
