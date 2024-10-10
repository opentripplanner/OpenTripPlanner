import { COORDINATE_PRECISION } from '../components/SearchBar/constants.ts';
import { Location } from '../gql/graphql.ts';

const DOUBLE_PATTERN = '-{0,1}\\d+(\\.\\d+){0,1}';

const LAT_LON_PATTERN = '(' + DOUBLE_PATTERN + ')(\\s*,\\s*|\\s+)(' + DOUBLE_PATTERN + ')';

const ID_SEPARATOR = ':';

const NAME_SEPARATOR = '::';

export function parseLocation(value: string): Location | null {
  let name: string | undefined = undefined;
  let place = value;

  if (value.indexOf(NAME_SEPARATOR) >= 0) {
    const parts = value.split(NAME_SEPARATOR, 2);
    name = parts[0];
    place = parts[1];
  }

  const latLonMatch = place.match(LAT_LON_PATTERN);

  if (latLonMatch) {
    return {
      name,
      coordinates: {
        latitude: +latLonMatch[1],
        longitude: +latLonMatch[4],
      },
    };
  }

  if (validFeedScopeIdString(place)) {
    return {
      name,
      place,
    };
  }

  return null;
}

function validFeedScopeIdString(value: string): boolean {
  return value.indexOf(ID_SEPARATOR) > -1;
}

export function toString(location: Location): string | null {
  if (location.coordinates) {
    return `${location.coordinates?.latitude.toPrecision(
      COORDINATE_PRECISION,
    )} ${location.coordinates?.longitude.toPrecision(COORDINATE_PRECISION)}`;
  }

  if (location.place) {
    return location.place;
  }

  return null;
}
