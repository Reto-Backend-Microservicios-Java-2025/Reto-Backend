CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    product_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    balance NUMERIC(12, 2) NOT NULL
);
