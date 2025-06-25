package org.opentripplanner.model.impl;

import com.csvreader.CsvReader;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.TransitMode;

@Module
public class SubmodeMappingModule implements GraphBuilderModule {

  private static final String INPUT_FEED_TYPE = "Input feed type";
  private static final String INPUT_LABEL = "Input label";
  private static final String GTFS_ROUTE_TYPE = "GTFS route type";
  private static final String NETEX_SUBMODE = "NeTEx submode";
  private static final String REPLACEMENT_MODE = "Replacement mode";

  private final GraphBuilderDataSources graphBuilderDataSources;
  private final Graph graph;

  public SubmodeMappingModule(GraphBuilderDataSources graphBuilderDataSources, Graph graph) {
    this.graphBuilderDataSources = graphBuilderDataSources;
    this.graph = graph;
  }

  @Provides
  @Singleton
  public static SubmodeMappingService submodeMappingService(Graph graph) {
    return new SubmodeMappingService(graph.submodeMapping);
  }

  public void read(DataSource dataSource, Map<SubmodeMappingMatcher, SubmodeMappingRow> map) {
    try {
      var reader = new CsvReader(dataSource.asInputStream(), StandardCharsets.UTF_8);
      reader.readHeaders();
      var headers = reader.getHeaders();
      while (reader.readRecord()) {
        var inputFeedType = reader.get(INPUT_FEED_TYPE);
        var inputLabel = reader.get(INPUT_LABEL);
        var gtfsRouteType = reader.get(GTFS_ROUTE_TYPE);
        var netexSubmode = reader.get(NETEX_SUBMODE);
        var replacementMode = reader.get(REPLACEMENT_MODE);
        var matcher = new SubmodeMappingMatcher(inputFeedType, inputLabel);
        var row = new SubmodeMappingRow(
          Integer.parseInt(gtfsRouteType),
          netexSubmode,
          TransitMode.valueOf(replacementMode)
        );
        map.put(matcher, row);
      }
    } catch (IOException ioe) {
      throw new OtpAppException("cannot read submode mapping config file", ioe);
    }
  }

  public void useDefaultMapping(Map<SubmodeMappingMatcher, SubmodeMappingRow> map) {
    map.put(
      new SubmodeMappingMatcher("GTFS", "714"),
      new SubmodeMappingRow(714, "railReplacementBus", TransitMode.RAIL)
    );
    map.put(
      new SubmodeMappingMatcher("NeTEx", "railreplacementBus"),
      new SubmodeMappingRow(714, "railReplacementBus", TransitMode.RAIL)
    );
  }

  @Override
  public void buildGraph() {
    var dataSources = graphBuilderDataSources.getSubmodeMappingDataSource();
    var map = new HashMap<SubmodeMappingMatcher, SubmodeMappingRow>();
    boolean mapWritten = false;
    for (var dataSource : dataSources) {
      read(dataSource, map);
      mapWritten = true;
    }
    if (!mapWritten) {
      useDefaultMapping(map);
    }
    graph.submodeMapping = map;
  }
}
