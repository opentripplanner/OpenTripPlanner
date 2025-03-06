package org.opentripplanner.apis.transmodel.model.timetable;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import java.util.function.Function;
import org.opentripplanner.apis.transmodel.model.EnumTypes;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;

public class InterchangeType {

  public static GraphQLObjectType create(
    GraphQLOutputType lineType,
    GraphQLOutputType serviceJourneyType
  ) {
    return GraphQLObjectType.newObject()
      .name("Interchange")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("staySeated")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> constraint(env).isStaySeated())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("guaranteed")
          .type(Scalars.GraphQLBoolean)
          .dataFetcher(env -> constraint(env).isGuaranteed())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("priority")
          .description(
            "The transfer priority is used to decide where a transfer should " +
            "happen, at the highest prioritized location. If the guaranteed " +
            "flag is set it take precedence priority. A guaranteed ALLOWED " +
            "transfer is preferred over a PREFERRED none-guaranteed transfer."
          )
          .type(EnumTypes.INTERCHANGE_PRIORITY)
          .dataFetcher(env -> constraint(env).getPriority())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("maximumWaitTime")
          .description(
            "Maximum time after scheduled departure time the connecting " +
            "transport is guaranteed to wait for the delayed trip. [NOT " +
            "RESPECTED DURING ROUTING, JUST PASSED THROUGH]"
          )
          .type(Scalars.GraphQLInt)
          .dataFetcher(env -> constraint(env).getMaxWaitTime())
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("FromLine")
          .deprecate("This is the same as using the `fromServiceJourney { line }` field.")
          .type(lineType)
          .dataFetcher(env -> transferRoute(env, ConstrainedTransfer::getFrom))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("ToLine")
          .deprecate("This is the same as using the `toServiceJourney { line }` field.")
          .type(lineType)
          .dataFetcher(env -> transferRoute(env, ConstrainedTransfer::getTo))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("fromServiceJourney")
          .type(serviceJourneyType)
          .dataFetcher(env -> transferTrip(env, ConstrainedTransfer::getFrom))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("toServiceJourney")
          .type(serviceJourneyType)
          .dataFetcher(env -> transferTrip(env, ConstrainedTransfer::getTo))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("FromServiceJourney")
          .type(serviceJourneyType)
          .deprecate("Use fromServiceJourney instead")
          .dataFetcher(env -> transferTrip(env, ConstrainedTransfer::getFrom))
          .build()
      )
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("ToServiceJourney")
          .type(serviceJourneyType)
          .deprecate("Use toServiceJourney instead")
          .dataFetcher(env -> transferTrip(env, ConstrainedTransfer::getTo))
          .build()
      )
      .build();
  }

  private static ConstrainedTransfer transfer(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  private static TransferPoint transferPoint(
    DataFetchingEnvironment environment,
    Function<ConstrainedTransfer, TransferPoint> fromTo
  ) {
    return fromTo.apply(transfer(environment));
  }

  private static Trip transferTrip(
    DataFetchingEnvironment environment,
    Function<ConstrainedTransfer, TransferPoint> fromTo
  ) {
    return TransferPoint.getTrip(transferPoint(environment, fromTo));
  }

  private static Route transferRoute(
    DataFetchingEnvironment environment,
    Function<ConstrainedTransfer, TransferPoint> fromTo
  ) {
    return TransferPoint.getRoute(transferPoint(environment, fromTo));
  }

  private static TransferConstraint constraint(DataFetchingEnvironment environment) {
    return transfer(environment).getTransferConstraint();
  }
}
