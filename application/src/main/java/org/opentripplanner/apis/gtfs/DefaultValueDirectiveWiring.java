package org.opentripplanner.apis.gtfs;

import graphql.language.IntValue;
import graphql.language.Value;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import java.util.List;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

public class DefaultValueDirectiveWiring implements SchemaDirectiveWiring {

  private final RoutingPreferences defaultPreferences;

  public DefaultValueDirectiveWiring(RoutingPreferences defaultPreferences) {
    this.defaultPreferences = defaultPreferences;
  }

  @Override
  public GraphQLInputObjectField onInputObjectField(
    SchemaDirectiveWiringEnvironment<GraphQLInputObjectField> environment
  ) {
    GraphQLInputObjectField field = environment.getElement();
    if (field.hasAppliedDirective("defaultValue")) {
      String valueKey = environment
        .getAppliedDirective("defaultValue")
        .getArgument("valueKey")
        .getValue();
      return field.transform(builder ->
        builder
          .defaultValueLiteral(getDefaultValueForKey(valueKey))
          .replaceDirectives(List.of(field.getDirective("defaultValue")))
          .build()
      );
    }
    return field;
  }

  private Value getDefaultValueForKey(String key) {
    switch (key) {
      case "bicycleBoardCost":
        return IntValue.of(defaultPreferences.bike().boardCost());
      default:
        return null;
    }
  }
}
