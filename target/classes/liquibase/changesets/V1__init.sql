-- Create table business_user_info
CREATE SCHEMA IF NOT EXISTS telebot;


CREATE TABLE IF NOT EXISTS business_user_info (
                                                  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                                  username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    chat_id BIGINT,
    number VARCHAR(255),
    bot_state VARCHAR(255)
    );

-- Create table business_order
CREATE TABLE IF NOT EXISTS business_order (
                                              id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                              description VARCHAR(255),
    sum VARCHAR(255),
    deadline VARCHAR(255),
    business_user_info_id BIGINT,
    FOREIGN KEY (business_user_info_id) REFERENCES business_user_info(id)
    );

-- Create table user_info
CREATE TABLE IF NOT EXISTS user_info (
                                         id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                         username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    chat_id BIGINT,
    bot_state VARCHAR(255)
    );

-- Create table user_profile
CREATE TABLE IF NOT EXISTS user_profile (
                                            id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                                            name VARCHAR(255),
    number VARCHAR(255),
    specialization VARCHAR(255),
    description VARCHAR(255),
    projects VARCHAR(255),
    money VARCHAR(255),
    user_info_id BIGINT,
    FOREIGN KEY (user_info_id) REFERENCES user_info(id)
    );
