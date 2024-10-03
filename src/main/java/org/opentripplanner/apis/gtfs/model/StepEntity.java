package org.opentripplanner.apis.gtfs.model;

import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;

public interface StepEntity extends GraphQLDataFetchers.GraphQLStepEntity {
  record Entrance(String code, String name, String gtfsId) implements StepEntity {
    public static Entrance withCode(String code) {
      return new Entrance(code, null, null);
    }
  }

  @Override
  default GraphQLObjectType getType(TypeResolutionEnvironment env) {
    var schema = env.getSchema();
    Object o = env.getObject();
    if (o instanceof Entrance) {
      return schema.getObjectType("Entrance");
    }
    return null;
  }
}
