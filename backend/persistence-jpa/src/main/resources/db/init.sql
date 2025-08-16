-- LegisTrack Database Initialization Script
-- This file is used by PostgreSQL Docker container to initialize the database

-- Create the main database (this is done automatically by the POSTGRES_DB environment variable)
-- CREATE DATABASE legistrack;

-- Create any additional schemas or roles if needed
-- For now, we'll let Flyway handle the schema creation

-- This file can be empty as Flyway will handle all migrations
SELECT 'LegisTrack database initialization completed' AS message;
