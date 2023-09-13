import { Button, Form, Stack } from 'react-bootstrap';
import { TripQueryVariables } from '../gql/graphql.ts';

const COORDINATE_PRECISION = 7;

export function SearchBarContainer({
  onRoute,
  tripQueryVariables,
}: {
  onRoute: () => void;
  tripQueryVariables?: TripQueryVariables;
}) {
  return (
    <section
      style={{
        height: '5rem',
        paddingLeft: '1rem',
      }}
    >
      <Stack direction="horizontal" gap={2}>
        <Form.Group>
          <Form.Label htmlFor="fromInputField">From</Form.Label>
          <Form.Control
            type="text"
            id="fromInputField"
            size="sm"
            placeholder="[Click in map]"
            value={
              tripQueryVariables?.from
                ? `${tripQueryVariables?.from.coordinates?.latitude.toPrecision(
                    COORDINATE_PRECISION,
                  )} ${tripQueryVariables?.from.coordinates?.longitude.toPrecision(COORDINATE_PRECISION)}`
                : undefined
            }
          />
        </Form.Group>
        <Form.Group>
          <Form.Label htmlFor="toInputField">To</Form.Label>
          <Form.Control
            type="text"
            id="toInputField"
            size="sm"
            placeholder="[Click in map]"
            value={
              tripQueryVariables?.to
                ? `${tripQueryVariables?.to.coordinates?.latitude.toPrecision(
                    COORDINATE_PRECISION,
                  )} ${tripQueryVariables?.to.coordinates?.longitude.toPrecision(COORDINATE_PRECISION)}`
                : undefined
            }
          />
        </Form.Group>

        <Button variant="primary" onClick={onRoute}>
          Route
        </Button>
      </Stack>
    </section>
  );
}
