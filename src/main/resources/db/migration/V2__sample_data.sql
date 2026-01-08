-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password, first_name, last_name, role, enabled)
VALUES ('suleman_admin', 'suleman_admin@test.com', '$2a$10$drUziYUerP7WeCNhopkZg.E7C4bX9vZUmbTbBom3TyCsLvr0Wbem2', 'suleman_admin', 'suleman_admin', 'ADMIN', true);

-- Insert sample users (password: user123)
INSERT INTO users (username, email, password, first_name, last_name, role, enabled)
VALUES ('suleman', 'suleman@test.com', '$2a$10$UNJpyn1.lbKUth7IsqnVc.itUFs0UtDFFXU8A4FkdRM5M5bEx8U8G', 'Regular', 'User', 'USER', true);

-- Insert premium user (password: premium123)
INSERT INTO users (username, email, password, first_name, last_name, role, enabled)
VALUES ('suleman_premium', 'sulemanpremium@test.com', '$2a$10$BWZF8VmJyUMQlTCHkqgVkeFjTQ41o5FKNqocUbO3gQ3bKmrti.ZB6', 'Premium', 'User', 'PREMIUM_USER', true);

-- Insert sample products
INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Mobile', 'Samsung smart mobile phone', 1000.99, 100, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Laptop Pro', 'High-performance laptop for professionals', 1299.99, 50, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 49.99, 200, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Mechanical Keyboard', 'RGB mechanical keyboard with Cherry MX switches', 149.99, 100, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('4K Monitor', '27-inch 4K UHD monitor with HDR support', 599.99, 30, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('USB-C Hub', 'Multi-port USB-C hub with HDMI and SD card reader', 79.99, 150, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Webcam HD', '1080p HD webcam with built-in microphone', 89.99, 80, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Noise Cancelling Headphones', 'Premium wireless headphones with ANC', 299.99, 60, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('External SSD 1TB', 'Portable SSD with USB 3.2 Gen 2', 129.99, 100, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Desk Lamp', 'LED desk lamp with adjustable brightness', 39.99, 120, false);

INSERT INTO products (name, description, price, quantity, deleted)
VALUES ('Laptop Stand', 'Adjustable aluminum laptop stand', 59.99, 90, false);

