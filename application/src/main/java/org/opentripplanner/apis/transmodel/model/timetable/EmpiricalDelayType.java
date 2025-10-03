package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.model.framework.TransmodelScalars;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.model.TripTimeOnDate;

public class EmpiricalDelayType {

  static final String NAME = "EmpiricalDelay";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  private EmpiricalDelayType() {}

  public static GraphQLObjectType create() {
    return GraphQLObjectType.newObject()
      .name(NAME)
      .description(
        """
        The empirical delay indicate how late a service journey is based on historic data.
        """
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("p50")
          .description(
            "The median/50% percentile. This value is in the middle of the distribution."
          )
          .type(TransmodelScalars.DURATION_SCALAR)
          .dataFetcher(e -> (empiricalDelay(e).p50()))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("p90")
          .description(
            "The 90% percentile. 90% of the values is better and 10% have is more delayed."
          )
          .type(TransmodelScalars.DURATION_SCALAR)
          .dataFetcher(e -> (empiricalDelay(e).p90()))
          .build()
      )
      .build();
  }

  @Nullable
  public static EmpiricalDelay dataFetcherForTripTimeOnDate(DataFetchingEnvironment environment) {
    TripTimeOnDate parent = environment.getSource();
    TransmodelRequestContext ctx = environment.getContext();
    var service = ctx.getEmpiricalDelayService();

    if (parent == null || service == null) {
      return null;
    }
    return service
      .findEmpiricalDelay(
        parent.getTrip().getId(),
        parent.getServiceDay(),
        parent.getStopPosition()
      )
      .orElse(null);
  }

  private static EmpiricalDelay empiricalDelay(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
