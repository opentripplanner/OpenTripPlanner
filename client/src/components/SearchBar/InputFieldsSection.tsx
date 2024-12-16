import { Button, ButtonGroup, Spinner } from 'react-bootstrap';
import { TripQueryVariables } from '../../gql/graphql.ts';
import { LocationInputField } from './LocationInputField.tsx';
import { SwapLocationsButton } from './SwapLocationsButton.tsx';
import { DepartureArrivalSelect } from './DepartureArrivalSelect.tsx';
import { DateTimeInputField } from './DateTimeInputField.tsx';
import { SearchWindowInput } from './SearchWindowInput.tsx';
import { AccessSelect } from './AccessSelect.tsx';
import { EgressSelect } from './EgressSelect.tsx';
import { DirectModeSelect } from './DirectModeSelect.tsx';
import { TransitModeSelect } from './TransitModeSelect.tsx';
import { NumTripPatternsInput } from './NumTripPatternsInput.tsx';
import { ItineraryFilterDebugSelect } from './ItineraryFilterDebugSelect.tsx';
import GraphiQLRouteButton from './GraphiQLRouteButton.tsx';

type InputFieldsSectionProps = {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
  onRoute: () => void;
  loading: boolean;
};

export function InputFieldsSection({
  tripQueryVariables,
  setTripQueryVariables,
  onRoute,
  loading,
}: InputFieldsSectionProps) {
  return (
    <div className="box input-group search-bar">
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <LocationInputField location={tripQueryVariables.from} label="From" id="fromInputField" />
        <SwapLocationsButton tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <LocationInputField location={tripQueryVariables.to} label="To" id="toInputField" />
      </div>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <DepartureArrivalSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <DateTimeInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <NumTripPatternsInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <SearchWindowInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <AccessSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <TransitModeSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <EgressSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <DirectModeSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>
      <ItineraryFilterDebugSelect
        tripQueryVariables={tripQueryVariables}
        setTripQueryVariables={setTripQueryVariables}
      />

      <div className="search-bar-route-button-wrapper">
        <ButtonGroup>
          <Button variant="primary" onClick={() => onRoute()} disabled={loading}>
            {loading && (
              <>
                <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />{' '}
              </>
            )}
            Route
          </Button>
          <GraphiQLRouteButton tripQueryVariables={tripQueryVariables} />
        </ButtonGroup>
      </div>
    </div>
  );
}
