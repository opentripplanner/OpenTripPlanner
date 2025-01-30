/**
 * Retrieves a nested value from an object or array based on a dot-separated path.
 * @param obj  - The object/array to traverse (can be anything).
 * @param path - The dot-separated path string (e.g. "myList.0.fieldName").
 * @returns The value at the specified path or undefined if not found.
 */
export function getNestedValue(obj: unknown, path: string): unknown {
  return path.split('.').reduce<unknown>((acc, key) => {
    if (acc == null) {
      return undefined;
    }

    if (Array.isArray(acc)) {
      // If the current accumulator is an array, parse key as a numeric index
      const idx = Number(key);
      if (Number.isNaN(idx)) return undefined; // mismatch (path wanted array index but got non-numeric)
      return acc[idx];
    } else if (typeof acc === 'object') {
      // treat it like a dictionary
      const record = acc as Record<string, unknown>;
      return record[key];
    }
    // If acc is neither object nor array, we can't go deeper
    return undefined;
  }, obj);
}

/**
 * Sets a nested value in an object (or array) based on a dot-separated path,
 * returning a new top-level object/array to ensure immutability.
 *
 * This version detects numeric path segments (like "0", "1") and uses arrays
 * at those levels. Non-numeric segments use objects. If there's a mismatch,
 * it will convert that level to the correct type.
 *
 * @param obj   - The original object/array (could be anything).
 * @param path  - The dot-separated path (e.g. "myList.0.fieldName").
 * @param value - The value to set at that path.
 * @returns A new object or array with the updated value.
 */
export function setNestedValue(obj: unknown, path: string, value: unknown): unknown {
  const keys = path.split('.');

  /**
   * Recursively traverse `current` based on the path segments.
   * At each level, create a shallow clone of the array/object
   * and update the correct child.
   */
  function cloneAndSet(current: unknown, index: number): unknown {
    const key = keys[index];
    const isNumeric = !isNaN(Number(key));

    // Base case: if we're at the final segment, just return `value`.
    if (index === keys.length - 1) {
      // If current is an array and key is numeric, place `value` at that index
      if (Array.isArray(current) && isNumeric) {
        const newArray = [...current];
        newArray[Number(key)] = value;
        return newArray;
      }
      // If current is an object (Record) and key is non-numeric, place `value` in that object
      if (isObject(current) && !isNumeric) {
        return { ...current, [key]: value };
      }
      // Otherwise there's a type mismatch, so we convert:
      if (isNumeric) {
        // We expected an array
        const arr = Array.isArray(current) ? [...current] : [];
        arr[Number(key)] = value;
        return arr;
      } else {
        // We expected an object
        const base = isObject(current) ? current : {};
        return {
          ...base,
          [key]: value,
        };
      }
    }

    // Not at the final segment => recurse deeper
    const nextIndex = index + 1;
    const nextKey = keys[nextIndex];
    const nextIsNumeric = !isNaN(Number(nextKey));

    if (Array.isArray(current) && isNumeric) {
      // current is an array, and we have a numeric key
      const newArray = [...current];
      const childVal = current[Number(key)];
      newArray[Number(key)] = cloneAndSet(childVal !== undefined ? childVal : nextIsNumeric ? [] : {}, nextIndex);
      return newArray;
    } else if (isObject(current) && !isNumeric) {
      // current is an object (Record), and we have a string key
      const newObj = { ...current };
      const childVal = (current as Record<string, unknown>)[key];
      newObj[key] = cloneAndSet(childVal !== undefined ? childVal : nextIsNumeric ? [] : {}, nextIndex);
      return newObj;
    } else {
      // There's a mismatch at this level
      // e.g. current is an object but key is numeric => we want an array, or vice versa.
      if (isNumeric) {
        // create a new array at this level
        const arr: unknown[] = [];
        arr[Number(key)] = cloneAndSet(nextIsNumeric ? [] : {}, nextIndex);
        return arr;
      } else {
        // create a new object at this level
        return {
          [key]: cloneAndSet(nextIsNumeric ? [] : {}, nextIndex),
        };
      }
    }
  }

  // If the root `obj` is undefined or null, base it on the first key
  if (obj == null) {
    const firstKeyIsNumeric = !isNaN(Number(keys[0]));
    obj = firstKeyIsNumeric ? [] : {};
  }

  return cloneAndSet(obj, 0);
}

/**
 * A small helper type-guard to check if `value` is a non-null object (but not an array).
 */
function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
