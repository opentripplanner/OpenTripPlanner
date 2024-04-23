import { LngLat, Popup } from 'react-map-gl/maplibre';
import { Table } from 'react-bootstrap';

export function GeometryPropertyPopup({
  coordinates,
  properties,
  onClose,
}: {
  coordinates: LngLat;
  properties: { [s: string]: string };
  onClose: () => void;
}) {
  return (
    <Popup
      latitude={coordinates.lat}
      longitude={coordinates.lng}
      closeButton={true}
      onClose={() => onClose()}
      maxWidth="350px"
    >
      <Table bordered>
        <tbody>
          {Object.entries(properties).map(([key, value]) => (
            <tr key={key}>
              <th scope="row">{key}</th>
              <td>{value}</td>
            </tr>
          ))}
        </tbody>
      </Table>
      <div className="geometry-popup-osm-link">
        <a
          href={`https://www.openstreetmap.org/?mlat=${coordinates.lat}&mlon=${coordinates.lng}#map=18/${coordinates.lat}/${coordinates.lng}`}
          target="_blank"
          rel="noreferrer"
        >
          ↗️ osm.org
        </a>
      </div>
    </Popup>
  );
}
