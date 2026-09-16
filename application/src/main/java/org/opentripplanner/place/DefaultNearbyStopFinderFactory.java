package org.opentripplanner.place;

import jakarta.inject.Inject;
import java.util.Collection;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.transit.service.TransitService;

public class DefaultNearbyStopFinderFactory implements NearbyStopFinderFactory {

  private final Graph graph;
  private final TransitService transitService;
  private final LinkingContextFactory linkingContextFactory;

  //Constructor used by dagger injection
  @Inject
  public DefaultNearbyStopFinderFactory(
    Graph graph,
    TransitService transitService,
    LinkingContextFactory linkingContextFactory
  ) {
    this.graph = graph;
    this.transitService = transitService;
    this.linkingContextFactory = linkingContextFactory;
  }

  @Override
  public NearbyStopFinder create() {
    if (!graph.hasStreets) {
      return new StraightLineNearbyStopFinder(transitService::findRegularStopsByBoundingBox);
    } else {
      return StreetNearbyStopFinder.of(linkingContextFactory).build();
    }
  }

  @Override
  public StreetNearbyStopFinder createWithExtensionRequestContexts(Collection<ExtensionRequestContext> extensionRequestContexts)
  {
    return StreetNearbyStopFinder.of(linkingContextFactory)
        .withExtensionRequestContexts(extensionRequestContexts)
        .build();
  }

  @Override
  public StreetNearbyStopFinder createWithLinkingContextFactory(LinkingContextFactory linkingContextFactory)
  {
    return StreetNearbyStopFinder.of(linkingContextFactory)
      .build();
  }
}
