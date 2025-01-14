package org.opentripplanner.apis.transmodel.support;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Provide some of the commonly used "chain" of methods. Like all ids should be created the same
 * way.
 */
public class GqlUtil {

  /** private constructor, prevent instantiation of utility class */
  private GqlUtil() {}

  public static TransitService getTransitService(DataFetchingEnvironment environment) {
    return ((TransmodelRequestContext) environment.getContext()).getTransitService();
  }

  public static VehicleRentalService getVehicleRentalService(DataFetchingEnvironment environment) {
    return ((TransmodelRequestContext) environment.getContext()).getServerContext()
      .vehicleRentalService();
  }

  public static VehicleParkingService getVehicleParkingService(
    DataFetchingEnvironment environment
  ) {
    return ((TransmodelRequestContext) environment.getContext()).getServerContext()
      .vehicleParkingService();
  }

  public static GraphFinder getGraphFinder(DataFetchingEnvironment environment) {
    return ((TransmodelRequestContext) environment.getContext()).getServerContext().graphFinder();
  }

  public static GraphQLFieldDefinition newTransitIdField() {
    return GraphQLFieldDefinition
      .newFieldDefinition()
      .name("id")
      .type(new GraphQLNonNull(Scalars.GraphQLID))
      .dataFetcher(env -> TransitIdMapper.mapEntityIDToApi(env.getSource()))
      .build();
  }

  public static GraphQLInputObjectField newIdListInputField(String name, String description) {
    return GraphQLInputObjectField
      .newInputObjectField()
      .name(name)
      .description(description)
      .type(new GraphQLList(Scalars.GraphQLID))
      .defaultValue(List.of())
      .build();
  }

  public static boolean hasArgument(DataFetchingEnvironment environment, String name) {
    return environment.containsArgument(name) && environment.getArgument(name) != null;
  }

  /**
   * Return the integer value of the argument or throw an exception if the value is null
   * or strictly negative.
   * This should generally be handled at the GraphQL schema level,
   * but must sometimes be implemented programmatically to preserve backward compatibility.
   */
  public static int getPositiveNonNullIntegerArgument(
    DataFetchingEnvironment environment,
    String argumentName
  ) {
    Integer argumentValue = environment.getArgument(argumentName);
    if (argumentValue == null || argumentValue < 0) {
      throw new IllegalArgumentException(
        "The argument '" + argumentName + "' should be a non-null positive value: " + argumentValue
      );
    }
    return argumentValue;
  }

  /**
   * Helper method to support the deprecated 'lang' argument.
   */
  public static Locale getLocale(DataFetchingEnvironment environment) {
    String lang = environment.getArgument("lang");
    return lang != null
      ? GraphQLUtils.getLocale(environment, lang)
      : GraphQLUtils.getLocale(environment);
  }

  /**
   * Null-safe handling of a collection of type T. Returns an empty list if the collection is null.
   * Null elements are filtered out.
   */
  public static <T> List<T> toListNullSafe(@Nullable Collection<T> args) {
    if (args == null) {
      return List.of();
    }
    return args.stream().filter(Objects::nonNull).toList();
  }
}
