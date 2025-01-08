/**
 * Retrieves a nested value from an object based on a dot-separated path.
 * Supports wildcard (`*`) in paths to match any index in arrays.
 *
 * @param obj - The object/array to traverse.
 * @param path - The dot-separated path string (e.g. "myList.*.fieldName").
 * @returns The value at the specified path or undefined if not found.
 */
export const getNestedValue = (obj: any, path: string): any => {
  const keys = path.split('.');

  return keys.reduce((acc, key) => {
    if (acc == null) return undefined;

    if (key === '*') {
      // If wildcard, return all matching values from the array
      if (Array.isArray(acc)) {
        return acc.map((item) => getNestedValue(item, keys.slice(1).join('.'))).filter((val) => val !== undefined);
      }
      // Wildcard on non-array is invalid
      return undefined;
    }

    return acc[key];
  }, obj);
};

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
export const setNestedValue = (obj: any, path: string, value: any): any => {
  const keys = path.split('.');

  /**
   * Recursively traverse `obj` based on the path segments.
   */
  function cloneAndSet(current: any, index: number): any {
    const key = keys[index];
    const isNumeric = !isNaN(Number(key));

    // Handle wildcard (`*`) updates
    if (key === '*') {
      if (!Array.isArray(current)) {
        console.error(`Wildcard '*' used on non-array at path: ${keys.slice(0, index).join('.')}`);
        return current;
      }
      // Update all items in the array
      return current.map((item) => cloneAndSet(item, index + 1));
    }

    // Base case: if we're at the final segment, just return `value`.
    if (index === keys.length - 1) {
      if (Array.isArray(current) && isNumeric) {
        const newArray = [...current];
        newArray[Number(key)] = value;
        return newArray;
      }
      if (!Array.isArray(current) && !isNumeric) {
        return { ...current, [key]: value };
      }
      if (isNumeric) {
        const arr: any[] = Array.isArray(current) ? [...current] : [];
        arr[Number(key)] = value;
        return arr;
      } else {
        return { ...(Array.isArray(current) ? {} : current), [key]: value };
      }
    }

    // Recursively update the next level
    const nextKey = keys[index + 1];
    const nextIsNumeric = !isNaN(Number(nextKey));

    if (Array.isArray(current) && isNumeric) {
      const newArray = [...current];
      const childVal = current[Number(key)];
      newArray[Number(key)] = cloneAndSet(
          childVal !== undefined ? childVal : nextIsNumeric ? [] : {},
          index + 1,
      );
      return newArray;
    } else if (!Array.isArray(current) && !isNumeric) {
      const newObj = { ...current };
      const childVal = current[key];
      newObj[key] = cloneAndSet(
          childVal !== undefined ? childVal : nextIsNumeric ? [] : {},
          index + 1,
      );
      return newObj;
    } else {
      if (isNumeric) {
        const arr: any[] = [];
        arr[Number(key)] = cloneAndSet(nextIsNumeric ? [] : {}, index + 1);
        return arr;
      } else {
        return {
          [key]: cloneAndSet(nextIsNumeric ? [] : {}, index + 1),
        };
      }
    }
  }

  if (obj == null) {
    const firstKeyIsNumeric = !isNaN(Number(keys[0]));
    obj = firstKeyIsNumeric ? [] : {};
  }

  return cloneAndSet(obj, 0);
};
