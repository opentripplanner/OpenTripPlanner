import { useCallback } from 'react';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { LngLat, MapLayerMouseEvent } from 'react-map-gl/maplibre';

const setCoordinates = (tripQueryVariables: TripQueryVariables, lngLat: LngLat, key: 'from' | 'to') => ({
  ...tripQueryVariables,
  [key]: {
    coordinates: {
      latitude: lngLat.lat,
      longitude: lngLat.lng,
    },
  },
});

const setFromCoordinates = (tripQueryVariables: TripQueryVariables, lngLat: LngLat) =>
  setCoordinates(tripQueryVariables, lngLat, 'from');

const setToCoordinates = (tripQueryVariables: TripQueryVariables, lngLat: LngLat) =>
  setCoordinates(tripQueryVariables, lngLat, 'to');

export function useMapDoubleClick({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
}) {
  return useCallback(
    (event: MapLayerMouseEvent) => {
      event.preventDefault();
      if (!tripQueryVariables.from.coordinates) {
        setTripQueryVariables(setFromCoordinates(tripQueryVariables, event.lngLat));
      } else {
        setTripQueryVariables(setToCoordinates(tripQueryVariables, event.lngLat));
      }
    },
    [tripQueryVariables, setTripQueryVariables],
  );
}
