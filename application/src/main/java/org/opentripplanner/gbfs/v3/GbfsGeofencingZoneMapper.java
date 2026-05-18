package org.opentripplanner.gbfs.v3;

import static org.opentripplanner.gbfs.v3.GbfsFeedMapper.optionalLocalizedString;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.geojson.MultiPolygon;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSFeature;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSGeofencingZones;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSName;
import org.mobilitydata.gbfs.v3_0.geofencing_zones.GBFSRule;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;

/**
 * A mapper from the raw GBFS v3 type into the internal model of the geofencing zones. Each rule
 * within a zone becomes a separate GeofencingZone with its own priority.
 */
class GbfsGeofencingZoneMapper
  extends org.opentripplanner.gbfs.GbfsGeofencingZoneMapper<GBFSFeature, GBFSRule> {

  public GbfsGeofencingZoneMapper(String systemId) {
    super(systemId);
  }

  public List<GeofencingZone> mapGeofencingZone(GBFSGeofencingZones input) {
    var features = input.getData().getGeofencingZones().getFeatures();
    var zones = new ArrayList<GeofencingZone>();
    for (int i = 0; i < features.size(); i++) {
      if (features.get(i).getGeometry() != null) {
        zones.addAll(toInternalModel(features.get(i), i));
      }
    }
    return zones;
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
  protected @Nullable Boolean ruleBansDropOff(GBFSRule rule) {
    Boolean val = rule.getRideEndAllowed();
    return val == null ? null : !val;
  }

  @Override
  protected @Nullable Boolean ruleBansPassThrough(GBFSRule rule) {
    Boolean val = rule.getRideThroughAllowed();
    return val == null ? null : !val;
  }

  @Override
  protected @Nullable Boolean ruleBansRideStart(GBFSRule rule) {
    Boolean val = rule.getRideStartAllowed();
    return val == null ? null : !val;
  }

  @Override
  protected @Nullable List<String> ruleVehicleTypeIds(GBFSRule rule) {
    return rule.getVehicleTypeIds();
  }

  @Override
  protected @Nullable Integer ruleMaximumSpeedKph(GBFSRule rule) {
    return rule.getMaximumSpeedKph();
  }
}
