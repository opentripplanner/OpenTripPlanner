package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2_3;

import java.util.List;
import java.util.Objects;
import org.geojson.MultiPolygon;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSFeature;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsGeofencingZoneMapper;

/**
 * A mapper from the raw GBFS type into the internal model of the geofencing zones.
 */
public class GbfsGeofencingZoneMapperV23 extends GbfsGeofencingZoneMapper<GBFSFeature> {

  public GbfsGeofencingZoneMapperV23(String systemId) {
    super(systemId);
  }

  public List<GeofencingZone> mapGeofencingZone(GBFSGeofencingZones input) {
    return input
      .getData()
      .getGeofencingZones()
      .getFeatures()
      .stream()
      .map(this::toInternalModel)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  protected boolean featureBansDropOff(GBFSFeature feature) {
    return !feature.getProperties().getRules().get(0).getRideAllowed();
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
  protected I18NString featureName(GBFSFeature feature) {
    return NonLocalizedString.ofNullable(feature.getProperties().getName());
  }
}
