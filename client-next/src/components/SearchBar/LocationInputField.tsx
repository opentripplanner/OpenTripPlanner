import { Form } from 'react-bootstrap';
import { COORDINATE_PRECISION } from './constants.ts';
import { Location } from '../../gql/graphql.ts';

export function LocationInputField({ location, id, label }: { location?: Location; id: string; label: string }) {
  return (
    <Form.Group>
      <Form.Label htmlFor={id}>{label}</Form.Label>
      <Form.Control
        type="text"
        id={id}
        size="sm"
        placeholder="[Click in map]"
        value={
          location
            ? `${location.coordinates?.latitude.toPrecision(
                COORDINATE_PRECISION,
              )} ${location.coordinates?.longitude.toPrecision(COORDINATE_PRECISION)}`
            : undefined
        }
      />
    </Form.Group>
  );
}
