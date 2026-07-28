package org.opentripplanner.ext.carpooling.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.service.DefaultCarpoolingService;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
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
  public static CarpoolTripVertexResolver provideCarpoolTripVertexResolver(
    VertexCreationService vertexCreationService,
    @Nullable CarReachableVertexSnapper carReachableVertexSnapper
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new CarpoolTripVertexResolver(vertexCreationService, carReachableVertexSnapper);
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
