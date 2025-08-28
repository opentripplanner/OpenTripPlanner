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
      // Remove arriveBy from variables when "Depart after" is selected
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
        Departure/Arrival
      </Form.Label>
      <Form.Select
        size="sm"
        onChange={(e) => (e.target.value === 'arrival' ? onChange(true) : onChange(undefined))}
        value={tripQueryVariables.arriveBy === true ? 'arrival' : 'departure'}
        style={{ verticalAlign: 'bottom' }}
      >
        <option value="arrival">Arrive before</option>
        <option value="departure">Depart after</option>
      </Form.Select>
    </Form.Group>
  );
}
