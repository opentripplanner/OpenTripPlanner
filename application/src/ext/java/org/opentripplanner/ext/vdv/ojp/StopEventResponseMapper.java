package org.opentripplanner.ext.vdv.ojp;

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
import de.vdv.ojp20.ServiceDepartureStructure;
import de.vdv.ojp20.StopEventResultStructure;
import de.vdv.ojp20.StopEventStructure;
import de.vdv.ojp20.siri.DefaultedTextStructure;
import de.vdv.ojp20.siri.LineRefStructure;
import de.vdv.ojp20.siri.OperatorRefStructure;
import de.vdv.ojp20.siri.ParticipantRefStructure;
import de.vdv.ojp20.siri.ServiceDelivery;
import de.vdv.ojp20.siri.StopPointRefStructure;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.xml.namespace.QName;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;

public class StopEventResponseMapper {

  private static final String OJP_NAMESPACE = "http://www.vdv.de/ojp";
  private final ZoneId zoneId;

  public StopEventResponseMapper(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  public OJP mapStopTimesInPattern(List<TripTimeOnDate> tripTimesOnDate, Instant timestamp) {
    List<JAXBElement<StopEventResultStructure>> stopEvents = tripTimesOnDate
      .stream()
      .map(this::stopEventResult)
      .map(StopEventResponseMapper::jaxbElement)
      .toList();

    var sed = new OJPStopEventDeliveryStructure().withStatus(true);
    stopEvents.forEach(sed::withRest);

    var serviceDelivery = new ServiceDelivery()
      .withAbstractFunctionalServiceDelivery(jaxbElement(sed))
      .withResponseTimestamp(timestamp.atZone(zoneId))
      .withProducerRef(new ParticipantRefStructure().withValue("OpenTripPlanner"));

    var response = new OJPResponseStructure().withServiceDelivery(serviceDelivery);
    return new OJP().withOJPResponse(response);
  }

  private StopEventResultStructure stopEventResult(TripTimeOnDate tripTimeOnDate) {
    var call = new CallAtNearStopStructure().withCallAtStop(callAtStop(tripTimeOnDate));
    var stopEvent = new StopEventStructure()
      .withThisCall(call)
      .withService(datedJourney(tripTimeOnDate));
    return new StopEventResultStructure().withStopEvent(stopEvent).withId(eventId(tripTimeOnDate));
  }

  private String eventId(TripTimeOnDate tripTimeOnDate) {
    var x = new StringBuilder()
      .append(tripTimeOnDate.getStopTimeKey().toString())
      .append(tripTimeOnDate.getServiceDay())
      .toString()
      .getBytes(StandardCharsets.UTF_8);
    return UUID.nameUUIDFromBytes(x).toString();
  }

  private static DatedJourneyStructure datedJourney(TripTimeOnDate tripTimeOnDate) {
    final Route route = tripTimeOnDate.getTrip().getRoute();
    var firstStop = tripTimeOnDate.pattern().getStops().getFirst();
    var lastStop = tripTimeOnDate.pattern().getStops().getLast();
    return new DatedJourneyStructure()
      .withJourneyRef(new JourneyRefStructure().withValue(tripTimeOnDate.getTrip().getId().getId()))
      .withOperatingDayRef(
        new OperatingDayRefStructure().withValue(tripTimeOnDate.getServiceDay().toString())
      )
      .withLineRef(new LineRefStructure().withValue(route.getId().getId()))
      .withMode(new ModeStructure().withPtMode(PtModeMapper.map(route.getMode())))
      .withPublishedServiceName(internationalText(route.getName(), lang(tripTimeOnDate)))
      .withOperatorRef(new OperatorRefStructure().withValue(route.getAgency().getId().getId()))
      .withOriginStopPointRef(stopPointRef(firstStop))
      .withOriginText(internationalText(firstStop.getName(), lang(tripTimeOnDate)))
      .withDestinationStopPointRef(stopPointRef(lastStop))
      .withDestinationText(internationalText(tripTimeOnDate.getHeadsign(), lang(tripTimeOnDate)))
      .withRouteDescription(internationalText(route.getDescription(), lang(tripTimeOnDate)));
  }

  private CallAtStopStructure callAtStop(TripTimeOnDate tripTimeOnDate) {
    var stop = tripTimeOnDate.getStop();
    var stopPointRef = stopPointRef(stop);
    return new CallAtStopStructure()
      .withStopPointRef(stopPointRef)
      .withStopPointName(internationalText(stop.getName(), lang(tripTimeOnDate)))
      .withServiceDeparture(serviceDeparture(tripTimeOnDate))
      .withOrder(BigInteger.valueOf(tripTimeOnDate.getGtfsSequence()))
      .withNoBoardingAtStop(isNone(tripTimeOnDate.getPickupType()))
      .withNoAlightingAtStop(isNone(tripTimeOnDate.getDropoffType()))
      .withPlannedQuay(internationalText(stop.getPlatformCode(), lang(tripTimeOnDate)));
  }

  private static String lang(TripTimeOnDate tripTimeOnDate) {
    return tripTimeOnDate.getTrip().getRoute().getAgency().getLang();
  }

  private static boolean isNone(PickDrop pickDrop) {
    return pickDrop == PickDrop.NONE;
  }

  private ServiceDepartureStructure serviceDeparture(TripTimeOnDate tripTimeOnDate) {
    var departure = new ServiceDepartureStructure()
      .withTimetabledTime(scheduledDeparture(tripTimeOnDate));
    if (tripTimeOnDate.isRealtime()) {
      departure.withEstimatedTime(realtimeDeparture(tripTimeOnDate));
    }
    return departure;
  }

  private static StopPointRefStructure stopPointRef(StopLocation stop) {
    return new StopPointRefStructure().withValue(stop.getId().getId());
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

  private ZonedDateTime scheduledDeparture(TripTimeOnDate tripTimeOnDate) {
    var localTime = LocalTime.ofSecondOfDay(tripTimeOnDate.getScheduledDeparture());
    return tripTimeOnDate.getServiceDay().atTime(localTime).atZone(zoneId);
  }

  private ZonedDateTime realtimeDeparture(TripTimeOnDate tripTimeOnDate) {
    var localTime = LocalTime.ofSecondOfDay(tripTimeOnDate.getRealtimeDeparture());
    return tripTimeOnDate.getServiceDay().atTime(localTime).atZone(zoneId);
  }

  public static <T> JAXBElement<T> jaxbElement(T value) {
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
