package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.IndexedLegProducts;

public class LegacyGraphQLFareProductsImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProducts {

  @Override
  public DataFetcher<Iterable<FareProduct>> itinerary() {
    return env -> getSource(env).getFares().getItineraryProducts();
  }

  @Override
  public DataFetcher<Iterable<IndexedLegProducts>> legs() {
    return env -> {
      var itinerary = getSource(env);
      return itinerary.getFares().indexedLegProducts(itinerary);
    };
  }

  private Itinerary getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
