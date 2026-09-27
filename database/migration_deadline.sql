-- ============================================================================
--  Migration: add exam paper + submission deadline to the exams table.
--  Run this ONLY if you already imported an older schema.sql (without these
--  columns). Fresh imports of schema.sql already include them.
--  phpMyAdmin > select your DB > SQL tab > paste > Go.
-- ============================================================================

ALTER TABLE exams
    ADD COLUMN deadline  DATETIME     DEFAULT NULL AFTER exam_date,
    ADD COLUMN paper_url VARCHAR(500) DEFAULT NULL AFTER deadline;
