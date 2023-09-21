import { Stack } from 'react-bootstrap';
import { MapView } from '../components/MapView/MapView.tsx';
import { NavBarContainer } from '../components/NavBarContainer.tsx';
import { SearchBarContainer } from '../components/SearchBarContainer.tsx';
import { ItineraryListContainer } from '../components/ItineraryListContainer.tsx';
import { DetailsViewContainer } from '../components/DetailsViewContainer.tsx';
import { useState } from 'react';
import { TripQueryVariables } from '../gql/graphql.ts';
import { useTripQuery } from '../hooks/useTripQuery.ts';

export function App() {
  const [tripQueryVariables, setTripQueryVariables] = useState<TripQueryVariables | undefined>();
  const [tripQueryResult, callback] = useTripQuery(tripQueryVariables);

  return (
    <div className="app">
      <NavBarContainer />
      <SearchBarContainer onRoute={callback} tripQueryVariables={tripQueryVariables} />
      <Stack direction="horizontal" gap={0}>
        <ItineraryListContainer tripQueryResult={tripQueryResult} />
        <MapView
          tripQueryResult={tripQueryResult}
          tripQueryVariables={tripQueryVariables}
          setTripQueryVariables={setTripQueryVariables}
        />
      </Stack>
      <DetailsViewContainer />
    </div>
  );
}
