import { Form } from 'react-bootstrap';
import wheelchairIcon from '../../static/img/wheelchair.svg';
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
        <img
          alt="Wheelchair Accessible Trip"
          title="Wheelchair Accessible Trip"
          src={wheelchairIcon}
          width="20"
          height="20"
          className="d-inline-block align-middle"
        />
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
