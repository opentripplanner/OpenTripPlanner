package org.opentripplanner.apis.gtfs;

import graphql.language.IntValue;
import graphql.language.Value;
import graphql.schema.GraphQLArgument;
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
  public GraphQLArgument onArgument(SchemaDirectiveWiringEnvironment<GraphQLArgument> environment) {
    GraphQLArgument argument = environment.getElement();
    var defaultValue = getDefaultValueForSchemaObject(environment);
    if (defaultValue != null) {
      return argument.transform(builder -> builder.defaultValueLiteral(defaultValue).build());
    }
    return argument;
  }

  @Override
  public GraphQLInputObjectField onInputObjectField(
    SchemaDirectiveWiringEnvironment<GraphQLInputObjectField> environment
  ) {
    GraphQLInputObjectField field = environment.getElement();
    var defaultValue = getDefaultValueForSchemaObject(environment);
    if (defaultValue != null) {
      return field.transform(builder -> builder.defaultValueLiteral(defaultValue).build());
    }
    return field;
  }

  private Value getDefaultValueForSchemaObject(SchemaDirectiveWiringEnvironment<?> environment) {
    // Arguments and input fields always have a parent
    var parentName = environment.getNodeParentTree().getParentInfo().get().getNode().getName();
    var key = parentName + "_" + environment.getElement().getName();
    var preferences = defaultRouteRequest.preferences();
    switch (key) {
      case "planConnection_first":
        return IntValue.of(defaultRouteRequest.numItineraries());
      case "BicyclePreferencesInput_boardCost":
        return IntValue.of(preferences.bike().boardCost());
      default:
        return null;
    }
  }
}
