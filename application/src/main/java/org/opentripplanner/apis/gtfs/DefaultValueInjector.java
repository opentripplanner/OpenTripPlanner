package org.opentripplanner.apis.gtfs;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
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
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.mapping.routerequest.AccessModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.DirectModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.EgressModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.StreetModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.TransferModeMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;

/**
 * GraphQL type visitor that injects default values to input fields and query arguments from code
 * and configuration.
 */
public class DefaultValueInjector extends GraphQLTypeVisitorStub implements GraphQLTypeVisitor {

  private final Map<String, Value<?>> defaultForKey;

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

  private Value<?> getDefaultValueForSchemaObject(
    TraverserContext<GraphQLSchemaElement> context,
    String name
  ) {
    // Arguments and input fields always have a parent
    var parent = (GraphQLNamedSchemaElement) context.getParentNode();
    var parentName = parent.getName();
    var key = parentName + "." + name;
    return defaultForKey.get(key);
  }

  private static Map<String, Value<?>> createDefaultMapping(RouteRequest defaultRouteRequest) {
    var builder = new DefaultMappingBuilder()
      .intReq("planConnection.first", defaultRouteRequest.numItineraries())
      .stringOpt("planConnection.searchWindow", defaultRouteRequest.searchWindow());
    setBikeDefaults(defaultRouteRequest.preferences().bike(), builder);
    setCarDefaults(defaultRouteRequest.preferences().car(), builder);
    setModeDefaults(defaultRouteRequest.journey(), builder);
    setScooterDefaults(defaultRouteRequest.preferences().scooter(), builder);
    setTransitDefaults(defaultRouteRequest.preferences().transit(), builder);
    setTransferDefaults(defaultRouteRequest.preferences().transfer(), builder);
    setWalkDefaults(defaultRouteRequest.preferences().walk(), builder);
    setWheelchairDefaults(defaultRouteRequest, builder);
    return builder.build();
  }

  private static void setBikeDefaults(BikePreferences bike, DefaultMappingBuilder builder) {
    builder
      .intReq("BicyclePreferencesInput.boardCost", bike.boardCost())
      .floatReq("BicyclePreferencesInput.reluctance", bike.reluctance())
      .floatReq("BicyclePreferencesInput.speed", bike.speed());
    setBikeParkingDefaults(bike.parking(), builder);
    setBikeRentalDefaults(bike.rental(), builder);
    setBikeWalkingDefaults(bike.walking(), builder);
  }

  private static void setBikeParkingDefaults(
    VehicleParkingPreferences parking,
    DefaultMappingBuilder builder
  ) {
    builder.intReq(
      "BicycleParkingPreferencesInput.unpreferredCost",
      parking.unpreferredVehicleParkingTagCost().toSeconds()
    );
  }

  private static void setBikeRentalDefaults(
    VehicleRentalPreferences rental,
    DefaultMappingBuilder builder
  ) {
    builder
      .boolReq(
        "DestinationBicyclePolicyInput.allowKeeping",
        rental.allowArrivingInRentedVehicleAtDestination()
      )
      .intReq(
        "DestinationBicyclePolicyInput.keepingCost",
        rental.arrivingInRentalVehicleAtDestinationCost().toSeconds()
      );
  }

  private static void setBikeWalkingDefaults(
    VehicleWalkingPreferences walking,
    DefaultMappingBuilder builder
  ) {
    builder
      .intReq(
        "BicycleWalkPreferencesCostInput.mountDismountCost",
        walking.mountDismountCost().toSeconds()
      )
      .floatReq("BicycleWalkPreferencesCostInput.reluctance", walking.reluctance())
      .stringReq("BicycleWalkPreferencesInput.mountDismountTime", walking.mountDismountTime())
      .floatReq("BicycleWalkPreferencesInput.speed", walking.speed());
  }

  private static void setCarDefaults(CarPreferences car, DefaultMappingBuilder builder) {
    builder.floatReq("CarPreferencesInput.reluctance", car.reluctance());
    setCarParkingDefaults(car.parking(), builder);
  }

  private static void setCarParkingDefaults(
    VehicleParkingPreferences parking,
    DefaultMappingBuilder builder
  ) {
    builder.intReq(
      "CarParkingPreferencesInput.unpreferredCost",
      parking.unpreferredVehicleParkingTagCost().toSeconds()
    );
  }

  private static void setModeDefaults(JourneyRequest journey, DefaultMappingBuilder builder) {
    builder.enumListReq(
      "PlanModesInput.direct",
      StreetModeMapper
        .getStreetModesForApi(journey.direct().mode())
        .stream()
        .map(mode -> (Enum) DirectModeMapper.map(mode))
        .toList()
    );
    builder.enumListReq(
      "PlanTransitModesInput.access",
      StreetModeMapper
        .getStreetModesForApi(journey.access().mode())
        .stream()
        .map(mode -> (Enum) AccessModeMapper.map(mode))
        .toList()
    );
    builder.enumListReq(
      "PlanTransitModesInput.egress",
      StreetModeMapper
        .getStreetModesForApi(journey.egress().mode())
        .stream()
        .map(mode -> (Enum) EgressModeMapper.map(mode))
        .toList()
    );
    builder.enumListReq(
      "PlanTransitModesInput.transfer",
      StreetModeMapper
        .getStreetModesForApi(journey.transfer().mode())
        .stream()
        .map(mode -> (Enum) TransferModeMapper.map(mode))
        .toList()
    );
  }

  private static void setScooterDefaults(
    ScooterPreferences scooter,
    DefaultMappingBuilder builder
  ) {
    builder
      .floatReq("ScooterPreferencesInput.reluctance", scooter.reluctance())
      .floatReq("ScooterPreferencesInput.speed", scooter.speed());
    setScooterRentalDefaults(scooter.rental(), builder);
  }

  private static void setScooterRentalDefaults(
    VehicleRentalPreferences rental,
    DefaultMappingBuilder builder
  ) {
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

  private static void setTransitDefaults(
    TransitPreferences transit,
    DefaultMappingBuilder builder
  ) {
    builder
      .stringReq("AlightPreferencesInput.slack", transit.alightSlack().defaultValue())
      .stringReq("BoardPreferencesInput.slack", transit.boardSlack().defaultValue())
      .boolReq("TimetablePreferencesInput.excludeRealTimeUpdates", transit.ignoreRealtimeUpdates())
      .boolReq(
        "TimetablePreferencesInput.includePlannedCancellations",
        transit.includePlannedCancellations()
      )
      .boolReq(
        "TimetablePreferencesInput.includeRealTimeCancellations",
        transit.includeRealtimeCancellations()
      );
  }

  private static void setTransferDefaults(
    TransferPreferences transfer,
    DefaultMappingBuilder builder
  ) {
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

  private static void setWalkDefaults(WalkPreferences walk, DefaultMappingBuilder builder) {
    builder
      .intReq("WalkPreferencesInput.boardCost", walk.boardCost())
      .floatReq("WalkPreferencesInput.reluctance", walk.reluctance())
      .floatReq("WalkPreferencesInput.safetyFactor", walk.safetyFactor())
      .floatReq("WalkPreferencesInput.speed", walk.speed());
  }

  private static void setWheelchairDefaults(
    RouteRequest defaultRouteRequest,
    DefaultMappingBuilder builder
  ) {
    builder.boolReq("WheelchairPreferencesInput.enabled", defaultRouteRequest.wheelchair());
  }

  private static class DefaultMappingBuilder {

    private final Map<String, Value<?>> defaultValueForKey = new HashMap<>();

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

    public DefaultMappingBuilder enumListReq(String key, List<Enum> valueList) {
      defaultValueForKey.put(
        key,
        ArrayValue
          .newArrayValue()
          .values((valueList.stream().map(value -> (Value) new EnumValue(value.name())).toList()))
          .build()
      );
      return this;
    }

    public Map<String, Value<?>> build() {
      return defaultValueForKey;
    }
  }
}
