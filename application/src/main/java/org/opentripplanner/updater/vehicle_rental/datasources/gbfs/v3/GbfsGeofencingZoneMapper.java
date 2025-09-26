package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.geojson.MultiPolygon;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSFeature;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSName;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * A mapper from the raw GBFS type into the internal model of the geofencing zones.
 */
class GbfsGeofencingZoneMapper
  extends org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsGeofencingZoneMapper<
    GBFSFeature
  > {

  public GbfsGeofencingZoneMapper(String systemId) {
    super(systemId);
  }

  public List<GeofencingZone> mapGeofencingZone(GBFSGeofencingZones input) {
    return input
      .getData()
      .getGeofencingZones()
      .getFeatures()
      .stream()
      .filter(f -> f.getGeometry() != null)
      .map(this::toInternalModel)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  protected boolean featureBansDropOff(GBFSFeature feature) {
    return !feature.getProperties().getRules().get(0).getRideEndAllowed();
  }

  @Override
  protected boolean featureBansPassThrough(GBFSFeature feature) {
    return !feature.getProperties().getRules().get(0).getRideThroughAllowed();
  }

  @Override
  protected MultiPolygon featureGeometry(GBFSFeature feature) {
    return feature.getGeometry();
  }

  @Override
  protected @Nullable I18NString featureName(GBFSFeature feature) {
    return optionalLocalizedString(
      feature.getProperties().getName(),
      GBFSName::getLanguage,
      GBFSName::getText
    );
  }
}
