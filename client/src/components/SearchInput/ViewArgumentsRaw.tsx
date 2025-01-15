import React from 'react';
import {TripQueryVariables} from "../../gql/graphql.ts";

interface ViewArgumentsRawProps {
  tripQueryVariables: TripQueryVariables;
}

const ViewArgumentsRaw: React.FC<ViewArgumentsRawProps> = ({ tripQueryVariables }) => {

  return (
      <div className={'left-pane-container below-content'} style={{fontSize: "14px"}}>
        <div className="panel-header">
          Arguments raw
        </div>

        <pre>
            {JSON.stringify(tripQueryVariables, null, 2)}
          </pre>

      </div>
  );
};

export default ViewArgumentsRaw;
