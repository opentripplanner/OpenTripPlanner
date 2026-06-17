package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.RealTimeTripStateModel;

public class RealTimeTripStateImpl implements GraphQLDataFetchers.GraphQLRealTimeTripState {

  @Override
  public DataFetcher<Boolean> added() {
    return environment -> getSource(environment).added();
  }

  @Override
  public DataFetcher<Boolean> canceled() {
    return environment -> getSource(environment).canceled();
  }

  @Override
  public DataFetcher<Boolean> deleted() {
    return environment -> getSource(environment).deleted();
  }

  @Override
  public DataFetcher<Boolean> timesModified() {
    return environment -> getSource(environment).timesModified();
  }

  @Override
  public DataFetcher<Boolean> tripPatternModified() {
    return environment -> getSource(environment).tripPatternModified();
  }

  @Override
  public DataFetcher<Boolean> updated() {
    return environment -> getSource(environment).updated();
  }

  private RealTimeTripStateModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
