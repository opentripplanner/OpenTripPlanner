import { Button, Spinner } from 'react-bootstrap';
import { ServerInfo, TripQueryVariables } from '../../gql/graphql.ts';
import { LocationInputField } from './LocationInputField.tsx';
import { DepartureArrivalSelect } from './DepartureArrivalSelect.tsx';
import { TimeInputField } from './TimeInputField.tsx';
import { DateInputField } from './DateInputField.tsx';
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
      <LocationInputField location={tripQueryVariables.to} label="To" id="toInputField" />
      <DepartureArrivalSelect tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <TimeInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      <DateInputField tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
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
      <div className="search-bar-route-button-wrapper">
        <Button variant="primary" onClick={() => onRoute()} disabled={loading}>
          {loading && (
            <>
              <Spinner as="span" animation="border" size="sm" role="status" aria-hidden="true" />{' '}
            </>
          )}
          Route
        </Button>
      </div>
    </div>
  );
}
