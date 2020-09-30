package org.opentripplanner.ext.vectortiles.layers.stops;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DigitransitStopPropertyMapper extends PropertyMapper<TransitStopVertex> {
  private final Graph graph;

  public static DigitransitStopPropertyMapper create(Graph graph) {
    return new DigitransitStopPropertyMapper(graph);
  }

  private DigitransitStopPropertyMapper(Graph graph) {this.graph = graph;}

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

    String patterns = JSONArray.toJSONString(patternsForStop.stream().map(tripPattern -> {
      JSONObject pattern = new JSONObject();
      pattern.put("headsign", tripPattern.scheduledTimetable.tripTimes.get(0).getHeadsign(tripPattern.getStopIndex(stop)));
      pattern.put("type", tripPattern.route.getMode().name());
      pattern.put("shortName", tripPattern.route.getShortName());
      return pattern;
    }).collect(Collectors.toList()));

    return List.of(
      new T2<>("gtfsId", stop.getId().toString()),
      new T2<>("name", stop.getName()),
      new T2<>("code", stop.getCode()),
      new T2<>("platform", stop.getPlatformCode()),
      new T2<>("desc", stop.getDescription()),
      new T2<>("parentStation", stop.getParentStation() != null ? stop.getParentStation().getId() : "null"),
      new T2<>("type", type),
      new T2<>("patterns", patterns));
  }
}
