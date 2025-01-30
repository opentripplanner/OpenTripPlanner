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
      <div className="input-family">
        <LocationInputField
          label="From"
          id="fromInputField"
          locationFieldKey="from"
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
        />
        <SwapLocationsButton tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <LocationInputField
          label="To"
          id="toInputField"
          locationFieldKey="to"
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
        />
      </div>
      <div className="input-family">
        <DepartureArrivalSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <DateTimeInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>
      <div className="input-family">
        <NumTripPatternsInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
        <SearchWindowInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>
      <div className="input-family">
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
