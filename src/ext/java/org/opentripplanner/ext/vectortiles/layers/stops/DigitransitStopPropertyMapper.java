package org.opentripplanner.ext.vectortiles.layers.stops;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class DigitransitStopPropertyMapper extends PropertyMapper<TransitStopVertex> {

  private final Graph graph;

  private DigitransitStopPropertyMapper(Graph graph) {
    this.graph = graph;
  }

  public static DigitransitStopPropertyMapper create(Graph graph) {
    return new DigitransitStopPropertyMapper(graph);
  }

  @Override
  public Collection<T2<String, Object>> map(TransitStopVertex input) {
    Stop stop = input.getStop();
    Collection<TripPattern> patternsForStop = graph.index.getPatternsForStop(stop);

    String type = patternsForStop
      .stream()
      .map(TripPattern::getMode)
      .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
      .entrySet()
      .stream()
      .max(Map.Entry.comparingByValue())
      .map(Map.Entry::getKey)
      .map(Enum::name)
      .orElse(null);

    String patterns = JSONArray.toJSONString(
      patternsForStop
        .stream()
        .map(tripPattern -> {
          int stopPos = tripPattern.findStopPosition(stop);
          Timetable scheduledTimetable = tripPattern.getScheduledTimetable();
          String headsign;
          if (stopPos > -1 && !scheduledTimetable.getTripTimes().isEmpty()) {
            headsign = scheduledTimetable.getTripTimes().get(0).getHeadsign(stopPos);
          } else if (stopPos > -1 && !scheduledTimetable.getFrequencyEntries().isEmpty()) {
            headsign =
              scheduledTimetable.getFrequencyEntries().get(0).tripTimes.getHeadsign(stopPos);
          } else {
            headsign = "Not Available";
          }
          JSONObject pattern = new JSONObject();
          pattern.put("headsign", headsign);
          pattern.put("type", tripPattern.getRoute().getMode().name());
          pattern.put("shortName", tripPattern.getRoute().getShortName());
          return pattern;
        })
        .collect(Collectors.toList())
    );

    return List.of(
      new T2<>("gtfsId", stop.getId().toString()),
      // Name is I18NString now, we return default name
      new T2<>("name", stop.getName().toString()),
      new T2<>("code", stop.getCode()),
      new T2<>("platform", stop.getPlatformCode()),
      new T2<>("desc", stop.getDescription()),
      new T2<>(
        "parentStation",
        stop.getParentStation() != null ? stop.getParentStation().getId() : "null"
      ),
      new T2<>("type", type),
      new T2<>("patterns", patterns)
    );
  }
}
