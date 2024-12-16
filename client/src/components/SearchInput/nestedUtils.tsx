// src/utils/nestedUtils.ts

/**
 * Retrieves a nested value from an object based on a dot-separated path.
 * @param obj - The object to traverse.
 * @param path - The dot-separated path string.
 * @returns The value at the specified path or undefined if not found.
 */
export const getNestedValue = (obj: any, path: string): any => {
    return path.split('.').reduce((acc, key) => (acc && acc[key] !== undefined ? acc[key] : undefined), obj);
};

/**
 * Sets a nested value in an object based on a dot-separated path.
 * Returns a new object with the updated value, ensuring immutability.
 * @param obj - The original object.
 * @param path - The dot-separated path string.
 * @param value - The value to set.
 * @returns A new object with the updated value.
 */
export const setNestedValue = (obj: any, path: string, value: any): any => {
    const keys = path.split('.');
    const newObj = { ...obj };
    let current = newObj;
    for (let i = 0; i < keys.length - 1; i++) {
        const key = keys[i];
        current[key] = { ...current[key] };
        current = current[key];
    }
    current[keys[keys.length - 1]] = value;
    return newObj;
};
