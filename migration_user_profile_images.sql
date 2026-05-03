-- Migration: Add profile picture and banner columns to users table
-- Date: 2026-05-03

ALTER TABLE users
ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS banner_url VARCHAR(500);
