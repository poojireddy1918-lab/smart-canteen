CREATE DATABASE IF NOT EXISTS smart_canteen;
USE smart_canteen;

CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    role          ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT'
);

CREATE TABLE IF NOT EXISTS food_items (
    food_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    price     DECIMAL(8,2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uq_food_name (name)
);

CREATE TABLE IF NOT EXISTS orders (
    order_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status       ENUM('PLACED','COMPLETED','CANCELLED','DELIVERED') NOT NULL DEFAULT 'PLACED',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT NOT NULL,
    food_id       INT NOT NULL,
    quantity      INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (food_id)  REFERENCES food_items(food_id)
);

CREATE TABLE IF NOT EXISTS tokens (
    token_id     INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT NOT NULL UNIQUE,
    token_number INT NOT NULL,
    token_date   DATE NOT NULL DEFAULT (CURRENT_DATE),
    status       ENUM('WAITING','SERVED','CANCELLED','DELIVERED') NOT NULL DEFAULT 'WAITING',
    UNIQUE KEY uq_day_token (token_date, token_number),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Sample menu (idempotent: safe to run multiple times)
INSERT IGNORE INTO food_items (name, price) VALUES
    ('Veg Sandwich', 40.00),
    ('Samosa', 15.00),
    ('Masala Dosa', 60.00),
    ('Chole Bhature', 70.00),
    ('Paneer Roll', 55.00),
    ('Tea', 10.00),
    ('Cold Coffee', 35.00);

-- Admin account (admin@canteen.com / admin123) is created automatically by the app on first start.
