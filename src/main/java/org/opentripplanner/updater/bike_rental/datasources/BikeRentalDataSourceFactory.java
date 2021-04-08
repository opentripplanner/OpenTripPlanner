package org.opentripplanner.updater.bike_rental.datasources;

import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.GenericKmlBikeRentalDataSourceParameters;

public class BikeRentalDataSourceFactory {

  public static BikeRentalDataSource create(BikeRentalDataSourceParameters source) {
    switch (source.getSourceType()) {
      case JCDECAUX:      return new JCDecauxBikeRentalDataSource(source);
      case B_CYCLE:       return new BCycleBikeRentalDataSource(source);
      case BIXI:          return new BixiBikeRentalDataSource(source);
      case KEOLIS_RENNES: return new KeolisRennesBikeRentalDataSource(source);
      case OV_FIETS:      return new OVFietsKMLDataSource(source);
      case CITY_BIKES:    return new CityBikesBikeRentalDataSource(source);
      case VCUV:          return new VCubDataSource(source);
      case CITI_BIKE_NYC: return new CitiBikeNycBikeRentalDataSource(source);
      case NEXT_BIKE:     return new NextBikeRentalDataSource(source);
      case KML:           return new GenericKmlBikeRentalDataSource(
          (GenericKmlBikeRentalDataSourceParameters) source
      );
      case SF_BAY_AREA:   return new SanFranciscoBayAreaBikeRentalDataSource(source);
      case SHARE_BIKE:    return new ShareBikeRentalDataSource(source);
      case UIP_BIKE:      return new UIPBikeRentalDataSource(source);
      case GBFS:          return new GbfsBikeRentalDataSource((GbfsBikeRentalDataSourceParameters) source);
      case SMOOVE:        return new SmooveBikeRentalDataSource(source);
      case BICIMAD:       return new BicimadBikeRentalDataSource(source);
    }
    throw new IllegalArgumentException(
        "Unknown bike rental source type: " + source.getSourceType()
    );
  }
}
