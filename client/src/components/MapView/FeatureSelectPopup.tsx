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
      <p style={{ fontWeight: 'bold' }}>Overlapping features</p>
      <Table>
        <tbody>
          {features.map((feature, index) => (
            <tr className="feature-select-item" key={index}>
              <th scope="row">{index}</th>
              <td
                onClick={() => {
                  onClose();
                  setShowPropsPopup({ coordinates, feature });
                }}
              >
                {feature.properties.name || feature.properties.class}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </Popup>
  );
}
