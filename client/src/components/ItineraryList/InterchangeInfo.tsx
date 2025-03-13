import logo from '../../static/img/stay-seated.svg';
import { TripQuery } from '../../gql/graphql.ts';

type Leg = TripQuery['trip']['tripPatterns'][0]['legs'][0];

/**
 * Displays an icon if a leg has a stay-seated transfer from the previous one.
 */
export function InterchangeInfo({ leg }: { leg: Leg }) {
  if (leg.interchangeFrom?.staySeated) {
    return (
      <img
        alt="Stay-seated transfer"
        title="Stay-seated transfer"
        src={logo}
        width="20"
        height="20"
        className="d-inline-block align-middle"
      />
    );
  } else {
    return null;
  }
}
