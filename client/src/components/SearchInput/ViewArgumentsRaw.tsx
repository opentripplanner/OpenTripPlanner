import React from 'react';
import { TripQueryVariables } from '../../gql/graphql.ts';
import ResetButton from './ResetButton.tsx';

interface ViewArgumentsRawProps {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}

const ViewArgumentsRaw: React.FC<ViewArgumentsRawProps> = ({ tripQueryVariables, setTripQueryVariables }) => {
  return (
    <div className={'left-pane-container below-content'} style={{ fontSize: '14px' }}>
      <div className="panel-header">
        Arguments raw
        <ResetButton tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>

      <pre>{JSON.stringify(tripQueryVariables, null, 2)}</pre>
    </div>
  );
};

export default ViewArgumentsRaw;
