/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs.model;

import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.serialization.mappings.DefaultAgencyIdFieldMappingFactory;
import org.onebusaway.gtfs.serialization.mappings.LatLonFieldMappingFactory;

@CsvFields(filename = "stops.txt", prefix = "stop_")
public final class Stop extends IdentityBean<AgencyAndId> {

  private static final long serialVersionUID = 1L;
  
  private static final int MISSING_VALUE = -999;

  @CsvField(mapping = DefaultAgencyIdFieldMappingFactory.class)
  private AgencyAndId id;

  private String name;

  @CsvField(mapping = LatLonFieldMappingFactory.class)
  private double lat;

  @CsvField(mapping = LatLonFieldMappingFactory.class)
  private double lon;

  @CsvField(optional = true)
  private String code;

  @CsvField(optional = true)
  private String desc;

  @CsvField(name="zone_id", optional = true)
  private String zoneId;

  @CsvField(optional = true)
  private String url;

  @CsvField(name="location_type", optional = true, defaultValue = "0")
  private int locationType = 0;

  @CsvField(name="parent_station", optional = true)
  private String parentStation;

  @CsvField(name="wheelchair_boarding", optional = true, defaultValue = "0")
  private int wheelchairBoarding = 0;

  @CsvField(optional = true)
  private String direction;

  @CsvField(optional = true)
  private String timezone;
  
  @CsvField(name="vehicle_type", optional = true)
  private int vehicleType = MISSING_VALUE;

  @CsvField(name="platform_code", optional = true)
  private String platformCode;

  public Stop() {

  }

  public Stop(Stop obj) {
    this.id = obj.id;
    this.code = obj.code;
    this.name = obj.name;
    this.desc = obj.desc;
    this.lat = obj.lat;
    this.lon = obj.lon;
    this.zoneId = obj.zoneId;
    this.url = obj.url;
    this.locationType = obj.locationType;
    this.parentStation = obj.parentStation;
    this.wheelchairBoarding = obj.wheelchairBoarding;
    this.direction = obj.direction;
    this.timezone = obj.timezone;
    this.vehicleType = obj.vehicleType;
    this.platformCode = obj.platformCode;
  }

  public AgencyAndId getId() {
    return id;
  }

  public void setId(AgencyAndId id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public String getZoneId() {
    return zoneId;
  }

  public void setZoneId(String zoneId) {
    this.zoneId = zoneId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getLocationType() {
    return locationType;
  }

  public void setLocationType(int locationType) {
    this.locationType = locationType;
  }

  public String getParentStation() {
    return parentStation;
  }

  public void setParentStation(String parentStation) {
    this.parentStation = parentStation;
  }

  @Override
  public String toString() {
    return "<Stop " + this.id + ">";
  }

  public void setWheelchairBoarding(int wheelchairBoarding) {
    this.wheelchairBoarding = wheelchairBoarding;
  }

  public int getWheelchairBoarding() {
    return wheelchairBoarding;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }
  
  public boolean isVehicleTypeSet() {
    return vehicleType != MISSING_VALUE;
  }

  public int getVehicleType() {
    return vehicleType;
  }

  public void setVehicleType(int vehicleType) {
    this.vehicleType = vehicleType;
  }
  
  public void clearVehicleType() {
    vehicleType = MISSING_VALUE;
  }

  public String getPlatformCode() {
    return platformCode;
  }

  public void setPlatformCode(String platformCode) {
    this.platformCode = platformCode;
  }
}
