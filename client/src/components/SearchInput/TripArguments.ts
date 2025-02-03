export interface TripArguments {
  trip: {
    arguments: {
      [key: string]: Argument;
    };
  };
}

export interface Argument {
  type: TypeDescriptor;
  defaultValue?: string;
}

export type TypeDescriptor = ScalarType | NestedObject;

export type ScalarType = 'ID' | 'String' | 'Int' | 'Float' | 'Boolean' | 'DateTime' | 'Duration';

export interface NestedObject {
  [key: string]: Argument | string[]; // Allows for nested objects or enum arrays
}
