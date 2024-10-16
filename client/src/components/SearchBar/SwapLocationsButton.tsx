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
  const onChange = () => {
    setTripQueryVariables({
      ...tripQueryVariables,
      from: tripQueryVariables.to,
      to: tripQueryVariables.from,
    });
  };

  return <img className="swap-from-to" alt={HINT} title={HINT} src={swapImg} onClick={onChange} />;
}
