USE supermarket_billing;

-- Run this once against an existing database created with the original schema.
-- Perishable products may now have an unknown/blank expiry date.
ALTER TABLE products
    DROP CHECK chk_products_expiry,
    ADD CONSTRAINT chk_products_expiry CHECK (
        category = 'PERISHABLE' OR expiry_date IS NULL
    );
