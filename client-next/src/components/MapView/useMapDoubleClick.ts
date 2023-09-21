import { useCallback } from 'react';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { LngLat, MapLayerMouseEvent } from 'react-map-gl';

const setFromCoordinates = (tripQueryVariables: TripQueryVariables, lngLat: LngLat) => ({
  ...tripQueryVariables,
  from: {
    coordinates: {
      latitude: lngLat.lat,
      longitude: lngLat.lng,
    },
  },
  to: {
    coordinates: {
      latitude: 0.0,
      longitude: 0.0,
    },
  },
});

const setToCoordinates = (tripQueryVariables: TripQueryVariables, lngLat: LngLat) => ({
  ...tripQueryVariables,
  to: {
    coordinates: {
      latitude: lngLat.lat,
      longitude: lngLat.lng,
    },
  },
});

export function useMapDoubleClick({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables?: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
}) {
  return useCallback(
    (event: MapLayerMouseEvent) => {
      event.preventDefault();
      if (!tripQueryVariables?.from.coordinates) {
        setTripQueryVariables(setFromCoordinates(tripQueryVariables!, event.lngLat));
      } else {
        setTripQueryVariables(setToCoordinates(tripQueryVariables!, event.lngLat));
      }
    },
    [tripQueryVariables, setTripQueryVariables],
  );
}
