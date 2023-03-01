package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.core.IndexedLegProducts;

public class LegacyGraphQLLegProductsImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLLegProducts {

  @Override
  public DataFetcher<Iterable<Integer>> legIndices() {
    return env -> List.of(getSource(env).legIndex());
  }

  @Override
  public DataFetcher<Iterable<FareProduct>> products() {
    return env -> getSource(env).products();
  }

  private IndexedLegProducts getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
