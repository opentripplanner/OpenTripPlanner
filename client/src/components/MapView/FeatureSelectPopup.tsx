import { Table } from 'react-bootstrap';
import { Popup, LngLat, MapGeoJSONFeature } from 'react-map-gl/maplibre';

export function FeatureSelectPopup({
  coordinates,
  features,
  onClose,
  setShowPropsPopup,
}: {
  coordinates: LngLat;
  features: MapGeoJSONFeature[];
  onClose: () => void;
  setShowPropsPopup: ({ coordinates, feature }: { coordinates: LngLat; feature: MapGeoJSONFeature }) => void;
}) {
  return (
    <Popup latitude={coordinates.lat} longitude={coordinates.lng} onClose={onClose}>
      <Table>
        <tbody>
          {features.map((feature, index) => (
            <tr key={index}>
              <th scope="row">{index}</th>
              <td onClick={() => setShowPropsPopup({ coordinates, feature })}>{String(feature.properties.class)}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Popup>
  );
}
