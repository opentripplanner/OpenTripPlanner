package org.opentripplanner.ext.carpickupzone.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneIndex;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneRepository;
import org.opentripplanner.ext.carpickupzone.internal.itinerary.CarPickupZoneItineraryDecorator;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.algorithm.filterchain.ext.CarPickupZoneDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;

@Module
public class CarPickupZoneServiceModule {

  @Provides
  @Nullable
  @CarPickupZoneDecorator
  public ItineraryListFilter provideCarPickupZoneDecorator(
    @Nullable CarPickupZoneRepository carPickupZoneRepository
  ) {
    if (OTPFeature.CarPickupZone.isOff() || carPickupZoneRepository == null) {
      return null;
    }
    return new CarPickupZoneItineraryDecorator(
      new CarPickupZoneIndex(carPickupZoneRepository.getZones())
    );
  }
}
