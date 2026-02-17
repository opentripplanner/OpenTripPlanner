package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3;

import static org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.geojson.MultiPolygon;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSFeature;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSName;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSRule;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * A mapper from the raw GBFS v3 type into the internal model of the geofencing zones.
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
      .filter(zoneIndex -> features.get(zoneIndex).getGeometry() != null)
      .flatMap(zoneIndex -> toInternalModel(features.get(zoneIndex), zoneIndex).stream())
      .toList();
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

  @Override
  protected List<GBFSRule> featureRules(GBFSFeature feature) {
    return feature.getProperties().getRules();
  }

  @Override
  protected boolean ruleBansDropOff(GBFSRule rule) {
    return !rule.getRideEndAllowed();
  }

  @Override
  protected boolean ruleBansPassThrough(GBFSRule rule) {
    return !rule.getRideThroughAllowed();
  }
}
