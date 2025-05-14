package org.opentripplanner.ext.trias.mapping;

import static org.opentripplanner.ext.trias.mapping.StopEventResponseMapper.OptionalFeature.REALTIME_DATA;

import de.vdv.ojp20.CallAtNearStopStructure;
import de.vdv.ojp20.CallAtStopStructure;
import de.vdv.ojp20.DatedJourneyStructure;
import de.vdv.ojp20.InternationalTextStructure;
import de.vdv.ojp20.JourneyRefStructure;
import de.vdv.ojp20.ModeStructure;
import de.vdv.ojp20.OJP;
import de.vdv.ojp20.OJPResponseStructure;
import de.vdv.ojp20.OJPStopEventDeliveryStructure;
import de.vdv.ojp20.OperatingDayRefStructure;
import de.vdv.ojp20.ServiceArrivalStructure;
import de.vdv.ojp20.ServiceDepartureStructure;
import de.vdv.ojp20.StopEventResultStructure;
import de.vdv.ojp20.StopEventStructure;
import de.vdv.ojp20.siri.DefaultedTextStructure;
import de.vdv.ojp20.siri.DirectionRefStructure;
import de.vdv.ojp20.siri.LineRefStructure;
import de.vdv.ojp20.siri.OperatorRefStructure;
import de.vdv.ojp20.siri.StopPointRefStructure;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import org.opentripplanner.ext.trias.id.IdResolver;
import org.opentripplanner.ext.trias.service.CallAtStop;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.ojp.time.XmlDateTime;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Maps the OTP-internal data types into OJP responses.
 */
public class StopEventResponseMapper {

  private final Function<String, Optional<String>> resolveFeedLanguage;
  private final IdResolver idResolver;

  public enum OptionalFeature {
    PREVIOUS_CALLS,
    ONWARD_CALLS,
    REALTIME_DATA,
  }

  private static final String OJP_NAMESPACE = "http://www.vdv.de/ojp";
  private final Set<OptionalFeature> optionalFeatures;
  private final ZoneId zoneId;

  public StopEventResponseMapper(
    Set<OptionalFeature> optionalFeatures,
    ZoneId zoneId,
    IdResolver idResolver,
    Function<String, Optional<String>> resolveFeedLanguage
  ) {
    this.optionalFeatures = optionalFeatures;
    this.zoneId = zoneId;
    this.idResolver = idResolver;
    this.resolveFeedLanguage = resolveFeedLanguage;
  }

  public OJP mapCalls(List<CallAtStop> calls, ZonedDateTime timestamp) {
    List<JAXBElement<StopEventResultStructure>> stopEvents = calls
      .stream()
      .map(call -> this.stopEventResult(call))
      .map(StopEventResponseMapper::jaxbElement)
      .toList();

    var sed = new OJPStopEventDeliveryStructure().withStatus(true);
    stopEvents.forEach(sed::withRest);

    var serviceDelivery = ServiceDeliveryMapper.serviceDelivery(
      timestamp
    ).withAbstractFunctionalServiceDelivery(StopEventResponseMapper.jaxbElement(sed));

    var response = new OJPResponseStructure().withServiceDelivery(serviceDelivery);
    return new OJP().withOJPResponse(response);
  }

  private StopEventResultStructure stopEventResult(CallAtStop call) {
    var callAtNearStop = new CallAtNearStopStructure()
      .withCallAtStop(callAtStop(call.tripTimeOnDate()))
      .withWalkDuration(call.walkTime());

    var stopEvent = new StopEventStructure()
      .withThisCall(callAtNearStop)
      .withService(datedJourney(call.tripTimeOnDate()));
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
    var bytes =
      (tripTimeOnDate.getStopTimeKey().toString() + tripTimeOnDate.getServiceDay()).getBytes(
          StandardCharsets.UTF_8
        );
    return UUID.nameUUIDFromBytes(bytes).toString();
  }

  private DatedJourneyStructure datedJourney(TripTimeOnDate tripTimeOnDate) {
    final Route route = tripTimeOnDate.getTrip().getRoute();
    var firstStop = tripTimeOnDate.pattern().getStops().getFirst();
    var lastStop = tripTimeOnDate.pattern().getStops().getLast();
    return new DatedJourneyStructure()
      .withJourneyRef(
        new JourneyRefStructure().withValue(idResolver.toString(tripTimeOnDate.getTrip().getId()))
      )
      .withOperatingDayRef(
        new OperatingDayRefStructure().withValue(tripTimeOnDate.getServiceDay().toString())
      )
      .withLineRef(new LineRefStructure().withValue(idResolver.toString(route.getId())))
      .withMode(new ModeStructure().withPtMode(PtModeMapper.map(route.getMode())))
      .withPublishedServiceName(internationalText(route.getName(), lang(tripTimeOnDate)))
      .withOperatorRef(
        new OperatorRefStructure().withValue(idResolver.toString(route.getAgency().getId()))
      )
      .withOriginStopPointRef(stopPointRef(firstStop))
      .withOriginText(internationalText(firstStop.getName(), lang(tripTimeOnDate)))
      .withDestinationStopPointRef(stopPointRef(lastStop))
      .withDestinationText(internationalText(tripTimeOnDate.getHeadsign(), lang(tripTimeOnDate)))
      .withRouteDescription(internationalText(route.getDescription(), lang(tripTimeOnDate)))
      .withCancelled(tripTimeOnDate.getTripTimes().isCanceled())
      .withDirectionRef(
        new DirectionRefStructure().withValue(tripTimeOnDate.pattern().getDirection().toString())
      );
  }

  private CallAtStopStructure callAtStop(TripTimeOnDate tripTimeOnDate) {
    var stop = tripTimeOnDate.getStop();
    var stopPointRef = stopPointRef(stop);
    return new CallAtStopStructure()
      .withStopPointRef(stopPointRef)
      .withStopPointName(internationalText(stop.getName(), lang(tripTimeOnDate)))
      .withServiceArrival(serviceArrival(tripTimeOnDate))
      .withServiceDeparture(serviceDeparture(tripTimeOnDate))
      .withOrder(BigInteger.valueOf(tripTimeOnDate.getGtfsSequence()))
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
    var departure = new ServiceDepartureStructure()
      .withTimetabledTime(new XmlDateTime(tripTimeOnDate.scheduledDeparture().atZone(zoneId)));
    tripTimeOnDate
      .realtimeDeparture()
      .filter(d -> optionalFeatures.contains(REALTIME_DATA))
      .ifPresent(time -> departure.withEstimatedTime(new XmlDateTime(time.atZone(zoneId))));
    return departure;
  }

  private ServiceArrivalStructure serviceArrival(TripTimeOnDate tripTimeOnDate) {
    var arrival = new ServiceArrivalStructure()
      .withTimetabledTime(new XmlDateTime(tripTimeOnDate.scheduledArrival().atZone(zoneId)));

    tripTimeOnDate
      .realtimeArrival()
      .filter(d -> optionalFeatures.contains(REALTIME_DATA))
      .ifPresent(time -> arrival.withEstimatedTime(new XmlDateTime(time.atZone(zoneId))));
    return arrival;
  }

  private StopPointRefStructure stopPointRef(StopLocation stop) {
    return new StopPointRefStructure().withValue(idResolver.toString(stop.getId()));
  }

  private static InternationalTextStructure internationalText(I18NString string, String lang) {
    if (string == null) {
      return null;
    } else {
      return internationalText(string.toString(), lang);
    }
  }

  private static InternationalTextStructure internationalText(String string, String lang) {
    if (string == null) {
      return null;
    } else {
      return new InternationalTextStructure()
        .withText(new DefaultedTextStructure().withValue(string).withLang(lang));
    }
  }

  private static <T> JAXBElement<T> jaxbElement(T value) {
    var xmlType = value.getClass().getAnnotation(XmlType.class);
    return new JAXBElement<>(
      new QName(OJP_NAMESPACE, getName(xmlType)),
      (Class<T>) value.getClass(),
      value
    );
  }

  private static String getName(XmlType xmlType) {
    return xmlType.name().replaceAll("Structure", "");
  }
}
