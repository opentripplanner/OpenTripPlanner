package org.opentripplanner.ext.carpooling.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolStopIndex;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.routing.CorridorBuilder;
import org.opentripplanner.ext.carpooling.service.DefaultCarpoolingService;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;

@Module
public class CarpoolingModule {

  @Provides
  @Singleton
  @Nullable
  public CarpoolingRepository provideCarpoolingRepository() {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingRepository();
  }

  @Provides
  @Singleton
  @Nullable
  public static CarReachableVertexSnapper provideCarReachableVertexSnapper() {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return CarReachableVertexSnapper.createDefault();
  }

  @Provides
  @Singleton
  @Nullable
  public static CorridorBuilder provideCorridorBuilder(
    @Nullable CarpoolStopIndex stopIndex,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new CorridorBuilder(stopIndex, streetLimitationParametersService);
  }

  @Provides
  @Singleton
  @Nullable
  public static CarpoolTripVertexResolver provideCarpoolTripVertexResolver(
    VertexCreationService vertexCreationService,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper,
    @Nullable CorridorBuilder corridorBuilder
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new CarpoolTripVertexResolver(
      vertexCreationService,
      carReachableVertexSnapper,
      corridorBuilder
    );
  }

  @Provides
  @Singleton
  @Nullable
  public static CarpoolStopIndex provideCarpoolStopIndex(
    Graph graph,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new CarpoolStopIndex(graph, carReachableVertexSnapper);
  }

  @Provides
  @Singleton
  @Nullable
  public static CarpoolingService provideCarpoolingService(
    @Nullable CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    VertexCreationService vertexCreationService,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingService(
      repository,
      streetLimitationParametersService,
      vertexCreationService,
      carReachableVertexSnapper
    );
  }
}
