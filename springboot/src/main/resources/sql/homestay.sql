CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(255),
    enabled BIT NOT NULL,
    blacklisted BIT NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    title VARCHAR(100) NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    link_url VARCHAR(255),
    sort_order INT NOT NULL,
    enabled BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    title VARCHAR(100) NOT NULL,
    content LONGTEXT,
    published BIT NOT NULL
);

CREATE TABLE IF NOT EXISTS homestay (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    host_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(255),
    address VARCHAR(255) NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    total_rooms INT NOT NULL,
    house_type VARCHAR(50) NOT NULL,
    tags VARCHAR(255) NOT NULL,
    facilities VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    favorite_count INT NOT NULL,
    booking_count INT NOT NULL,
    rating DOUBLE NOT NULL,
    recommended BIT NOT NULL,
    latest_listed BIT NOT NULL,
    cover_image VARCHAR(500) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_homestay_host FOREIGN KEY (host_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS homestay_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    homestay_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_homestay_image_homestay FOREIGN KEY (homestay_id) REFERENCES homestay(id)
);

CREATE TABLE IF NOT EXISTS room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    homestay_id BIGINT NOT NULL,
    room_no VARCHAR(50) NOT NULL,
    room_type VARCHAR(50) NOT NULL,
    floor_no INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    bed_count INT NOT NULL,
    capacity INT NOT NULL,
    enabled BIT NOT NULL,
    CONSTRAINT fk_room_homestay FOREIGN KEY (homestay_id) REFERENCES homestay(id)
);

CREATE TABLE IF NOT EXISTS booking_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    homestay_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    nights INT NOT NULL,
    room_count INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    order_status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    contact_name VARCHAR(255),
    contact_phone VARCHAR(20),
    remark VARCHAR(500),
    CONSTRAINT fk_booking_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_booking_order_homestay FOREIGN KEY (homestay_id) REFERENCES homestay(id)
);

CREATE TABLE IF NOT EXISTS booking_order_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    order_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    room_no VARCHAR(50) NOT NULL,
    CONSTRAINT fk_booking_order_room_order FOREIGN KEY (order_id) REFERENCES booking_order(id),
    CONSTRAINT fk_booking_order_room_room FOREIGN KEY (room_id) REFERENCES room(id)
);

CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    homestay_id BIGINT NOT NULL,
    score INT NOT NULL,
    content LONGTEXT,
    image_urls VARCHAR(1000),
    reply_content VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES booking_order(id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_review_homestay FOREIGN KEY (homestay_id) REFERENCES homestay(id)
);

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    homestay_id BIGINT NOT NULL,
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_favorite_homestay FOREIGN KEY (homestay_id) REFERENCES homestay(id)
);

CREATE TABLE IF NOT EXISTS password_reset_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    username VARCHAR(50) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    new_password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewed_at DATETIME
);
