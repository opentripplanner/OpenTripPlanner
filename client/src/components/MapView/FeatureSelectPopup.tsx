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
    <Popup maxWidth="600px" latitude={coordinates.lat} longitude={coordinates.lng} onClose={onClose}>
      <strong>Overlapping features</strong>
      <Table bordered>
        <thead>
          <tr>
            <th scope="col">layer id</th>
            <th scope="col">class</th>
            <th scope="col">name/label</th>
          </tr>
        </thead>
        <tbody>
          {features.map((feature, index) => (
            <tr
              onClick={() => {
                onClose();
                setShowPropsPopup({ coordinates, feature });
              }}
              className="feature-select-item"
              key={index}
            >
              <td>{feature.layer.id}</td>
              <td>{feature.properties.class}</td>
              <td>{feature.properties.name || feature.properties.label || 'n/a'}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Popup>
  );
}
