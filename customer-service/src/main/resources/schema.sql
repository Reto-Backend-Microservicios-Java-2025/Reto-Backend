CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(70) NOT NULL,
    full_last_name VARCHAR(70) NOT NULL,
    typedocument VARCHAR(20),
    document_number VARCHAR(20) NOT NULL,
    unique_code BIGINT NOT NULL
);
