import { TripQueryVariables } from '../../gql/graphql.ts';
import { LngLat, Popup } from 'react-map-gl/maplibre';
import { Button, ButtonGroup } from 'react-bootstrap';

export function ContextMenuPopup({
  tripQueryVariables,
  setTripQueryVariables,
  coordinates,
  onClose,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (variables: TripQueryVariables) => void;
  coordinates: LngLat;
  onClose: () => void;
}) {
  return (
    <Popup longitude={coordinates.lng} latitude={coordinates.lat} anchor="bottom" onClose={onClose}>
      <ButtonGroup vertical>
        <Button
          onClick={() => {
            setTripQueryVariables({
              ...tripQueryVariables,
              from: {
                coordinates: {
                  latitude: coordinates.lat,
                  longitude: coordinates.lng,
                },
              },
            });
            onClose();
          }}
        >
          Start here
        </Button>
        <Button
          onClick={() => {
            setTripQueryVariables({
              ...tripQueryVariables,
              to: {
                coordinates: {
                  latitude: coordinates.lat,
                  longitude: coordinates.lng,
                },
              },
            });
            onClose();
          }}
        >
          End here
        </Button>
      </ButtonGroup>
    </Popup>
  );
}
