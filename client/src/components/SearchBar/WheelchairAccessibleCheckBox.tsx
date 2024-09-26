import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';

export default function WheelchairAccessibleCheckBox({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="wheelchairAccessibleCheck">
        Wheelchair accessible trip
      </Form.Label>
      <Form.Check
        id="wheelchairAccessible"
        onChange={(e) => {
          setTripQueryVariables({
            ...tripQueryVariables,
            wheelchairAccessible: e.target.checked,
          });
        }}
      ></Form.Check>
    </Form.Group>
  );
}
