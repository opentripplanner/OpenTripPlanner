package org.opentripplanner.ext.transmodelapi.model.framework;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;

public class ValidityPeriodType {

  public static GraphQLObjectType create(GqlUtil qglUtil) {
  return GraphQLObjectType.newObject()
          .name("ValidityPeriod")
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("startTime")
                  .type(qglUtil.dateTimeScalar)
                  .description("Start of validity period")
                  .dataFetcher(environment -> {
                      Pair<Long, Long> period = environment.getSource();
                      return period != null ? period.getLeft() : null;
                  })
                  .build())
          .field(GraphQLFieldDefinition.newFieldDefinition()
                  .name("endTime")
                  .type(qglUtil.dateTimeScalar)
                  .description("End of validity period. Will return 'null' if validity is open-ended.")
                  .dataFetcher(environment -> {
                      Pair<Long, Long> period = environment.getSource();
                      return period != null ? period.getRight() : null;
                  })
                  .build())
          .build();
}
}
