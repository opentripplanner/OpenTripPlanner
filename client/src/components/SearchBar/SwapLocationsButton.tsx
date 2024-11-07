import { TripQueryVariables } from '../../gql/graphql.ts';
import icon from '../../static/img/swap.svg';

export function SwapLocationsButton({
  tripQueryVariables,
  setTripQueryVariables,
}: {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}) {
  const swapFromTo = () => {
    setTripQueryVariables({
      ...tripQueryVariables,
      from: tripQueryVariables.to,
      to: tripQueryVariables.from,
    });
  };

  return (
    <button className="swap-from-to" onClick={swapFromTo} title="Swap from/to">
      <img alt="Swap from/to" src={icon} />
    </button>
  );
}
