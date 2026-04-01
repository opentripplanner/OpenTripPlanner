package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.collection.ListUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * A simple data structure that is used by the {@link TransitTestEnvironment} to create
 * trips, trips on date and patterns.
 */
public class TripInput {

  private final String id;
  private final List<StopCallInput> stops = new ArrayList<>();

  // Null means use the default route
  @Nullable
  private Route route;

  // Null means use default service date
  @Nullable
  private List<LocalDate> serviceDates = null;

  @Nullable
  private I18NString headsign;

  @Nullable
  private String tripOnServiceDateId;

  @Nullable
  private TransitMode mode;

  @Nullable
  private String netexSubmode;

  @Nullable
  private String netexInternalPlanningCode;

  private final boolean isFlex;

  private TripInput(String id, boolean isFlex) {
    this.id = id;
    this.isFlex = isFlex;
  }

  public static TripInput of(String id) {
    return new TripInput(id, false);
  }

  public static TripInput flex(String id) {
    return new TripInput(id, true);
  }

  public String id() {
    return id;
  }

  public List<StopTime> stopTimes(Trip trip) {
    return IntStream.range(0, stops.size())
      .mapToObj(i -> stops.get(i).toStopTime(trip, i))
      .toList();
  }

  public List<StopLocation> stopLocations() {
    return stops.stream().map(StopCallInput::stopLocation).toList();
  }

  @Nullable
  public I18NString headsign() {
    return headsign;
  }

  @Nullable
  public List<LocalDate> serviceDates() {
    return serviceDates;
  }

  @Nullable
  public Route route() {
    return route;
  }

  @Nullable
  public String tripOnServiceDateId() {
    return tripOnServiceDateId;
  }

  @Nullable
  public TransitMode mode() {
    return mode;
  }

  @Nullable
  public String netexSubmode() {
    return netexSubmode;
  }

  @Nullable
  public String netexInternalPlanningCode() {
    return netexInternalPlanningCode;
  }

  public boolean isFlex() {
    return isFlex;
  }

  public TripInput addStop(RegularStop stop, String arrivalTime, String departureTime) {
    return addStopWithHeadsign(stop, arrivalTime, departureTime, null);
  }

  public TripInput addStop(
    RegularStop stop,
    String arrivalTime,
    String departureTime,
    PickDrop pickup,
    PickDrop dropoff
  ) {
    stops.add(
      new RegularStopCallInput(
        stop,
        TimeUtils.time(arrivalTime),
        TimeUtils.time(departureTime),
        null,
        pickup,
        dropoff
      )
    );
    return this;
  }

  public TripInput addStop(RegularStop stopId, String arrivalAndDeparture) {
    return addStop(stopId, arrivalAndDeparture, arrivalAndDeparture);
  }

  public TripInput addStopWithHeadsign(
    RegularStop stop,
    String arrivalTime,
    String departureTime,
    String headsign
  ) {
    stops.add(
      new RegularStopCallInput(
        stop,
        TimeUtils.time(arrivalTime),
        TimeUtils.time(departureTime),
        headsign
      )
    );
    return this;
  }

  public TripInput addStop(AreaStop stop, String windowStart, String windowEnd) {
    stops.add(new FlexStopCallInput(stop, TimeUtils.time(windowStart), TimeUtils.time(windowEnd)));
    return this;
  }

  public TripInput withRoute(Route route) {
    this.route = route;
    return this;
  }

  public TripInput withHeadsign(I18NString headsign) {
    this.headsign = headsign;
    return this;
  }

  public TripInput withServiceDates(LocalDate... serviceDates) {
    var list = Arrays.stream(serviceDates).toList();
    ListUtils.requireAtLeastNElements(list, 1);
    this.serviceDates = list;
    return this;
  }

  public TripInput withWithTripOnServiceDate(String tripOnServiceDateId) {
    this.tripOnServiceDateId = tripOnServiceDateId;
    return this;
  }

  public TripInput withMode(TransitMode mode) {
    this.mode = mode;
    return this;
  }

  public TripInput withNetexSubmode(String netexSubmode) {
    this.netexSubmode = netexSubmode;
    return this;
  }

  public TripInput withNetexInternalPlanningCode(String netexInternalPlanningCode) {
    this.netexInternalPlanningCode = netexInternalPlanningCode;
    return this;
  }

  private interface StopCallInput {
    StopTime toStopTime(Trip trip, int stopSequence);
    StopLocation stopLocation();
  }

  private record RegularStopCallInput(
    RegularStop stop,
    int arrivalTime,
    int departureTime,
    @Nullable String headsign,
    @Nullable PickDrop pickupType,
    @Nullable PickDrop dropoffType
  ) implements StopCallInput {
    RegularStopCallInput(
      RegularStop stop,
      int arrivalTime,
      int departureTime,
      @Nullable String headsign
    ) {
      this(stop, arrivalTime, departureTime, headsign, null, null);
    }

    public StopTime toStopTime(Trip trip, int stopSequence) {
      var st = new StopTime();
      st.setTrip(trip);
      st.setStopSequence(stopSequence);
      st.setStop(stop);
      st.setArrivalTime(arrivalTime);
      st.setDepartureTime(departureTime);
      if (headsign != null) {
        st.setStopHeadsign(new NonLocalizedString(headsign));
      }
      if (pickupType != null) {
        st.setPickupType(pickupType);
      }
      if (dropoffType != null) {
        st.setDropOffType(dropoffType);
      }
      return st;
    }

    public StopLocation stopLocation() {
      return stop;
    }
  }

  private record FlexStopCallInput(AreaStop stop, int windowStart, int windowEnd) implements
    StopCallInput {
    public StopTime toStopTime(Trip trip, int stopSequence) {
      var st = new StopTime();
      st.setTrip(trip);
      st.setStopSequence(stopSequence);
      st.setStop(stop);
      st.setFlexWindowStart(windowStart);
      st.setFlexWindowEnd(windowEnd);
      return st;
    }

    public StopLocation stopLocation() {
      return stop;
    }
  }
}
