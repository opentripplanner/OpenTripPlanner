package org.opentripplanner.ext.taxizone.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.internal.itinerary.TaxiZoneItineraryDecorator;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.algorithm.filterchain.ext.TaxiZoneDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryListFilter;

@Module
public class TaxiZoneServiceModule {

  @Provides
  @Nullable
  @TaxiZoneDecorator
  public ItineraryListFilter provideTaxiZoneDecorator(
    @Nullable TaxiZoneRepository taxiZoneRepository
  ) {
    if (OTPFeature.TaxiZone.isOff() || taxiZoneRepository == null) {
      return null;
    }
    return new TaxiZoneItineraryDecorator(new TaxiZoneIndex(taxiZoneRepository.getZones()));
  }
}
