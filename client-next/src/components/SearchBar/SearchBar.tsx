import { Button, Stack } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { LocationInputField } from './LocationInputField.tsx';
import { DepartureArrivalSelect } from './DepartureArrivalSelect.tsx';
import { TimeInputField } from './TimeInputField.tsx';
import { DateInputField } from './DateInputField.tsx';
import { SearchWindowInput } from './SearchWindowInput.tsx';

export function SearchBar({
  onRoute,
  tripQueryVariables,
  setTripQueryVariables,
}: {
  onRoute: () => void;
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  return (
    <section className="search-bar-container">
      <Stack direction="horizontal" gap={2}>
        <LocationInputField location={tripQueryVariables.from} label="From" id="fromInputField" />
        <LocationInputField location={tripQueryVariables.to} label="To" id="toInputField" />
        <DepartureArrivalSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <TimeInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <DateInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <SearchWindowInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <div className="search-bar-route-button-wrapper">
          <Button variant="primary" onClick={onRoute}>
            Route
          </Button>
        </div>
      </Stack>
    </section>
  );
}
