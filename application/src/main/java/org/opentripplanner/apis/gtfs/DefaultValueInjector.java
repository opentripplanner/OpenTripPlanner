package org.opentripplanner.apis.gtfs;

import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * GraphQL type visitor that injects default values to input fields and query arguments from code
 * and configuration.
 */
public class DefaultValueInjector extends GraphQLTypeVisitorStub implements GraphQLTypeVisitor {

  private final Map<String, Value> defaultForKey;

  public DefaultValueInjector(RouteRequest defaultRouteRequest) {
    this.defaultForKey = createDefaultMapping(defaultRouteRequest);
  }

  @Override
  public TraversalControl visitGraphQLArgument(
    GraphQLArgument argument,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    var defaultValue = getDefaultValueForSchemaObject(context, argument.getName());
    if (defaultValue != null) {
      return changeNode(
        context,
        argument.transform(builder -> builder.defaultValueLiteral(defaultValue).build())
      );
    }
    return TraversalControl.CONTINUE;
  }

  @Override
  public TraversalControl visitGraphQLInputObjectField(
    GraphQLInputObjectField field,
    TraverserContext<GraphQLSchemaElement> context
  ) {
    var defaultValue = getDefaultValueForSchemaObject(context, field.getName());
    if (defaultValue != null) {
      return changeNode(
        context,
        field.transform(builder -> builder.defaultValueLiteral(defaultValue).build())
      );
    }
    return TraversalControl.CONTINUE;
  }

  private Value getDefaultValueForSchemaObject(
    TraverserContext<GraphQLSchemaElement> context,
    String name
  ) {
    // Arguments and input fields always have a parent
    var parent = (GraphQLNamedSchemaElement) context.getParentNode();
    var parentName = parent.getName();
    var key = parentName + "." + name;
    return defaultForKey.get(key);
  }

  private static Map<String, Value> createDefaultMapping(RouteRequest defaultRouteRequest) {
    var builder = new DefaultMappingBuilder();
    var preferences = defaultRouteRequest.preferences();
    return builder
      .intReq("planConnection.first", defaultRouteRequest.numItineraries())
      .stringOpt("planConnection.searchWindow", defaultRouteRequest.searchWindow())
      .stringReq("AlightPreferencesInput.slack", preferences.transit().alightSlack().defaultValue())
      .intReq(
        "BicycleParkingPreferencesInput.unpreferredCost",
        preferences.bike().parking().unpreferredVehicleParkingTagCost().toSeconds()
      )
      .intReq("BicyclePreferencesInput.boardCost", preferences.bike().boardCost())
      .floatReq("BicyclePreferencesInput.reluctance", preferences.bike().reluctance())
      .floatReq("BicyclePreferencesInput.speed", preferences.bike().speed())
      .intReq(
        "BicycleWalkPreferencesCostInput.mountDismountCost",
        preferences.bike().walking().mountDismountCost().toSeconds()
      )
      .floatReq(
        "BicycleWalkPreferencesCostInput.reluctance",
        preferences.bike().walking().reluctance()
      )
      .stringReq(
        "BicycleWalkPreferencesInput.mountDismountTime",
        preferences.bike().walking().mountDismountTime()
      )
      .floatReq("BicycleWalkPreferencesInput.speed", preferences.bike().walking().speed())
      .stringReq("BoardPreferencesInput.slack", preferences.transit().boardSlack().defaultValue())
      .floatReq("BoardPreferencesInput.waitReluctance", preferences.transfer().waitReluctance())
      .intReq(
        "CarParkingPreferencesInput.unpreferredCost",
        preferences.car().parking().unpreferredVehicleParkingTagCost().toSeconds()
      )
      .floatReq("CarPreferencesInput.reluctance", preferences.car().reluctance())
      .boolReq(
        "DestinationBicyclePolicyInput.allowKeeping",
        preferences.bike().rental().allowArrivingInRentedVehicleAtDestination()
      )
      .intReq(
        "DestinationBicyclePolicyInput.keepingCost",
        preferences.bike().rental().arrivingInRentalVehicleAtDestinationCost().toSeconds()
      )
      .boolReq(
        "DestinationScooterPolicyInput.allowKeeping",
        preferences.scooter().rental().allowArrivingInRentedVehicleAtDestination()
      )
      .intReq(
        "DestinationScooterPolicyInput.keepingCost",
        preferences.scooter().rental().arrivingInRentalVehicleAtDestinationCost().toSeconds()
      )
      .floatReq("ScooterPreferencesInput.reluctance", preferences.scooter().reluctance())
      .floatReq("ScooterPreferencesInput.speed", preferences.scooter().speed())
      .boolReq(
        "TimetablePreferencesInput.excludeRealTimeUpdates",
        preferences.transit().ignoreRealtimeUpdates()
      )
      .boolReq(
        "TimetablePreferencesInput.includePlannedCancellations",
        preferences.transit().includePlannedCancellations()
      )
      .boolReq(
        "TimetablePreferencesInput.includeRealTimeCancellations",
        preferences.transit().includeRealtimeCancellations()
      )
      .intReq("TransferPreferencesInput.cost", preferences.transfer().cost())
      .intReq(
        "TransferPreferencesInput.maximumAdditionalTransfers",
        preferences.transfer().maxAdditionalTransfers()
      )
      // Max transfers are wrong in the internal model but fixed in the API mapping
      .intReq(
        "TransferPreferencesInput.maximumTransfers",
        preferences.transfer().maxTransfers() - 1
      )
      .stringReq("TransferPreferencesInput.slack", preferences.transfer().slack())
      .intReq("WalkPreferencesInput.boardCost", preferences.walk().boardCost())
      .floatReq("WalkPreferencesInput.reluctance", preferences.walk().reluctance())
      .floatReq("WalkPreferencesInput.safetyFactor", preferences.walk().safetyFactor())
      .floatReq("WalkPreferencesInput.speed", preferences.walk().speed())
      .boolReq("WheelchairPreferencesInput.enabled", defaultRouteRequest.wheelchair())
      .build();
  }

  private static class DefaultMappingBuilder {

    private final Map<String, Value> defaultValueForKey = new HashMap<String, Value>();

    public DefaultMappingBuilder intReq(String key, int value) {
      defaultValueForKey.put(key, IntValue.of(value));
      return this;
    }

    public DefaultMappingBuilder floatReq(String key, double value) {
      defaultValueForKey.put(key, FloatValue.of(value));
      return this;
    }

    public DefaultMappingBuilder stringReq(String key, Object value) {
      defaultValueForKey.put(key, StringValue.of(value.toString()));
      return this;
    }

    public DefaultMappingBuilder stringOpt(String key, @Nullable Object value) {
      if (value != null) {
        defaultValueForKey.put(key, StringValue.of(value.toString()));
      }
      return this;
    }

    public DefaultMappingBuilder boolReq(String key, boolean value) {
      defaultValueForKey.put(key, BooleanValue.of(value));
      return this;
    }

    public Map<String, Value> build() {
      return defaultValueForKey;
    }
  }
}
