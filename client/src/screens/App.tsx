import { MapView } from '../components/MapView/MapView.tsx';
import { ItineraryListContainer } from '../components/ItineraryList/ItineraryListContainer.tsx';
import { useState, useEffect } from 'react';
import { useTripQuery } from '../hooks/useTripQuery.ts';
import { useServerInfo } from '../hooks/useServerInfo.ts';
import { useTripQueryVariables } from '../hooks/useTripQueryVariables.ts';
import { TimeZoneContext } from '../hooks/TimeZoneContext.ts';
import { LogoSection } from '../components/SearchBar/LogoSection.tsx';
import { InputFieldsSection } from '../components/SearchBar/InputFieldsSection.tsx';
import TripQueryArguments from '../components/SearchInput/TripQueryArguments.tsx';
import Sidebar from '../components/SearchInput/Sidebar.tsx';
import ViewArgumentsRaw from '../components/SearchInput/ViewArgumentsRaw.tsx';
import { TripSchemaProvider } from '../components/SearchInput/TripSchemaProvider.tsx';
import { getApiUrl } from '../util/getApiUrl.ts';

export function App() {
  const serverInfo = useServerInfo();
  const { tripQueryVariables, setTripQueryVariables } = useTripQueryVariables();
  const [tripQueryResult, loading, callback, error] = useTripQuery(tripQueryVariables);
  const [selectedTripPatternIndexes, setSelectedTripPatternIndexes] = useState<number[]>([0]);
  const [expandedArguments, setExpandedArguments] = useState<Record<string, boolean>>({});
  const timeZone = serverInfo?.internalTransitModelTimeZone || Intl.DateTimeFormat().resolvedOptions().timeZone;

  useEffect(() => {
    if (tripQueryResult?.trip.tripPatterns.length) {
      setSelectedTripPatternIndexes([0]);
    }
  }, [tripQueryResult]);

  return (
    <div className="app">
      <TimeZoneContext.Provider value={timeZone}>
        <div className="layout">
          <div className="box logo-section">
            <LogoSection serverInfo={serverInfo}></LogoSection>
          </div>
          <div className="box input-section">
            <InputFieldsSection
              onRoute={callback}
              tripQueryVariables={tripQueryVariables}
              setTripQueryVariables={setTripQueryVariables}
              loading={loading}
            ></InputFieldsSection>
          </div>
          <div className="box trip-section">
            <Sidebar>
              <ItineraryListContainer
                tripQueryResult={tripQueryResult}
                selectedTripPatternIndexes={selectedTripPatternIndexes}
                setSelectedTripPatternIndexes={setSelectedTripPatternIndexes}
                pageResults={callback}
                loading={loading}
                error={error}
              ></ItineraryListContainer>
              <TripSchemaProvider endpoint={getApiUrl()}>
                <TripQueryArguments
                  tripQueryVariables={tripQueryVariables}
                  setTripQueryVariables={setTripQueryVariables}
                  expandedArguments={expandedArguments}
                  setExpandedArguments={setExpandedArguments}
                ></TripQueryArguments>
              </TripSchemaProvider>
              <ViewArgumentsRaw
                tripQueryVariables={tripQueryVariables}
                setTripQueryVariables={setTripQueryVariables}
                setExpandedArguments={setExpandedArguments}
              ></ViewArgumentsRaw>
            </Sidebar>
          </div>
          <div className="box map-section">
            <MapView
              tripQueryResult={tripQueryResult}
              tripQueryVariables={tripQueryVariables}
              setTripQueryVariables={setTripQueryVariables}
              selectedTripPatternIndexes={selectedTripPatternIndexes}
              loading={loading}
            />
          </div>
        </div>
      </TimeZoneContext.Provider>
    </div>
  );
}
