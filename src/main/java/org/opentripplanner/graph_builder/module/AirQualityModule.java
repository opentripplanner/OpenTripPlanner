package org.opentripplanner.graph_builder.module;

import fi.metatavu.airquality.AirQualityDataFile;
import fi.metatavu.airquality.AirQualityEdgeUpdater;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class AirQualityModule implements GraphBuilderModule {
  private final AirQualityDataFile dataFile;

  public AirQualityModule(AirQualityDataFile dataFile) {
    this.dataFile = dataFile;
  }

  @Override
  public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra, DataImportIssueStore issueStore) {
    AirQualityEdgeUpdater airQualityEdgeUpdater = new AirQualityEdgeUpdater(dataFile, graph.getStreetEdges());
    airQualityEdgeUpdater.updateEdges();
  }

  @Override
  public void checkInputs() throws Exception {
    if (!dataFile.isValid()) {
      throw new Exception(dataFile.getError());
    }
  }
}
