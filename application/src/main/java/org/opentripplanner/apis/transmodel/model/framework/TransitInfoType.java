package org.opentripplanner.apis.transmodel.model.framework;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import org.opentripplanner.apis.transmodel.model.siri.sx.ValidityPeriod;
import org.opentripplanner.apis.transmodel.support.GqlUtil;

public class TransitInfoType {

  public static GraphQLOutputType create(GraphQLObjectType validityPeriodType) {
    return GraphQLObjectType.newObject()
      .name("TransitInfo")
      .description("Information about the transit data available in the system.")
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name("validityPeriod")
          .description("The validity period for the transit data currently loaded in the system.")
          .type(validityPeriodType)
          .dataFetcher(environment -> {
            var transitService = GqlUtil.getTransitService(environment);
            var startTime = transitService.getTransitServiceStarts();
            var endTime = transitService.getTransitServiceEnds();

            Long startMillis = startTime != null ? startTime.toEpochMilli() : null;
            Long endMillis = endTime != null ? endTime.toEpochMilli() : null;

            return new ValidityPeriod(startMillis, endMillis);
          })
          .build()
      )
      .build();
  }
}
