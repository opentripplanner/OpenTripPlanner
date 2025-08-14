package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLDependentFareProductDependenciesArgs;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;

public class DependentFareProductImpl implements GraphQLDataFetchers.GraphQLDependentFareProduct {

  @Override
  public DataFetcher<Iterable<FareOffer>> dependencies() {
    return env -> {
      var fpl = getSource(env);
      var filter = new GraphQLDependentFareProductDependenciesArgs(
        env.getArguments()
      ).getGraphQLFilter();
      return switch (filter) {
        case null -> fpl.dependencies();
        case ALL -> fpl.dependencies();
        case MATCH_CATEGORY_AND_MEDIUM -> fpl.dependenciesMatchingCategoryAndMedium();
      };
    };
  }

  @Override
  public DataFetcher<String> id() {
    return env -> getSource(env).fareProduct().id().toString();
  }

  @Override
  public DataFetcher<FareMedium> medium() {
    return env -> getSource(env).fareProduct().medium();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> getSource(env).fareProduct().name();
  }

  @Override
  public DataFetcher<Money> price() {
    return env -> getSource(env).fareProduct().price();
  }

  @Override
  public DataFetcher<RiderCategory> riderCategory() {
    return env -> getSource(env).fareProduct().category();
  }

  private FareOffer.DependentFareOffer getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
