package org.opentripplanner.transit.model._data;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.time.TimeUtils;

public record FlexTripInput(String id, List<FlexStop> stops) {

  public List<StopLocation> stopLocations() {
    return stops.stream().map(FlexTripInput.FlexStop::stop).toList();
  }

  public static FlexTripInputBuilder of(String id) {
    return new FlexTripInputBuilder(id);
  }

  public record FlexStop(StopLocation stop, int stopSequence, int windowStart, int windowEnd) {
    public StopTime toStopTime(Trip trip) {
      var st = new StopTime();
      st.setTrip(trip);
      st.setStopSequence(stopSequence);
      st.setStop(stop);
      st.setFlexWindowStart(windowStart);
      st.setFlexWindowEnd(windowEnd);
      return st;
    }
  }

  public static class FlexTripInputBuilder {

    private final String id;
    private final List<FlexStop> stops = new ArrayList<>();

    FlexTripInputBuilder(String id) {
      this.id = id;
    }

    public FlexTripInputBuilder addStop(StopLocation stop, String windowStart, String windowEnd) {
      this.stops.add(new FlexStop(stop, stops.size(), TimeUtils.time(windowStart), TimeUtils.time(windowEnd)));
      return this;
    }

    public FlexTripInput build() {
      return new FlexTripInput(id, stops);
    }
  }
}
