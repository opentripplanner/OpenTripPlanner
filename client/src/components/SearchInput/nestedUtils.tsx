/**
 * Retrieves a nested value from an object based on a dot-separated path.
 * @param obj - The object/array to traverse.
 * @param path - The dot-separated path string (e.g. "myList.0.fieldName").
 * @returns The value at the specified path or undefined if not found.
 */
export const getNestedValue = (obj: any, path: string): any => {
  return path.split('.').reduce((acc, key) => {
    if (acc == null) return undefined;
    return acc[key];
  }, obj);
};

/**
 * Sets a nested value in an object (or array) based on a dot-separated path,
 * returning a new top-level object/array to ensure immutability.
 *
 * This version detects numeric path segments (like "0", "1") and uses arrays
 * at those levels. Non-numeric segments use objects. If there's a mismatch,
 * it will convert that level to the correct type.
 *
 * @param obj - The original object/array.
 * @param path - The dot-separated path string (e.g. "myList.0.fieldName").
 * @param value - The value to set at that path.
 * @returns A new object (or array) with the updated value.
 */
export const setNestedValue = (obj: any, path: string, value: any): any => {
  const keys = path.split('.');

  /**
   * Recursively traverse `obj` based on the path segments.
   * At each level, create a shallow clone of the array/object,
   * then update the next key.
   */
  function cloneAndSet(current: any, index: number): any {
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
      // If current is an object and key is non-numeric, place `value`
      if (!Array.isArray(current) && !isNumeric) {
        return { ...current, [key]: value };
      }
      // If there's a mismatch, create the correct type
      if (isNumeric) {
        // We expected an array, so create a new one
        const arr: any[] = Array.isArray(current) ? [...current] : [];
        arr[Number(key)] = value;
        return arr;
      } else {
        // We expected an object
        return { ...(Array.isArray(current) ? {} : current), [key]: value };
      }
    }

    // If we are *not* at the final segment, we need to recurse deeper.

    // Next level
    const nextKey = keys[index + 1];
    const nextIsNumeric = !isNaN(Number(nextKey));

    if (Array.isArray(current) && isNumeric) {
      // current is an array, and we have a numeric key
      const newArray = [...current];
      const childVal = current[Number(key)];
      newArray[Number(key)] = cloneAndSet(
          // pass existing childVal or fallback to correct type
          childVal !== undefined ? childVal : nextIsNumeric ? [] : {},
          index + 1
      );
      return newArray;

    } else if (!Array.isArray(current) && !isNumeric) {
      // current is an object, and we have a string key
      const newObj = { ...current };
      const childVal = current[key];
      newObj[key] = cloneAndSet(
          childVal !== undefined ? childVal : nextIsNumeric ? [] : {},
          index + 1
      );
      return newObj;

    } else {
      // There's a mismatch:
      // e.g. current is an object but key is numeric (so we want an array),
      // or current is an array but key is non-numeric (so we want an object).
      // We'll convert to the correct type at this level.
      if (isNumeric) {
        // create array
        const arr: any[] = [];
        arr[Number(key)] = cloneAndSet(
            nextIsNumeric ? [] : {},
            index + 1
        );
        return arr;
      } else {
        // create object
        return {
          [key]: cloneAndSet(nextIsNumeric ? [] : {}, index + 1),
        };
      }
    }
  }

  // If the root `obj` is undefined or null, let the root
  // be an object or array depending on the first key:
  if (obj == null) {
    const firstKeyIsNumeric = !isNaN(Number(keys[0]));
    obj = firstKeyIsNumeric ? [] : {};
  }

  return cloneAndSet(obj, 0);
};
