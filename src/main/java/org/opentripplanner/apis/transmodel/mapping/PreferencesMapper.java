package org.opentripplanner.apis.transmodel.mapping;

import static org.opentripplanner.apis.transmodel.mapping.preferences.BikePreferencesMapper.mapBikePreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.CarPreferencesMapper.mapCarPreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.ItineraryFilterPreferencesMapper.mapItineraryFilterPreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.ScooterPreferencesMapper.mapScooterPreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.StreetPreferencesMapper.mapStreetPreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.TransferPreferencesMapper.mapTransferPreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.TransitPreferencesMapper.mapTransitPreferences;
import static org.opentripplanner.apis.transmodel.mapping.preferences.WalkPreferencesMapper.mapWalkPreferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

class PreferencesMapper {

  static void mapPreferences(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RoutingPreferences.Builder preferences
  ) {
    preferences.withStreet(street -> mapStreetPreferences(street, environment, preferences.street())
    );
    preferences.withWalk(walk -> mapWalkPreferences(walk, callWith));
    preferences.withBike(bike -> mapBikePreferences(bike, callWith));
    preferences.withCar(car -> mapCarPreferences(car, callWith));
    preferences.withScooter(scooter -> mapScooterPreferences(scooter, callWith));
    preferences.withTransfer(transfer -> mapTransferPreferences(transfer, environment, callWith));
    preferences.withTransit(transit -> mapTransitPreferences(transit, environment, callWith));
    preferences.withItineraryFilter(itineraryFilter ->
      mapItineraryFilterPreferences(itineraryFilter, environment, callWith)
    );
  }
}
