-- Migration: thêm cột status vào bảng items
-- Chạy script này 1 lần trên database hiện tại

ALTER TABLE items ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';

-- Cập nhật các item cũ chưa có status về PENDING
UPDATE items SET status = 'PENDING' WHERE status IS NULL;
