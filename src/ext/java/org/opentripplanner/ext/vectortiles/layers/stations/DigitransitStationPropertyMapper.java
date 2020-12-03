package org.opentripplanner.ext.vectortiles.layers.stations;

import org.json.simple.JSONArray;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.vectortiles.PropertyMapper;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DigitransitStationPropertyMapper extends PropertyMapper<Station> {
  private final Graph graph;

  private DigitransitStationPropertyMapper(Graph graph) {this.graph = graph;}

  public static DigitransitStationPropertyMapper create(Graph graph) {
    return new DigitransitStationPropertyMapper(graph);
  }

  @Override
  public Collection<T2<String, Object>> map(Station station) {
    Collection<Stop> childStops = station.getChildStops();

    return List.of(
      new T2<>("gtfsId", station.getId().toString()),
      new T2<>("name", station.getName()),
      new T2<>("type", childStops
        .stream()
        .flatMap(stop -> graph.index.getPatternsForStop(stop).stream())
        .map(tripPattern -> tripPattern.getMode().getMainMode().name())
        .distinct()
        .collect(Collectors.joining(","))),
      new T2<>("stops", JSONArray.toJSONString(childStops
        .stream()
        .map(StationElement::getId)
        .map(FeedScopedId::toString)
        .collect(Collectors.toUnmodifiableList()))),
      new T2<>("routes", JSONArray.toJSONString(childStops
        .stream()
        .flatMap(stop -> graph.index.getRoutesForStop(stop).stream())
        .distinct()
        .map(route ->
            route.getShortName() == null
            ? Map.of("mode", route.getMode().getMainMode().name())
            : Map.of("mode", route.getMode().getMainMode().name(), "shortName", route.getShortName()))
        .collect(Collectors.toList())))
      );
  }
}
