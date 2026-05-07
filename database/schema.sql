CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE items (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    starting_price DOUBLE NOT NULL,
    seller_id VARCHAR(36),
    item_type VARCHAR(15) NOT NULL,
    warranty_months INT,
    artist_name VARCHAR(100),
    material VARCHAR(100),
    engine_number VARCHAR(50),
    manufacturing_year INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE auctions (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    current_price DOUBLE NOT NULL,
    highest_bidder_id VARCHAR(36),
    status VARCHAR(10) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
);

CREATE TABLE bid_transactions (
    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    bid_amount DOUBLE NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);
