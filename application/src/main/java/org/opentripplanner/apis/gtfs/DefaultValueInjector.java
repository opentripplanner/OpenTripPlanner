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
    var builder = new DefaultMappingBuilder()
      .intReq("planConnection.first", defaultRouteRequest.numItineraries())
      .stringOpt("planConnection.searchWindow", defaultRouteRequest.searchWindow());
    {
      var bike = defaultRouteRequest.preferences().bike();
      builder
        .intReq("BicyclePreferencesInput.boardCost", bike.boardCost())
        .floatReq("BicyclePreferencesInput.reluctance", bike.reluctance())
        .floatReq("BicyclePreferencesInput.speed", bike.speed());
      {
        var bikeParking = bike.parking();
        builder.intReq(
          "BicycleParkingPreferencesInput.unpreferredCost",
          bikeParking.unpreferredVehicleParkingTagCost().toSeconds()
        );
      }
      {
        var bikeRental = bike.rental();
        builder
          .boolReq(
            "DestinationBicyclePolicyInput.allowKeeping",
            bikeRental.allowArrivingInRentedVehicleAtDestination()
          )
          .intReq(
            "DestinationBicyclePolicyInput.keepingCost",
            bikeRental.arrivingInRentalVehicleAtDestinationCost().toSeconds()
          );
      }
      {
        var bikeWalking = bike.walking();
        builder
          .intReq(
            "BicycleWalkPreferencesCostInput.mountDismountCost",
            bikeWalking.mountDismountCost().toSeconds()
          )
          .floatReq("BicycleWalkPreferencesCostInput.reluctance", bikeWalking.reluctance())
          .stringReq(
            "BicycleWalkPreferencesInput.mountDismountTime",
            bikeWalking.mountDismountTime()
          )
          .floatReq("BicycleWalkPreferencesInput.speed", bikeWalking.speed());
      }
    }
    {
      var car = defaultRouteRequest.preferences().car();
      builder.floatReq("CarPreferencesInput.reluctance", car.reluctance());
      {
        var parking = car.parking();
        builder.intReq(
          "CarParkingPreferencesInput.unpreferredCost",
          parking.unpreferredVehicleParkingTagCost().toSeconds()
        );
      }
    }
    {
      var scooter = defaultRouteRequest.preferences().scooter();
      builder
        .floatReq("ScooterPreferencesInput.reluctance", scooter.reluctance())
        .floatReq("ScooterPreferencesInput.speed", scooter.speed());
      {
        var rental = scooter.rental();
        builder
          .boolReq(
            "DestinationScooterPolicyInput.allowKeeping",
            rental.allowArrivingInRentedVehicleAtDestination()
          )
          .intReq(
            "DestinationScooterPolicyInput.keepingCost",
            rental.arrivingInRentalVehicleAtDestinationCost().toSeconds()
          );
      }
    }
    {
      var transit = defaultRouteRequest.preferences().transit();
      builder
        .stringReq("AlightPreferencesInput.slack", transit.alightSlack().defaultValue())
        .stringReq("BoardPreferencesInput.slack", transit.boardSlack().defaultValue())
        .boolReq(
          "TimetablePreferencesInput.excludeRealTimeUpdates",
          transit.ignoreRealtimeUpdates()
        )
        .boolReq(
          "TimetablePreferencesInput.includePlannedCancellations",
          transit.includePlannedCancellations()
        )
        .boolReq(
          "TimetablePreferencesInput.includeRealTimeCancellations",
          transit.includeRealtimeCancellations()
        );
    }
    {
      var transfer = defaultRouteRequest.preferences().transfer();
      builder
        .floatReq("BoardPreferencesInput.waitReluctance", transfer.waitReluctance())
        .intReq("TransferPreferencesInput.cost", transfer.cost())
        .intReq(
          "TransferPreferencesInput.maximumAdditionalTransfers",
          transfer.maxAdditionalTransfers()
        )
        // Max transfers are wrong in the internal model but fixed in the API mapping
        .intReq("TransferPreferencesInput.maximumTransfers", transfer.maxTransfers() - 1)
        .stringReq("TransferPreferencesInput.slack", transfer.slack());
    }
    {
      var walk = defaultRouteRequest.preferences().walk();
      builder
        .intReq("WalkPreferencesInput.boardCost", walk.boardCost())
        .floatReq("WalkPreferencesInput.reluctance", walk.reluctance())
        .floatReq("WalkPreferencesInput.safetyFactor", walk.safetyFactor())
        .floatReq("WalkPreferencesInput.speed", walk.speed());
    }
    {
      builder.boolReq("WheelchairPreferencesInput.enabled", defaultRouteRequest.wheelchair());
    }
    return builder.build();
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
