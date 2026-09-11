package org.opentripplanner.ext.taxizone.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.TaxiZoneService;
import org.opentripplanner.ext.taxizone.internal.DefaultTaxiZoneService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;

@Module
public class TaxiZoneServiceModule {

  @Provides
  @Nullable
  public TaxiZoneService provideTaxiZoneService(
    @Nullable TaxiZoneRepository taxiZoneRepository,
    Graph graph,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings
  ) {
    if (OTPFeature.TaxiZone.isOff() || taxiZoneRepository == null) {
      return null;
    }
    return new DefaultTaxiZoneService(
      taxiZoneRepository.getZones(),
      graph,
      streetLimitationParametersService,
      vehicleRentalService,
      streetDetailsService,
      dataOverlayParameterBindings
    );
  }
}
