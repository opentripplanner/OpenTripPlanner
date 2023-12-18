import { TripQueryVariables } from '../../gql/graphql.ts';
import { Marker } from 'react-map-gl';

export function NavigationMarkers({
  tripQueryVariables,
  setTripQueryVariables,
  loading,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
  loading: boolean;
}) {
  return (
    <>
      {tripQueryVariables.from.coordinates && (
        <Marker
          draggable
          latitude={tripQueryVariables.from.coordinates?.latitude}
          longitude={tripQueryVariables.from.coordinates?.longitude}
          onDragEnd={(e) => {
            if (!loading) {
              setTripQueryVariables({
                ...tripQueryVariables,
                from: { coordinates: { latitude: e.lngLat.lat, longitude: e.lngLat.lng } },
              });
            }
          }}
          anchor="bottom-right"
        >
          <img alt="" src="/debug-client-preview/img/marker-flag-start-shadowed.png" height={48} width={49} />
        </Marker>
      )}
      {tripQueryVariables.to.coordinates && (
        <Marker
          draggable
          latitude={tripQueryVariables.to.coordinates?.latitude}
          longitude={tripQueryVariables.to.coordinates?.longitude}
          onDragEnd={(e) => {
            if (!loading) {
              setTripQueryVariables({
                ...tripQueryVariables,
                to: { coordinates: { latitude: e.lngLat.lat, longitude: e.lngLat.lng } },
              });
            }
          }}
          anchor="bottom-right"
        >
          <img alt="" src="/debug-client-preview/img/marker-flag-end-shadowed.png" height={48} width={49} />
        </Marker>
      )}
    </>
  );
}
