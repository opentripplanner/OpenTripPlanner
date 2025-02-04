import React, { JSX, useEffect, useState } from 'react';
import { useTripSchema } from './useTripSchema.ts';
import { TripQueryVariables } from '../../gql/graphql';
import { getNestedValue, setNestedValue } from './nestedUtils';
import ArgumentTooltip from './ArgumentTooltip.tsx';
import { excludedArguments } from './excluded-arguments.ts';
import { ResolvedType } from './useTripArgs.ts';
import ResetButton from './ResetButton.tsx';
import { DefaultValue, extractAllArgs, formatArgumentName, ProcessedArgument } from './extractArgs.ts';

interface TripQueryArgumentsProps {
  tripQueryVariables: TripQueryVariables;
  setTripQueryVariables: (tripQueryVariables: TripQueryVariables) => void;
}

const TripQueryArguments: React.FC<TripQueryArgumentsProps> = ({ tripQueryVariables, setTripQueryVariables }) => {
  const [argumentsList, setArgumentsList] = useState<ProcessedArgument[]>([]);
  const [expandedArguments, setExpandedArguments] = useState<Record<string, boolean>>({});
  const [searchText] = useState('');

  const { tripArgs, loading, error } = useTripSchema();

  useEffect(() => {
    if (!tripArgs) return; // Don't run if the data isn't loaded yet
    if (loading || error) return; // Optionally handle error/loading

    const extractedArgs = extractAllArgs(tripArgs.trip.arguments);
    setArgumentsList(extractedArgs);
  }, [tripArgs, loading, error]);

  function normalizePathForList(path: string): string {
    // Replace numeric segments with `*`
    return path.replace(/\.\d+/g, '.*');
  }

  function handleInputChange(path: string, value: DefaultValue | undefined): void {
    const normalizedPath = normalizePathForList(path);
    const argumentConfig = argumentsList.find((arg) => arg.path === normalizedPath);

    if (!argumentConfig) {
      console.error(`No matching argumentConfig found for path: ${path}`);
      return;
    }

    // Handle comma-separated input for string arrays
    if (
      argumentConfig.type.subtype != null &&
      ['String', 'DoubleFunction', 'ID', 'Duration'].includes(argumentConfig.type.subtype) &&
      argumentConfig.isList
    ) {
      if (typeof value === 'string') {
        // Convert comma-separated string into an array
        const idsArray = value.split(',').map((id) => id.trim());

        let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, idsArray) as TripQueryVariables;
        updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
        setTripQueryVariables(updatedTripQueryVariables);
        return;
      }
    }

    // Default handling for other cases
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, value) as TripQueryVariables;
    updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
    setTripQueryVariables(updatedTripQueryVariables);
  }

  function isPlainObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }

  /**
   * Recursively removes empty arrays/objects from `variables` based on a path.
   * Returns the updated variables.
   */
  function cleanUpParentIfEmpty(variables: TripQueryVariables, path: string): TripQueryVariables {
    if (!path.includes('.')) {
      const topValue = getNestedValue(variables, path);

      if (Array.isArray(topValue) && topValue.length === 0) {
        // Create a shallow copy as a flexible object:
        const copy = { ...variables } as Record<string, unknown>;
        // Remove the property:
        delete copy[path];
        return copy as TripQueryVariables;
      }

      // If it's a plain object and all keys are undefined/null or empty, remove it
      if (isPlainObject(topValue)) {
        const allKeysEmpty = Object.keys(topValue).every((key) => {
          const childVal = (topValue as Record<string, unknown>)[key];
          return childVal === undefined || childVal === null || (Array.isArray(childVal) && childVal.length === 0);
        });

        if (allKeysEmpty) {
          const copy = { ...variables } as Record<string, unknown>;
          delete copy[path];
          return copy as TripQueryVariables;
        }
      }

      return variables; // Otherwise leave it as is
    }

    // For nested paths
    const pathParts = path.split('.');
    for (let i = pathParts.length - 1; i > 0; i--) {
      const parentPath = pathParts.slice(0, i).join('.');
      const parentValue = getNestedValue(variables, parentPath);

      if (parentValue == null) {
        // Already null or undefined, nothing to do
        continue;
      }

      if (Array.isArray(parentValue)) {
        // If the parent array is now empty, remove it
        if (parentValue.length === 0) {
          variables = setNestedValue(variables, parentPath, undefined) as TripQueryVariables;
        }
      } else if (isPlainObject(parentValue)) {
        // If all child values are null/undefined or empty, remove the parent
        const allKeysEmpty = Object.keys(parentValue).every((key) => {
          const childPath = `${parentPath}.${key}`;
          const childValue = getNestedValue(variables, childPath);
          return (
            childValue === undefined || childValue === null || (Array.isArray(childValue) && childValue.length === 0)
          );
        });

        if (allKeysEmpty) {
          variables = setNestedValue(variables, parentPath, undefined) as TripQueryVariables;
        }
      }
    }

    return variables;
  }

  function toggleExpand(path: string): void {
    setExpandedArguments((prev) => ({
      ...prev,
      [path]: !prev[path],
    }));
  }

  const filteredArgumentsList = argumentsList
    .filter(({ path }) => formatArgumentName(path).toLowerCase().includes(searchText.toLowerCase()))
    .filter(({ path }) => !excludedArguments.has(path));

  /**
   * Renders multiple InputObjects within an array. Each item in the array
   * is shown with an expand/collapse toggle and a remove button.
   */
  function renderListOfInputObjects(
    listPath: string,
    allArgs: ProcessedArgument[],
    level: number,
    type: ResolvedType,
  ): React.JSX.Element {
    // We assume getNestedValue returns unknown; cast to an array if needed
    const arrayVal = (getNestedValue(tripQueryVariables, listPath) ?? []) as unknown[];

    // You can customize this if you have a better naming scheme
    const typeName = type.name;

    return (
      <div>
        {arrayVal.map((_, index) => {
          const itemPath = `${listPath}.${index}`;

          // Replace the `.*` placeholder with the actual index
          const itemNestedArgs = allArgs
            .filter((arg) => arg.path.startsWith(`${listPath}.*.`) && arg.path !== `${listPath}.*`)
            .map((arg) => ({
              ...arg,
              path: arg.path.replace(`${listPath}.*`, itemPath),
            }));

          const immediateNestedArgs = itemNestedArgs.filter(
            (arg) => arg.path.split('.').length === itemPath.split('.').length + 1,
          );

          const isExpandedItem = expandedArguments[itemPath];

          return (
            <div key={itemPath} style={{ marginLeft: 15 }}>
              <span style={{ cursor: 'pointer' }} onClick={() => toggleExpand(itemPath)}>
                {isExpandedItem ? '▼ ' : '▶ '} [#{index + 1}]
              </span>
              <button style={{ marginLeft: 10 }} onClick={() => handleRemoveItem(listPath, index)}>
                Remove
              </button>

              {isExpandedItem && (
                <div style={{ marginLeft: 20 }}>
                  {renderArgumentInputs(immediateNestedArgs, level + 1, itemNestedArgs)}
                </div>
              )}
            </div>
          );
        })}
        <button onClick={() => handleAddItem(listPath)}>+ Add {typeName}</button>
      </div>
    );
  }

  function handleAddItem(listPath: string): void {
    const currentValue = (getNestedValue(tripQueryVariables, listPath) ?? []) as unknown[];
    const newValue = [...currentValue, {}];
    const updatedTripQueryVariables = setNestedValue(tripQueryVariables, listPath, newValue) as TripQueryVariables;
    setTripQueryVariables(updatedTripQueryVariables);
  }

  function handleRemoveItem(listPath: string, index: number): void {
    const currentValue = (getNestedValue(tripQueryVariables, listPath) ?? []) as unknown[];
    const newValue = currentValue.filter((_, i) => i !== index);
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, listPath, newValue) as TripQueryVariables;
    updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, listPath);
    setTripQueryVariables(updatedTripQueryVariables);
  }

  function handleRemoveArgument(path: string): void {
    let updatedTripQueryVariables = setNestedValue(tripQueryVariables, path, undefined) as TripQueryVariables;
    updatedTripQueryVariables = cleanUpParentIfEmpty(updatedTripQueryVariables, path);
    setTripQueryVariables(updatedTripQueryVariables);
  }

  function renderArgumentInputs(args: ProcessedArgument[], level: number, allArgs: ProcessedArgument[]): JSX.Element[] {
    return args.map(({ path, type, defaultValue, enumValues, isComplex, isList }) => {
      const isExpanded = expandedArguments[path];
      const currentDepth = path.split('.').length;
      const nestedArgs = allArgs.filter((arg) => {
        const argDepth = arg.path.split('.').length;
        return arg.path.startsWith(`${path}.`) && arg.path !== path && argDepth === currentDepth + 1;
      });

      const nestedLevel = level + 1;

      // Various input renderings depending on subtype
      return (
        <div key={path} style={{ marginLeft: `${level * 15}px`, marginBottom: '10px' }}>
          {isComplex ? (
            <div>
              <span style={{ cursor: 'pointer' }} onClick={() => toggleExpand(path)}>
                {isExpanded ? '▼ ' : '▶ '} {formatArgumentName(path)}
              </span>
              {isExpanded && isList ? (
                <div style={{ marginLeft: 20 }}>{renderListOfInputObjects(path, allArgs, nestedLevel, type)}</div>
              ) : isExpanded ? (
                renderArgumentInputs(nestedArgs, nestedLevel, allArgs)
              ) : null}
            </div>
          ) : (
            <div>
              <label className="argument-label" htmlFor={path}>
                {formatArgumentName(path)} <ArgumentTooltip defaultValue={defaultValue} type={type} />
              </label>
              {type.subtype === 'Boolean' &&
                (() => {
                  const currentValue = getNestedValue(tripQueryVariables, path) as boolean | undefined;
                  const isInUse = currentValue !== undefined;
                  return (
                    <span>
                      <input
                        type="checkbox"
                        id={path}
                        checked={currentValue ?? false}
                        onChange={(e) => handleInputChange(path, e.target.checked)}
                      />
                      {isInUse && (
                        <a onClick={() => handleRemoveArgument(path)} className="remove-argument">
                          x
                        </a>
                      )}
                    </span>
                  );
                })()}

              {type.subtype != null &&
                ['String', 'DoubleFunction', 'ID', 'Duration'].includes(type.subtype) &&
                isList && (
                  <input
                    className="comma-separated-input"
                    type="text"
                    id={path}
                    value={(() => {
                      const currentValue = getNestedValue(tripQueryVariables, path);
                      return Array.isArray(currentValue) ? currentValue.join(', ') : '';
                    })()}
                    onChange={(e) => handleInputChange(path, e.target.value)}
                    placeholder="Comma-separated list"
                  />
                )}

              {type.subtype != null &&
                ['String', 'DoubleFunction', 'ID', 'Duration'].includes(type.subtype) &&
                !isList && (
                  <input
                    type="text"
                    id={path}
                    value={(getNestedValue(tripQueryVariables, path) as string) ?? ''}
                    onChange={(e) => handleInputChange(path, e.target.value || undefined)}
                  />
                )}

              {type.subtype === 'Int' && (
                <input
                  type="number"
                  id={path}
                  value={(getNestedValue(tripQueryVariables, path) as number) ?? ''}
                  onChange={(e) => {
                    const val = parseInt(e.target.value, 10);
                    handleInputChange(path, Number.isNaN(val) ? undefined : val);
                  }}
                />
              )}

              {type.subtype === 'Float' && (
                <input
                  type="number"
                  id={path}
                  step="any"
                  value={(getNestedValue(tripQueryVariables, path) as number) ?? ''}
                  onChange={(e) => {
                    const val = parseFloat(e.target.value);
                    handleInputChange(path, Number.isNaN(val) ? undefined : val);
                  }}
                />
              )}

              {type.subtype === 'DateTime' && (
                <input
                  type="datetime-local"
                  id={path}
                  value={((getNestedValue(tripQueryVariables, path) as string) ?? '').slice(0, 16)}
                  onChange={(e) => {
                    const newValue = e.target.value ? new Date(e.target.value).toISOString() : undefined;
                    handleInputChange(path, newValue);
                  }}
                />
              )}

              {type.type === 'Enum' && enumValues && isList && (
                <select
                  id={path}
                  multiple
                  value={(() => {
                    const currentValue = getNestedValue(tripQueryVariables, path);
                    return Array.isArray(currentValue) ? currentValue : [];
                  })()}
                  onChange={(e) => {
                    const selectedOptions = Array.from(e.target.selectedOptions, (option) => option.value);
                    handleInputChange(path, selectedOptions);
                  }}
                >
                  {enumValues.map((enumValue) => (
                    <option key={enumValue} value={enumValue}>
                      {enumValue}
                    </option>
                  ))}
                </select>
              )}

              {type.type === 'Enum' && enumValues && !isList && (
                <select
                  id={path}
                  value={(getNestedValue(tripQueryVariables, path) as string) ?? 'Not selected'}
                  onChange={(e) => {
                    handleInputChange(path, e.target.value || undefined);
                  }}
                >
                  {enumValues.map((enumValue) => (
                    <option key={enumValue} value={enumValue}>
                      {enumValue}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}
        </div>
      );
    });
  }

  return (
    <div className="left-pane-container below-content">
      <div className="panel-header">
        Filters
        <ResetButton tripQueryVariables={tripQueryVariables} setTripQueryVariables={setTripQueryVariables} />
      </div>
      {filteredArgumentsList.length === 0 ? (
        <p>No arguments found.</p>
      ) : (
        <div className="argument-list">
          {renderArgumentInputs(
            // Top-level arguments have a path depth of 1
            filteredArgumentsList.filter((arg) => arg.path.split('.').length === 1),
            0,
            filteredArgumentsList,
          )}
        </div>
      )}
    </div>
  );
};

export default TripQueryArguments;
