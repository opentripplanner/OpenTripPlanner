import logo from '../../static/img/stay-seated.svg';
import { Leg } from '../../static/query/tripQueryTypes';

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
