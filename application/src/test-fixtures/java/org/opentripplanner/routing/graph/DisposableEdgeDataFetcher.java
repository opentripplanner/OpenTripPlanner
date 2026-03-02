package org.opentripplanner.routing.graph;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.test.support.GeoJsonIo;

public class DisposableEdgeDataFetcher {

  private final DisposableEdgeCollection temp;

  public DisposableEdgeDataFetcher(DisposableEdgeCollection temp) {
    this.temp = temp;
  }

  public Set<String> summarize() {
    return temp
      .listEdges()
      .stream()
      .map(StreetSummarizer::summarizeEdge)
      .collect(Collectors.toSet());
  }

  public String geojsonUrl() {
    var vertices = temp
      .listEdges()
      .stream()
      .flatMap(e -> Stream.of(e.getFromVertex(), e.getToVertex()))
      .toList();
    return GeoJsonIo.toUrl(temp.listEdges(), vertices);
  }
}
