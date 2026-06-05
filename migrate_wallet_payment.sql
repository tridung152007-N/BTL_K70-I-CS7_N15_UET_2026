-- Migration: wallet/payment support
-- Run once on the existing MySQL database.

ALTER TABLE auctions ADD COLUMN IF NOT EXISTS is_paid BOOLEAN DEFAULT FALSE;

UPDATE auctions SET is_paid = FALSE WHERE is_paid IS NULL;
