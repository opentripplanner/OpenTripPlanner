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
import org.opentripplanner.routing.api.request.RouteRequest;

public class DefaultValueInjector extends GraphQLTypeVisitorStub implements GraphQLTypeVisitor {

  private final RouteRequest defaultRouteRequest;

  public DefaultValueInjector(RouteRequest defaultRouteRequest) {
    this.defaultRouteRequest = defaultRouteRequest;
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
    var key = parentName + "_" + name;
    var preferences = defaultRouteRequest.preferences();
    switch (key) {
      case "planConnection_first":
        return IntValue.of(defaultRouteRequest.numItineraries());
      case "planConnection_searchWindow":
        return defaultRouteRequest.searchWindow() != null
          ? StringValue.of(defaultRouteRequest.searchWindow().toString())
          : null;
      case "AlightPreferencesInput_slack":
        return StringValue.of(preferences.transit().alightSlack().defaultValue().toString());
      case "BicycleParkingPreferencesInput_unpreferredCost":
        return IntValue.of(
          preferences.bike().parking().unpreferredVehicleParkingTagCost().toSeconds()
        );
      case "BicyclePreferencesInput_boardCost":
        return IntValue.of(preferences.bike().boardCost());
      case "BicyclePreferencesInput_reluctance":
        return FloatValue.of(preferences.bike().reluctance());
      case "BicyclePreferencesInput_speed":
        return FloatValue.of(preferences.bike().speed());
      case "BicycleWalkPreferencesCostInput_mountDismountCost":
        return IntValue.of(preferences.bike().walking().mountDismountCost().toSeconds());
      case "BicycleWalkPreferencesCostInput_reluctance":
        return FloatValue.of(preferences.bike().walking().reluctance());
      case "BicycleWalkPreferencesInput_mountDismountTime":
        return StringValue.of(preferences.bike().walking().mountDismountTime().toString());
      case "BicycleWalkPreferencesInput_speed":
        return FloatValue.of(preferences.bike().walking().speed());
      case "BoardPreferencesInput_slack":
        return StringValue.of(preferences.transit().boardSlack().defaultValue().toString());
      case "BoardPreferencesInput_waitReluctance":
        return FloatValue.of(preferences.transfer().waitReluctance());
      case "CarParkingPreferencesInput_unpreferredCost":
        return IntValue.of(
          preferences.car().parking().unpreferredVehicleParkingTagCost().toSeconds()
        );
      case "CarPreferencesInput_reluctance":
        return FloatValue.of(preferences.car().reluctance());
      case "DestinationBicyclePolicyInput_allowKeeping":
        return BooleanValue.of(
          preferences.bike().rental().allowArrivingInRentedVehicleAtDestination()
        );
      case "DestinationBicyclePolicyInput_keepingCost":
        return IntValue.of(
          preferences.bike().rental().arrivingInRentalVehicleAtDestinationCost().toSeconds()
        );
      case "DestinationScooterPolicyInput_allowKeeping":
        return BooleanValue.of(
          preferences.scooter().rental().allowArrivingInRentedVehicleAtDestination()
        );
      case "DestinationScooterPolicyInput_keepingCost":
        return IntValue.of(
          preferences.scooter().rental().arrivingInRentalVehicleAtDestinationCost().toSeconds()
        );
      case "ScooterPreferencesInput_reluctance":
        return FloatValue.of(preferences.scooter().reluctance());
      case "ScooterPreferencesInput_speed":
        return FloatValue.of(preferences.scooter().speed());
      case "TimetablePreferencesInput_excludeRealTimeUpdates":
        return BooleanValue.of(preferences.transit().ignoreRealtimeUpdates());
      case "TimetablePreferencesInput_includePlannedCancellations":
        return BooleanValue.of(preferences.transit().includePlannedCancellations());
      case "TimetablePreferencesInput_includeRealTimeCancellations":
        return BooleanValue.of(preferences.transit().includeRealtimeCancellations());
      case "TransferPreferencesInput_cost":
        return IntValue.of(preferences.transfer().cost());
      case "TransferPreferencesInput_maximumAdditionalTransfers":
        return IntValue.of(preferences.transfer().maxAdditionalTransfers());
      case "TransferPreferencesInput_maximumTransfers":
        // Max transfers are wrong in the internal model but fixed in the API mapping
        return IntValue.of(preferences.transfer().maxTransfers() - 1);
      case "TransferPreferencesInput_slack":
        return StringValue.of(preferences.transfer().slack().toString());
      case "WalkPreferencesInput_boardCost":
        return IntValue.of(preferences.walk().boardCost());
      case "WalkPreferencesInput_reluctance":
        return FloatValue.of(preferences.walk().reluctance());
      case "WalkPreferencesInput_safetyFactor":
        return FloatValue.of(preferences.walk().safetyFactor());
      case "WalkPreferencesInput_speed":
        return FloatValue.of(preferences.walk().speed());
      case "WheelchairPreferencesInput_enabled":
        return BooleanValue.of(defaultRouteRequest.wheelchair());
      default:
        return null;
    }
  }
}
