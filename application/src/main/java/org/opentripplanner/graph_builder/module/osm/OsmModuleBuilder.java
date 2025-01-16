package org.opentripplanner.graph_builder.module.osm;

import java.util.Collection;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmProcessingParameters;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.street.model.StreetLimitationParameters;

/**
 * Builder for the {@link OsmModule}
 */
public class OsmModuleBuilder {

  private final Collection<OsmProvider> providers;
  private final Graph graph;
  private final VehicleParkingRepository parkingRepository;
  private final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private Set<String> boardingAreaRefTags = Set.of();
  private DataImportIssueStore issueStore = DataImportIssueStore.NOOP;
  private EdgeNamer edgeNamer = new DefaultNamer();
  private boolean areaVisibility = false;
  private boolean platformEntriesLinking = false;
  private boolean staticParkAndRide = false;
  private boolean staticBikeParkAndRide = false;
  private boolean includeOsmSubwayEntrances = false;
  private int maxAreaNodes;
  private StreetLimitationParameters streetLimitationParameters = new StreetLimitationParameters();

  OsmModuleBuilder(
    Collection<OsmProvider> providers,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VehicleParkingRepository parkingRepository
  ) {
    this.providers = providers;
    this.graph = graph;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.parkingRepository = parkingRepository;
  }

  public OsmModuleBuilder withBoardingAreaRefTags(Set<String> boardingAreaRefTags) {
    this.boardingAreaRefTags = boardingAreaRefTags;
    return this;
  }

  public OsmModuleBuilder withIssueStore(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
    return this;
  }

  public OsmModuleBuilder withEdgeNamer(EdgeNamer edgeNamer) {
    this.edgeNamer = edgeNamer;
    return this;
  }

  public OsmModuleBuilder withAreaVisibility(boolean areaVisibility) {
    this.areaVisibility = areaVisibility;
    return this;
  }

  public OsmModuleBuilder withPlatformEntriesLinking(boolean platformEntriesLinking) {
    this.platformEntriesLinking = platformEntriesLinking;
    return this;
  }

  public OsmModuleBuilder withStaticParkAndRide(boolean staticParkAndRide) {
    this.staticParkAndRide = staticParkAndRide;
    return this;
  }

  public OsmModuleBuilder withStaticBikeParkAndRide(boolean staticBikeParkAndRide) {
    this.staticBikeParkAndRide = staticBikeParkAndRide;
    return this;
  }

  public OsmModuleBuilder withMaxAreaNodes(int maxAreaNodes) {
    this.maxAreaNodes = maxAreaNodes;
    return this;
  }

  public OsmModuleBuilder withIncludeOsmSubwayEntrances(boolean includeOsmSubwayEntrances) {
    this.includeOsmSubwayEntrances = includeOsmSubwayEntrances;
    return this;
  }

  public OsmModuleBuilder withStreetLimitationParameters(StreetLimitationParameters parameters) {
    this.streetLimitationParameters = parameters;
    return this;
  }

  public OsmModule build() {
    return new OsmModule(
      providers,
      graph,
      osmInfoGraphBuildRepository,
      parkingRepository,
      issueStore,
      streetLimitationParameters,
      new OsmProcessingParameters(
        boardingAreaRefTags,
        edgeNamer,
        maxAreaNodes,
        areaVisibility,
        platformEntriesLinking,
        staticParkAndRide,
        staticBikeParkAndRide,
        includeOsmSubwayEntrances
      )
    );
  }
}
