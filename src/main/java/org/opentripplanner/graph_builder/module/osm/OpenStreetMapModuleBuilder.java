package org.opentripplanner.graph_builder.module.osm;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;

public class OpenStreetMapModuleBuilder {

  private final Collection<OpenStreetMapProvider> providers;
  private final Graph graph;
  private Set<String> boardingAreaRefTags = Set.of();
  private DataImportIssueStore issueStore = DataImportIssueStore.NOOP;
  private CustomNamer customNamer;
  private boolean areaVisibility = false;
  private boolean platformEntriesLinking = false;
  private boolean staticParkAndRide = false;
  private boolean staticBikeParkAndRide = false;
  private boolean banDiscouragedWalking = false;
  private boolean banDiscouragedBiking = false;
  private int maxAreaNodes;

  private OpenStreetMapModuleBuilder(Collection<OpenStreetMapProvider> providers, Graph graph) {
    this.providers = providers;
    this.graph = graph;
  }

  public static OpenStreetMapModuleBuilder of(
    Collection<OpenStreetMapProvider> providers,
    Graph graph
  ) {
    return new OpenStreetMapModuleBuilder(providers, graph);
  }

  public static OpenStreetMapModuleBuilder of(OpenStreetMapProvider provider, Graph graph) {
    return new OpenStreetMapModuleBuilder(List.of(provider), graph);
  }

  public OpenStreetMapModuleBuilder withBoardingAreaRefTags(Set<String> boardingAreaRefTags) {
    this.boardingAreaRefTags = boardingAreaRefTags;
    return this;
  }

  public OpenStreetMapModuleBuilder setIssueStore(DataImportIssueStore issueStore) {
    this.issueStore = issueStore;
    return this;
  }

  public OpenStreetMapModuleBuilder withCustomNamer(CustomNamer customNamer) {
    this.customNamer = customNamer;
    return this;
  }

  public OpenStreetMapModuleBuilder withAreaVisibility(boolean areaVisibility) {
    this.areaVisibility = areaVisibility;
    return this;
  }

  public OpenStreetMapModuleBuilder withPlatformEntriesLinking(boolean platformEntriesLinking) {
    this.platformEntriesLinking = platformEntriesLinking;
    return this;
  }

  public OpenStreetMapModuleBuilder withStaticParkAndRide(boolean staticParkAndRide) {
    this.staticParkAndRide = staticParkAndRide;
    return this;
  }

  public OpenStreetMapModuleBuilder withStaticBikeParkAndRide(boolean staticBikeParkAndRide) {
    this.staticBikeParkAndRide = staticBikeParkAndRide;
    return this;
  }

  public OpenStreetMapModuleBuilder withBanDiscouragedWalking(boolean banDiscouragedWalking) {
    this.banDiscouragedWalking = banDiscouragedWalking;
    return this;
  }

  public OpenStreetMapModuleBuilder withBanDiscouragedBiking(boolean banDiscouragedBiking) {
    this.banDiscouragedBiking = banDiscouragedBiking;
    return this;
  }

  public OpenStreetMapModuleBuilder withMaxAreaNodes(int maxAreaNodes) {
    this.maxAreaNodes = maxAreaNodes;
    return this;
  }

  public OpenStreetMapModule build() {
    return new OpenStreetMapModule(
      providers,
      graph,
      issueStore,
      new OpenStreetMapOptions(
        boardingAreaRefTags,
        customNamer,
        maxAreaNodes,
        areaVisibility,
        platformEntriesLinking,
        staticParkAndRide,
        staticBikeParkAndRide,
        banDiscouragedWalking,
        banDiscouragedBiking
      )
    );
  }
}
