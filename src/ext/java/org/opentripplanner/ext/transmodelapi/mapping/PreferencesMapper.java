package org.opentripplanner.ext.transmodelapi.mapping;

import static org.opentripplanner.ext.transmodelapi.mapping.preferences.BikePreferencesMapper.mapBikePreferences;
import static org.opentripplanner.ext.transmodelapi.mapping.preferences.ItineraryFilterPreferencesMapper.mapItineraryFilterPreferences;
import static org.opentripplanner.ext.transmodelapi.mapping.preferences.ItineraryFilterPreferencesMapper.mapRentalPreferences;
import static org.opentripplanner.ext.transmodelapi.mapping.preferences.StreetPreferencesMapper.mapStreetPreferences;
import static org.opentripplanner.ext.transmodelapi.mapping.preferences.TransferPreferencesMapper.mapTransferPreferences;
import static org.opentripplanner.ext.transmodelapi.mapping.preferences.TransitPreferencesMapper.mapTransitPreferences;
import static org.opentripplanner.ext.transmodelapi.mapping.preferences.WalkPreferencesMapper.mapWalkPreferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

class PreferencesMapper {

  static void mapPreferences(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RoutingPreferences.Builder preferences
  ) {
    preferences.withWalk(walk -> mapWalkPreferences(walk, environment, callWith));
    preferences.withStreet(street -> mapStreetPreferences(street, environment, preferences.street())
    );
    preferences.withBike(bike -> mapBikePreferences(bike, callWith));
    preferences.withTransfer(transfer -> mapTransferPreferences(transfer, environment, callWith));
    preferences.withTransit(transit -> mapTransitPreferences(transit, environment, callWith));
    preferences.withItineraryFilter(itineraryFilter ->
      mapItineraryFilterPreferences(itineraryFilter, environment, callWith)
    );
    preferences.withRental(rental -> mapRentalPreferences(rental, environment, callWith));
  }
}
