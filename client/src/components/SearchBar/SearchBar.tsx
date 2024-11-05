import { Button, ButtonGroup, Spinner } from 'react-bootstrap';
import { ServerInfo, TripQueryVariables } from '../../gql/graphql.ts';
import { LocationInputField } from './LocationInputField.tsx';
import { DepartureArrivalSelect } from './DepartureArrivalSelect.tsx';
import { DateTimeInputField } from './DateTimeInputField.tsx';
import { SearchWindowInput } from './SearchWindowInput.tsx';
import { AccessSelect } from './AccessSelect.tsx';
import { EgressSelect } from './EgressSelect.tsx';
import { DirectModeSelect } from './DirectModeSelect.tsx';
import { TransitModeSelect } from './TransitModeSelect.tsx';
import { NumTripPatternsInput } from './NumTripPatternsInput.tsx';
import { ItineraryFilterDebugSelect } from './ItineraryFilterDebugSelect.tsx';
import Navbar from 'react-bootstrap/Navbar';
import { ServerInfoTooltip } from './ServerInfoTooltip.tsx';
import { useRef, useState } from 'react';
import logo from '../../static/img/otp-logo.svg';
import GraphiQLRouteButton from './GraphiQLRouteButton.tsx';
import WheelchairAccessibleCheckBox from './WheelchairAccessibleCheckBox.tsx';
import { SwapLocationsButton } from './SwapLocationsButton.tsx';

type SearchBarProps = {
  onRoute: () => void;
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
  serverInfo?: ServerInfo;
  loading: boolean;
};

export function SearchBar({ onRoute, tripQueryVariables, setTripQueryVariables, serverInfo, loading }: SearchBarProps) {
  const [showServerInfo, setShowServerInfo] = useState(false);
  const target = useRef(null);

  return (
    <div className="search-bar top-content">
      <Navbar.Brand onClick={() => setShowServerInfo((v) => !v)}>
        <div style={{ position: 'relative' }} ref={target}>
          <img alt="" src={logo} width="30" height="30" className="d-inline-block align-top" /> OTP Debug Client
          {showServerInfo && <ServerInfoTooltip serverInfo={serverInfo} target={target} />}
        </div>
      </Navbar.Brand>
      <LocationInputField location={tripQueryVariables.from} label="From" id="fromInputField" />
      <SwapLocationsButton tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <LocationInputField location={tripQueryVariables.to} label="To" id="toInputField" />
      <DepartureArrivalSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <DateTimeInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <NumTripPatternsInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <SearchWindowInput tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <AccessSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <TransitModeSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <EgressSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <DirectModeSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <ItineraryFilterDebugSelect
        tripQueryVariables={tripQueryVariables}
        setTripQueryVariables={setTripQueryVariables}
      />
      <WheelchairAccessibleCheckBox
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
          <GraphiQLRouteButton tripQueryVariables={tripQueryVariables}></GraphiQLRouteButton>
        </ButtonGroup>
      </div>
    </div>
  );
}
