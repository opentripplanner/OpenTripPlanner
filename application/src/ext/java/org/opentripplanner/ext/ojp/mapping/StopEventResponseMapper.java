package org.opentripplanner.ext.ojp.mapping;

import static org.opentripplanner.ext.ojp.mapping.StopEventResponseMapper.OptionalFeature.REALTIME_DATA;
import static org.opentripplanner.ext.ojp.mapping.TextMapper.internationalText;

import de.vdv.ojp20.CallAtNearStopStructure;
import de.vdv.ojp20.CallAtStopStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.OJPStopEventDeliveryStructure;
import de.vdv.ojp20.ServiceArrivalStructure;
import de.vdv.ojp20.ServiceDepartureStructure;
import de.vdv.ojp20.StopEventResultStructure;
import de.vdv.ojp20.StopEventStructure;
import jakarta.xml.bind.JAXBElement;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.ext.ojp.service.CallAtStop;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.ojp.time.XmlDateTime;

/**
 * Maps the OTP-internal data types into OJP responses.
 */
public class StopEventResponseMapper {

  private final Function<String, Optional<String>> resolveFeedLanguage;
  private final FeedScopedIdMapper idMapper;
  private final StopRefMapper stopPointRefMapper;

  public enum OptionalFeature {
    PREVIOUS_CALLS,
    ONWARD_CALLS,
    REALTIME_DATA,
  }

  private final Set<OptionalFeature> optionalFeatures;
  private final ZoneId zoneId;

  public StopEventResponseMapper(
    Set<OptionalFeature> optionalFeatures,
    ZoneId zoneId,
    FeedScopedIdMapper idMapper,
    Function<String, Optional<String>> resolveFeedLanguage
  ) {
    this.optionalFeatures = optionalFeatures;
    this.zoneId = zoneId;
    this.idMapper = idMapper;
    this.stopPointRefMapper = new StopRefMapper(idMapper);
    this.resolveFeedLanguage = resolveFeedLanguage;
  }

  public OJP mapCalls(List<CallAtStop> calls, ZonedDateTime timestamp) {
    List<JAXBElement<StopEventResultStructure>> stopEvents = calls
      .stream()
      .map(call -> this.stopEventResult(call))
      .map(JaxbElementMapper::jaxbElement)
      .toList();

    var sed = new OJPStopEventDeliveryStructure().withStatus(true);
    stopEvents.forEach(sed::withRest);

    var serviceDelivery = ServiceDeliveryMapper.serviceDelivery(
      timestamp
    ).withAbstractFunctionalServiceDelivery(JaxbElementMapper.jaxbElement(sed));

    var response = new OJPResponseStructure().withServiceDelivery(serviceDelivery);
    return new OJP().withOJPResponse(response);
  }

  private StopEventResultStructure stopEventResult(CallAtStop call) {
    var callAtNearStop = new CallAtNearStopStructure()
      .withCallAtStop(callAtStop(call.tripTimeOnDate()))
      .withWalkDuration(call.walkTime());

    var mapper = new DatedJourneyMapper(idMapper);
    var stopEvent = new StopEventStructure()
      .withThisCall(callAtNearStop)
      .withService(mapper.datedJourney(call.tripTimeOnDate(), lang(call.tripTimeOnDate())));
    if (optionalFeatures.contains(OptionalFeature.PREVIOUS_CALLS)) {
      call
        .tripTimeOnDate()
        .previousTimes()
        .forEach(previous -> stopEvent.withPreviousCall(callAtNearStop(previous)));
    }
    if (optionalFeatures.contains(OptionalFeature.ONWARD_CALLS)) {
      call
        .tripTimeOnDate()
        .nextTimes()
        .forEach(next -> stopEvent.withOnwardCall(callAtNearStop(next)));
    }
    return new StopEventResultStructure()
      .withStopEvent(stopEvent)
      .withId(eventId(call.tripTimeOnDate()));
  }

  private String eventId(TripTimeOnDate tripTimeOnDate) {
    var bytes = (tripTimeOnDate.getStopTimeKey().toString() +
      tripTimeOnDate.getServiceDay()).getBytes(StandardCharsets.UTF_8);
    return UUID.nameUUIDFromBytes(bytes).toString();
  }

  private CallAtStopStructure callAtStop(TripTimeOnDate tripTimeOnDate) {
    var stop = tripTimeOnDate.getStop();
    var stopPointRef = stopPointRefMapper.stopPointRef(stop);
    return new CallAtStopStructure()
      .withStopPointRef(stopPointRef)
      .withStopPointName(internationalText(stop.getName(), lang(tripTimeOnDate)))
      .withServiceArrival(serviceArrival(tripTimeOnDate))
      .withServiceDeparture(serviceDeparture(tripTimeOnDate))
      .withOrder(tripTimeOnDate.getGtfsSequence())
      .withNoBoardingAtStop(isNone(tripTimeOnDate.getPickupType()))
      .withNoAlightingAtStop(isNone(tripTimeOnDate.getDropoffType()))
      .withPlannedQuay(internationalText(stop.getPlatformCode(), lang(tripTimeOnDate)))
      .withNotServicedStop(tripTimeOnDate.isCancelledStop());
  }

  private CallAtNearStopStructure callAtNearStop(TripTimeOnDate tripTimeOnDate) {
    return new CallAtNearStopStructure().withCallAtStop(callAtStop(tripTimeOnDate));
  }

  @Nullable
  private String lang(TripTimeOnDate tripTimeOnDate) {
    var agencyLang = tripTimeOnDate.getTrip().getRoute().getAgency().getLang();
    if (agencyLang != null) {
      return agencyLang;
    } else {
      return resolveFeedLanguage.apply(tripTimeOnDate.getTrip().getId().getFeedId()).orElse(null);
    }
  }

  private static boolean isNone(PickDrop pickDrop) {
    return pickDrop == PickDrop.NONE;
  }

  private ServiceDepartureStructure serviceDeparture(TripTimeOnDate tripTimeOnDate) {
    var departure = new ServiceDepartureStructure().withTimetabledTime(
      new XmlDateTime(tripTimeOnDate.scheduledDeparture().atZone(zoneId))
    );
    tripTimeOnDate
      .realtimeDeparture()
      .filter(d -> optionalFeatures.contains(REALTIME_DATA))
      .ifPresent(time -> departure.withEstimatedTime(new XmlDateTime(time.atZone(zoneId))));
    return departure;
  }

  private ServiceArrivalStructure serviceArrival(TripTimeOnDate tripTimeOnDate) {
    var arrival = new ServiceArrivalStructure().withTimetabledTime(
      new XmlDateTime(tripTimeOnDate.scheduledArrival().atZone(zoneId))
    );

    tripTimeOnDate
      .realtimeArrival()
      .filter(d -> optionalFeatures.contains(REALTIME_DATA))
      .ifPresent(time -> arrival.withEstimatedTime(new XmlDateTime(time.atZone(zoneId))));
    return arrival;
  }
}
