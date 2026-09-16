package org.opentripplanner.place;

import java.util.Collection;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;

public interface NearbyStopFinderFactory {
  NearbyStopFinder create();

  StreetNearbyStopFinder createWithExtensionRequestContexts(
    Collection<ExtensionRequestContext> extensionRequestContexts);

  StreetNearbyStopFinder createWithLinkingContextFactory(
    LinkingContextFactory linkingContextFactory);

}
