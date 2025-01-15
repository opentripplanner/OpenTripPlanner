export type NestedData = null | undefined | string | number | boolean | NestedData[] | { [key: string]: NestedData };

/**
 * Retrieves a nested value from an object/array based on a dot-separated path.
 * Supports wildcard (`*`) in paths to match any index in arrays.
 *
 * @param obj - The object/array to traverse.
 * @param path - The dot-separated path string (e.g. "myList.*.fieldName").
 * @returns The value at the specified path or undefined if not found.
 */
export function getNestedValue(obj: NestedData, path: string): NestedData {
  const keys = path.split('.');

  return keys.reduce<NestedData>((acc, key) => {
    if (acc == null) {
      // If acc is null or undefined, no deeper value can be retrieved
      return undefined;
    }

    if (key === '*') {
      // If wildcard, return all matching values from the array
      if (Array.isArray(acc)) {
        // We map over each item in the array, recursing on the "remaining" path
        return acc
          .map(
            (item) => getNestedValue(item, keys.slice(1).join('.')), // skip the current wildcard
          )
          .filter((val) => val !== undefined);
      }
      // Wildcard on non-array is invalid => undefined
      return undefined;
    }

    // Non-wildcard key:
    if (Array.isArray(acc)) {
      // If current data is an array, try to interpret `key` as an index
      const index = Number(key);
      if (!Number.isNaN(index)) {
        return acc[index];
      }
      // If the path key isn't a valid index, return undefined
      return undefined;
    }

    if (typeof acc === 'object') {
      // If it's a plain object, we can index by key
      return (acc as { [k: string]: NestedData })[key];
    }

    // If it's a primitive (string, number, boolean), there's nothing left to traverse
    return undefined;
  }, obj);
}

/**
 * Sets a nested value in an object (or array) based on a dot-separated path,
 * returning a new top-level object/array to ensure immutability.
 * Supports wildcard (`*`) in paths for updating all items in an array.
 *
 * @param obj - The original object/array.
 * @param path - The dot-separated path string (e.g. "myList.*.fieldName").
 * @param value - The value to set at that path.
 * @returns A new object (or array) with the updated value.
 */
export function setNestedValue(obj: NestedData, path: string, value: NestedData): NestedData {
  const keys = path.split('.');

  function cloneAndSet(current: NestedData, index: number): NestedData {
    const key = keys[index];
    const isLastSegment = index === keys.length - 1;

    // Wildcard logic
    if (key === '*') {
      if (!Array.isArray(current)) {
        // Wildcard used on non-array => just return current
        console.error(`Wildcard '*' used on non-array at path: ${keys.slice(0, index).join('.')}`);
        return current;
      }

      // If last segment is '*', we are setting the entire array, but that doesn't
      // quite make sense. Usually you'd do something like `myList.*.fieldName`.
      // We'll assume it means "set each item in the array to `value`" if last segment.
      if (isLastSegment) {
        return current.map(() => value);
      }

      // Otherwise, for each array item, recurse
      return current.map((item) => cloneAndSet(item, index + 1));
    }

    // For arrays, interpret `key` as an index, if it's numeric
    const numericKey = Number(key);
    const isNumericKey = !Number.isNaN(numericKey);

    if (isLastSegment) {
      // Base case: actually set the value here
      if (Array.isArray(current) && isNumericKey) {
        // Clone array, set index
        const newArray = [...current];
        newArray[numericKey] = value;
        return newArray;
      }
      if (typeof current === 'object' && !Array.isArray(current)) {
        // Clone object, set property
        return { ...current, [key]: value };
      }
      // If we're here, `current` might be a primitive or an array but `key` isn’t numeric
      // We must create a new array or object to attach the value
      if (isNumericKey) {
        const newArr: NestedData[] = Array.isArray(current) ? [...current] : [];
        newArr[numericKey] = value;
        return newArr;
      } else {
        // Create a new object if it isn't an object
        if (Array.isArray(current)) {
          // If "current" is an array, fallback to a new object.
          return { [key]: value };
        } else if (typeof current === 'object' && current !== null) {
          // If "current" is a non-null object, safely spread it.
          return { ...current as object, [key]: value };
        } else {
          // Otherwise (primitive or null), fallback to a new object.
          return { [key]: value };
        }
      }
    }

    // Recursive case: set deeper paths
    let child: NestedData;
    if (Array.isArray(current) && isNumericKey) {
      // If current is an array and key is numeric => we set that index
      const newArray = [...current];
      child = current[numericKey];
      newArray[numericKey] = cloneAndSet(child !== undefined ? child : /* create child if missing */ {}, index + 1);
      return newArray;
    } else if (typeof current === 'object' && !Array.isArray(current) && !isNumericKey && current !== null) {
      const newObj = { ...current };
      child = current[key];
      newObj[key] = cloneAndSet(child !== undefined ? child : /* create child if missing */ {}, index + 1);
      return newObj;
    } else {
      // If we got here, `current` might be a primitive or array with wrong key type, etc.
      // We must create something new to continue.
      if (isNumericKey) {
        const newArr: NestedData[] = [];
        newArr[numericKey] = cloneAndSet({}, index + 1);
        return newArr;
      } else {
        return {
          [key]: cloneAndSet({}, index + 1),
        };
      }
    }
  }

  // Ensure `obj` is initialized to an array or object if it's null/undefined
  if (obj == null) {
    const firstKeyIsNumeric = !Number.isNaN(Number(keys[0]));
    // If the first key is numeric, we start with an array; otherwise, an object
    obj = firstKeyIsNumeric ? [] : {};
  }

  return cloneAndSet(obj, 0);
}
