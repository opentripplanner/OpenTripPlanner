import { Form } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';

export function DepartureArrivalSelect({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const onChange = (arriveBy: boolean | undefined) => {
    if (arriveBy === undefined) {
      const updatedVariables = { ...tripQueryVariables };
      delete updatedVariables.arriveBy;
      setTripQueryVariables(updatedVariables);
    } else {
      setTripQueryVariables({
        ...tripQueryVariables,
        arriveBy,
      });
    }
  };

  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor="departureArrivalSelect">
        Depart/Arrive
      </Form.Label>
      <Form.Select
        size="sm"
        className="input-medium"
        onChange={(e) => (e.target.value === 'arrival' ? onChange(true) : onChange(undefined))}
        value={tripQueryVariables.arriveBy ? 'arrival' : 'departure'}
        style={{ verticalAlign: 'bottom' }}
      >
        <option value="arrival">Arrive by</option>
        <option value="departure">Depart after</option>
      </Form.Select>
    </Form.Group>
  );
}
