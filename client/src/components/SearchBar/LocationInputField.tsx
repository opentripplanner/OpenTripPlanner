import { Form } from 'react-bootstrap';
import { toString, parseLocation } from '../../util/locationConverter.ts';
import { Location } from '../../gql/graphql.ts';
import { useEffect, useState } from 'react';

interface Props {
  location: Location;
  onLocationChange: (location: Location) => void;
  id: string;
  label: string;
}

export function LocationInputField({ location, onLocationChange, id, label }: Props) {
  const [value, setValue] = useState('');

  useEffect(() => {
    setValue(toString(location) || '');
  }, [location]);

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
        // Intentionally empty for now, but needed because of
        // https://react.dev/reference/react-dom/components/input#controlling-an-input-with-a-state-variable
        onChange={(e) => {
          setValue(e.target.value);
        }}
        onBlur={(event) => {
          const location = parseLocation(event.target.value);
          if (location) {
            onLocationChange(location);
          }
        }}
        value={value}
      />
    </Form.Group>
  );
}
