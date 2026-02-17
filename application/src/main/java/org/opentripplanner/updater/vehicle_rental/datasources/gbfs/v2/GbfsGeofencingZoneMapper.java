package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2;

import java.util.List;
import java.util.stream.IntStream;
import org.geojson.MultiPolygon;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSFeature;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v2_3.geofencing_zones.GBFSRule;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * A mapper from the raw GBFS v2 type into the internal model of the geofencing zones.
 * Each rule within a zone becomes a separate GeofencingZone with its own priority.
 */
class GbfsGeofencingZoneMapper
  extends org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsGeofencingZoneMapper<
    GBFSFeature,
    GBFSRule
  > {

  public GbfsGeofencingZoneMapper(String systemId) {
    super(systemId);
  }

  public List<GeofencingZone> mapGeofencingZone(GBFSGeofencingZones input) {
    var features = input.getData().getGeofencingZones().getFeatures();
    return IntStream.range(0, features.size())
      .boxed()
      .flatMap(zoneIndex -> toInternalModel(features.get(zoneIndex), zoneIndex).stream())
      .toList();
  }

  @Override
  protected MultiPolygon featureGeometry(GBFSFeature feature) {
    return feature.getGeometry();
  }

  @Override
  protected I18NString featureName(GBFSFeature feature) {
    return NonLocalizedString.ofNullable(feature.getProperties().getName());
  }

  @Override
  protected List<GBFSRule> featureRules(GBFSFeature feature) {
    return feature.getProperties().getRules();
  }

  @Override
  protected boolean ruleBansDropOff(GBFSRule rule) {
    return !rule.getRideAllowed();
  }

  @Override
  protected boolean ruleBansPassThrough(GBFSRule rule) {
    return !rule.getRideThroughAllowed();
  }
}
