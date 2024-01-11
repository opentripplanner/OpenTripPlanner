import { LngLat, Popup } from 'react-map-gl';
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
    <Popup latitude={coordinates.lat} longitude={coordinates.lng} closeButton={true} onClose={() => onClose()}>
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
    </Popup>
  );
}
