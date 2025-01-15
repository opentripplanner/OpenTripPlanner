const endpoint = import.meta.env.VITE_SCHEMA_URL;

export const getSchemaUrl = () => {
    return endpoint;
};
