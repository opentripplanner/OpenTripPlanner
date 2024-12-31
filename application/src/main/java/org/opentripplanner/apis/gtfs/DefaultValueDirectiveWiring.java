package org.opentripplanner.apis.gtfs;

import graphql.language.IntValue;
import graphql.language.Value;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.opentripplanner.routing.api.request.RouteRequest;

public class DefaultValueDirectiveWiring implements SchemaDirectiveWiring {

  private final RouteRequest defaultRouteRequest;

  public DefaultValueDirectiveWiring(RouteRequest defaultRouteRequest) {
    this.defaultRouteRequest = defaultRouteRequest;
  }

  @Override
  public GraphQLInputObjectField onInputObjectField(
    SchemaDirectiveWiringEnvironment<GraphQLInputObjectField> environment
  ) {
    GraphQLInputObjectField field = environment.getElement();
    // Input object fields always have a parent
    var parentName = environment.getNodeParentTree().getParentInfo().get().getNode().getName();
    var key = parentName + "_" + field.getName();
    var defaultValue = getDefaultValueForKey(key);
    if (defaultValue != null) {
      return field.transform(builder -> builder.defaultValueLiteral(defaultValue).build());
    }
    return field;
  }

  private Value getDefaultValueForKey(String key) {
    var preferences = defaultRouteRequest.preferences();
    switch (key) {
      case "BicyclePreferencesInput_boardCost":
        return IntValue.of(preferences.bike().boardCost());
      default:
        return null;
    }
  }
}
