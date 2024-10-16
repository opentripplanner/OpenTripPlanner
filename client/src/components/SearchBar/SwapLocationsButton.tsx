import { TripQueryVariables } from '../../gql/graphql.ts';
import swapImg from '../../static/img/swap.svg';

const HINT = 'Swap from/to';

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
    <button className="swap-from-to" onClick={swapFromTo} title={HINT}>
      <img alt={HINT} src={swapImg} />
    </button>
  );
}
