CREATE DATABASE IF NOT EXISTS supermarket_billing
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE supermarket_billing;

CREATE TABLE IF NOT EXISTS products (
    product_code VARCHAR(24) PRIMARY KEY,
    product_name VARCHAR(120) NOT NULL,
    category ENUM('REGULAR', 'PERISHABLE') NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    expiry_date DATE NULL,
    CONSTRAINT chk_products_price CHECK (unit_price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock >= 0),
    CONSTRAINT chk_products_expiry CHECK (
        category = 'PERISHABLE' OR expiry_date IS NULL
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_number VARCHAR(40) NOT NULL UNIQUE,
    customer_name VARCHAR(120) NOT NULL,
    created_at DATETIME NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    discount_total DECIMAL(12, 2) NOT NULL,
    total DECIMAL(12, 2) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bill_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bill_id BIGINT NOT NULL,
    product_code VARCHAR(24) NOT NULL,
    product_name VARCHAR(120) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    line_total DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_bill_items_bill FOREIGN KEY (bill_id)
        REFERENCES bills(id) ON DELETE CASCADE,
    CONSTRAINT chk_bill_items_quantity CHECK (quantity > 0)
) ENGINE=InnoDB;

-- Seed the demo catalog once. Re-running this script updates descriptions and
-- prices but preserves the current stock values for products already present.
INSERT INTO products (product_code, product_name, category, unit_price, stock, expiry_date)
VALUES
    ('100101', 'Basmati Rice 1 kg', 'REGULAR', 118.00, 42, NULL),
    ('100102', 'Wheat Flour 1 kg', 'REGULAR', 62.00, 28, NULL),
    ('100103', 'Tea 250 g', 'REGULAR', 145.00, 31, NULL),
    ('100104', 'Dishwash Liquid', 'REGULAR', 99.00, 2, NULL),
    ('100105', 'Pasta 500 g', 'REGULAR', 78.00, 18, NULL),
    ('200201', 'Fresh Milk 1 L', 'PERISHABLE', 68.00, 12, DATE_ADD(CURRENT_DATE(), INTERVAL 5 DAY)),
    ('200202', 'Whole Wheat Bread', 'PERISHABLE', 45.00, 5, DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY)),
    ('200203', 'Plain Yogurt 400 g', 'PERISHABLE', 72.00, 14, DATE_ADD(CURRENT_DATE(), INTERVAL 3 DAY)),
    ('200204', 'Apples 1 kg', 'PERISHABLE', 165.00, 9, DATE_ADD(CURRENT_DATE(), INTERVAL 10 DAY)),
    ('200205', 'Cheddar Cheese 200 g', 'PERISHABLE', 132.00, 4, DATE_ADD(CURRENT_DATE(), INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    category = VALUES(category),
    unit_price = VALUES(unit_price),
    expiry_date = VALUES(expiry_date);
