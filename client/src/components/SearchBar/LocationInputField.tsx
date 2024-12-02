import { Form } from 'react-bootstrap';
import { toString, parseLocation } from '../../util/locationConverter.ts';
import { Location, TripQueryVariables } from '../../gql/graphql.ts';
import { useCallback, useEffect, useState } from 'react';

interface Props {
  id: string;
  label: string;
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
  locationFieldKey: 'from' | 'to';
}

export function LocationInputField({ id, label, tripQueryVariables, setTripQueryVariables, locationFieldKey }: Props) {
  const [value, setValue] = useState('');

  useEffect(() => {
    const initialLocation: Location = tripQueryVariables[locationFieldKey];

    setValue(toString(initialLocation) || '');
  }, [tripQueryVariables, locationFieldKey]);

  const onLocationChange = useCallback(
    (value: string) => {
      const newLocation = parseLocation(value) || {};

      setTripQueryVariables({
        ...tripQueryVariables,
        [locationFieldKey]: newLocation,
      });
    },
    [tripQueryVariables, setTripQueryVariables, locationFieldKey],
  );

  return (
    <Form.Group>
      <Form.Label column="sm" htmlFor={id}>
        {label}
      </Form.Label>
      <Form.Control
        type="text"
        id={id}
        size="sm"
        placeholder="[Click in map]"
        className="input-medium"
        onChange={(e) => {
          setValue(e.target.value);
        }}
        onBlur={(event) => {
          onLocationChange(event.target.value);
        }}
        value={value}
      />
    </Form.Group>
  );
}
