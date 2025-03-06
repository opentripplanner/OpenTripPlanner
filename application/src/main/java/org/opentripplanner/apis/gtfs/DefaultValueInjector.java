package org.opentripplanner.apis.gtfs;

import graphql.language.ArrayValue;
import graphql.language.BooleanValue;
import graphql.language.EnumValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.AccessModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.DirectModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.EgressModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.StreetModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.TransferModeMapper;
import org.opentripplanner.apis.gtfs.mapping.routerequest.VehicleOptimizationTypeMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.ScooterPreferences;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleParkingPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleWalkingPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingFilter;
import org.opentripplanner.routing.api.request.preference.filter.VehicleParkingSelect;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.transit.model.basic.TransitMode;

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
    setModeDefaults(
      defaultRouteRequest.journey(),
      defaultRouteRequest.preferences().transit(),
      builder
    );
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
      .floatReq("BicyclePreferencesInput.speed", bike.speed())
      .objectReq(
        "BicyclePreferencesInput.optimization",
        mapVehicleOptimize(
          bike.optimizeType(),
          bike.optimizeTriangle(),
          VehicleOptimizationTypeMapper::mapForBicycle
        )
      );
    setBikeParkingDefaults(bike.parking(), builder);
    setBikeRentalDefaults(bike.rental(), builder);
    setBikeWalkingDefaults(bike.walking(), builder);
  }

  private static void setBikeParkingDefaults(
    VehicleParkingPreferences parking,
    DefaultMappingBuilder builder
  ) {
    builder
      .intReq(
        "BicycleParkingPreferencesInput.unpreferredCost",
        parking.unpreferredVehicleParkingTagCost().toSeconds()
      )
      .arrayReq("BicycleParkingPreferencesInput.filters", mapVehicleParkingFilter(parking.filter()))
      .arrayReq(
        "BicycleParkingPreferencesInput.preferred",
        mapVehicleParkingFilter(parking.preferred())
      );
  }

  private static void setBikeRentalDefaults(
    VehicleRentalPreferences rental,
    DefaultMappingBuilder builder
  ) {
    builder
      .arrayStringsOpt(
        "BicycleRentalPreferencesInput.allowedNetworks",
        rental.allowedNetworks().isEmpty() ? null : rental.allowedNetworks()
      )
      .arrayStringsReq("BicycleRentalPreferencesInput.bannedNetworks", rental.bannedNetworks())
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
    builder
      .floatReq("CarPreferencesInput.reluctance", car.reluctance())
      .intReq("CarPreferencesInput.boardCost", car.boardCost());
    setCarParkingDefaults(car.parking(), builder);
    setCarRentalDefaults(car.rental(), builder);
  }

  private static void setCarParkingDefaults(
    VehicleParkingPreferences parking,
    DefaultMappingBuilder builder
  ) {
    builder
      .intReq(
        "CarParkingPreferencesInput.unpreferredCost",
        parking.unpreferredVehicleParkingTagCost().toSeconds()
      )
      .arrayReq("CarParkingPreferencesInput.filters", mapVehicleParkingFilter(parking.filter()))
      .arrayReq(
        "CarParkingPreferencesInput.preferred",
        mapVehicleParkingFilter(parking.preferred())
      );
  }

  private static void setCarRentalDefaults(
    VehicleRentalPreferences rental,
    DefaultMappingBuilder builder
  ) {
    builder
      .arrayStringsOpt(
        "CarRentalPreferencesInput.allowedNetworks",
        rental.allowedNetworks().isEmpty() ? null : rental.allowedNetworks()
      )
      .arrayStringsReq("CarRentalPreferencesInput.bannedNetworks", rental.bannedNetworks());
  }

  private static void setModeDefaults(
    JourneyRequest journey,
    TransitPreferences transit,
    DefaultMappingBuilder builder
  ) {
    builder
      .enumListReq(
        "PlanModesInput.direct",
        StreetModeMapper.getStreetModesForApi(journey.direct().mode())
          .stream()
          .map(mode -> (Enum) DirectModeMapper.map(mode))
          .toList()
      )
      .enumListReq(
        "PlanTransitModesInput.access",
        StreetModeMapper.getStreetModesForApi(journey.access().mode())
          .stream()
          .map(mode -> (Enum) AccessModeMapper.map(mode))
          .toList()
      )
      .enumListReq(
        "PlanTransitModesInput.egress",
        StreetModeMapper.getStreetModesForApi(journey.egress().mode())
          .stream()
          .map(mode -> (Enum) EgressModeMapper.map(mode))
          .toList()
      )
      .enumListReq(
        "PlanTransitModesInput.transfer",
        StreetModeMapper.getStreetModesForApi(journey.transfer().mode())
          .stream()
          .map(mode -> (Enum) TransferModeMapper.map(mode))
          .toList()
      )
      .arrayReq("PlanTransitModesInput.transit", mapTransitModes(transit.reluctanceForMode()));
  }

  private static void setScooterDefaults(
    ScooterPreferences scooter,
    DefaultMappingBuilder builder
  ) {
    builder
      .floatReq("ScooterPreferencesInput.reluctance", scooter.reluctance())
      .floatReq("ScooterPreferencesInput.speed", scooter.speed())
      .objectReq(
        "ScooterPreferencesInput.optimization",
        mapVehicleOptimize(
          scooter.optimizeType(),
          scooter.optimizeTriangle(),
          VehicleOptimizationTypeMapper::mapForScooter
        )
      );
    setScooterRentalDefaults(scooter.rental(), builder);
  }

  private static void setScooterRentalDefaults(
    VehicleRentalPreferences rental,
    DefaultMappingBuilder builder
  ) {
    builder
      .arrayStringsOpt(
        "ScooterRentalPreferencesInput.allowedNetworks",
        rental.allowedNetworks().isEmpty() ? null : rental.allowedNetworks()
      )
      .arrayStringsReq("ScooterRentalPreferencesInput.bannedNetworks", rental.bannedNetworks())
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

  private static ArrayValue mapTransitModes(Map<TransitMode, Double> reluctanceForMode) {
    var modesWithReluctance = Arrays.stream(GraphQLTypes.GraphQLTransitMode.values())
      .map(mode -> mapTransitMode(mode, reluctanceForMode.get(TransitModeMapper.map(mode))))
      .toList();
    return ArrayValue.newArrayValue().values(modesWithReluctance).build();
  }

  private static Value mapTransitMode(
    GraphQLTypes.GraphQLTransitMode mode,
    @Nullable Double reluctance
  ) {
    var objectBuilder = ObjectValue.newObjectValue()
      .objectField(
        ObjectField.newObjectField().name("mode").value(EnumValue.of(mode.name())).build()
      );
    if (reluctance != null) {
      objectBuilder.objectField(
        ObjectField.newObjectField()
          .name("cost")
          .value(
            ObjectValue.newObjectValue()
              .objectField(
                ObjectField.newObjectField()
                  .name("reluctance")
                  .value(FloatValue.of(reluctance))
                  .build()
              )
              .build()
          )
          .build()
      );
    }
    return objectBuilder.build();
  }

  private static ObjectValue mapVehicleOptimize(
    VehicleRoutingOptimizeType type,
    TimeSlopeSafetyTriangle triangle,
    Function<VehicleRoutingOptimizeType, Enum> typeMapper
  ) {
    var optimizationField = type == VehicleRoutingOptimizeType.TRIANGLE
      ? ObjectField.newObjectField()
        .name("triangle")
        .value(
          ObjectValue.newObjectValue()
            .objectField(
              ObjectField.newObjectField()
                .name("flatness")
                .value(FloatValue.of(triangle.slope()))
                .build()
            )
            .objectField(
              ObjectField.newObjectField()
                .name("safety")
                .value(FloatValue.of(triangle.safety()))
                .build()
            )
            .objectField(
              ObjectField.newObjectField()
                .name("time")
                .value(FloatValue.of(triangle.time()))
                .build()
            )
            .build()
        )
        .build()
      : ObjectField.newObjectField()
        .name("type")
        .value(EnumValue.of(typeMapper.apply(type).name()))
        .build();
    return ObjectValue.newObjectValue().objectField(optimizationField).build();
  }

  private static ArrayValue mapVehicleParkingFilter(VehicleParkingFilter filter) {
    var arrayBuilder = ArrayValue.newArrayValue();
    if (!filter.not().isEmpty() || !filter.select().isEmpty()) {
      arrayBuilder.value(
        ObjectValue.newObjectValue()
          .objectField(mapVehicleParkingSelects("not", filter.not()))
          .objectField(mapVehicleParkingSelects("select", filter.select()))
          .build()
      );
    }
    return arrayBuilder.build();
  }

  private static ObjectField mapVehicleParkingSelects(
    String fieldName,
    List<VehicleParkingSelect> selectList
  ) {
    var selects = selectList
      .stream()
      .map(select ->
        (Value) ObjectValue.newObjectValue()
          .objectField(
            ObjectField.newObjectField()
              .name("tags")
              .value(
                ArrayValue.newArrayValue()
                  .values(select.tags().stream().map(tag -> (Value) StringValue.of(tag)).toList())
                  .build()
              )
              .build()
          )
          .build()
      )
      .toList();
    return ObjectField.newObjectField()
      .name(fieldName)
      .value(ArrayValue.newArrayValue().values(selects).build())
      .build();
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
        ArrayValue.newArrayValue()
          .values((valueList.stream().map(value -> (Value) new EnumValue(value.name())).toList()))
          .build()
      );
      return this;
    }

    public DefaultMappingBuilder objectReq(String key, ObjectValue value) {
      defaultValueForKey.put(key, value);
      return this;
    }

    public DefaultMappingBuilder arrayReq(String key, ArrayValue value) {
      defaultValueForKey.put(key, value);
      return this;
    }

    public DefaultMappingBuilder arrayStringsReq(String key, Collection<String> values) {
      defaultValueForKey.put(
        key,
        ArrayValue.newArrayValue()
          .values(values.stream().map(value -> (Value) StringValue.of(value)).toList())
          .build()
      );
      return this;
    }

    public DefaultMappingBuilder arrayStringsOpt(String key, @Nullable Collection<String> values) {
      if (values != null) {
        defaultValueForKey.put(
          key,
          ArrayValue.newArrayValue()
            .values(values.stream().map(value -> (Value) StringValue.of(value)).toList())
            .build()
        );
      }
      return this;
    }

    public Map<String, Value<?>> build() {
      return defaultValueForKey;
    }
  }
}
