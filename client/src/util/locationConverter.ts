import { COORDINATE_PRECISION } from '../components/SearchBar/constants.ts';
import { Location } from '../gql/graphql.ts';

const DOUBLE_PATTERN = '-{0,1}\\d+(\\.\\d+){0,1}';

const LAT_LON_PATTERN = '(' + DOUBLE_PATTERN + ')(\\s*,\\s*|\\s+)(' + DOUBLE_PATTERN + ')';

const ID_SEPARATOR = ':';

export function parseLocation(value: string): Location | null {
  const latLonMatch = value.match(LAT_LON_PATTERN);

  if (latLonMatch) {
    return {
      coordinates: {
        latitude: +latLonMatch[1],
        longitude: +latLonMatch[4],
      },
    };
  }

  if (validFeedScopeIdString(value)) {
    return {
      place: value,
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
