import { TripQueryVariables } from '../../gql/graphql.ts';
import { Marker } from 'react-map-gl/maplibre';
import markerFlagStart from '../../static/img/marker-flag-start-shadowed.png';
import markerFlagEnd from '../../static/img/marker-flag-end-shadowed.png';
import { useCoordinateResolver } from './useCoordinateResolver.ts';

export function NavigationMarkers({
  setCursor,
  tripQueryVariables,
  setTripQueryVariables,
  loading,
}: {
  setCursor: (cursor: string) => void;
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
  loading: boolean;
}) {
  const fromCoordinates = useCoordinateResolver(tripQueryVariables.from);
  const toCoordinates = useCoordinateResolver(tripQueryVariables.to);

  return (
    <>
      {fromCoordinates && (
        <Marker
          draggable
          latitude={fromCoordinates.latitude}
          longitude={fromCoordinates.longitude}
          onDragStart={() => setCursor('grabbing')}
          onDragEnd={(e) => {
            setCursor('auto');
            if (!loading) {
              setTripQueryVariables({
                ...tripQueryVariables,
                from: { coordinates: { latitude: e.lngLat.lat, longitude: e.lngLat.lng } },
              });
            }
          }}
          anchor="bottom-right"
        >
          <img alt="" src={markerFlagStart} height={48} width={49} />
        </Marker>
      )}
      {toCoordinates && (
        <Marker
          draggable
          latitude={toCoordinates.latitude}
          longitude={toCoordinates.longitude}
          onDragStart={() => setCursor('grabbing')}
          onDragEnd={(e) => {
            setCursor('auto');
            if (!loading) {
              setTripQueryVariables({
                ...tripQueryVariables,
                to: { coordinates: { latitude: e.lngLat.lat, longitude: e.lngLat.lng } },
              });
            }
          }}
          anchor="bottom-right"
        >
          <img alt="" src={markerFlagEnd} height={48} width={49} />
        </Marker>
      )}
    </>
  );
}
