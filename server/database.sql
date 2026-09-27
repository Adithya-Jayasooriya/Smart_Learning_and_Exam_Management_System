-- ============================================================================
-- Smart Learning (SLEMS) — MySQL schema for cPanel hosting
-- Import this file in cPanel ▸ phpMyAdmin ▸ (select your database) ▸ Import.
-- The tables mirror the app's on-device SQLite database (LocalDb.kt), so the
-- Android app and the web portal share the same structure.
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    email       VARCHAR(190)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,             -- bcrypt hash (or plain text for the seed rows below)
    role        ENUM('student','lecturer','admin') NOT NULL DEFAULT 'student',
    phone       VARCHAR(30)   NOT NULL DEFAULT '',
    active      TINYINT(1)    NOT NULL DEFAULT 1,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subjects (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(30)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    lecturer_id INT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subject_lecturer FOREIGN KEY (lecturer_id)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS materials (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    subject_id  INT           NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    type        VARCHAR(20)   NOT NULL DEFAULT 'PDF',   -- PDF / PPT / VIDEO / NOTE
    file_url    VARCHAR(500)  NOT NULL DEFAULT '',
    uploaded_by INT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_material_subject FOREIGN KEY (subject_id)
        REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exams (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    subject_id       INT          NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      VARCHAR(1000) NOT NULL DEFAULT '',
    exam_date        DATETIME NULL,
    deadline         DATETIME NULL,
    paper_url        VARCHAR(500) NOT NULL DEFAULT '',
    duration_minutes INT          NOT NULL DEFAULT 60,
    created_by       INT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_subject FOREIGN KEY (subject_id)
        REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submissions (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    exam_id    INT          NOT NULL,
    student_id INT          NOT NULL,
    file_url   VARCHAR(500) NOT NULL DEFAULT '',
    status     ENUM('submitted','graded') NOT NULL DEFAULT 'submitted',
    marks      INT NULL,
    graded_by  INT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_exam_student (exam_id, student_id),
    CONSTRAINT fk_submission_exam FOREIGN KEY (exam_id)
        REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_submission_student FOREIGN KEY (student_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL DEFAULT 0,     -- 0 = broadcast to everyone
    title      VARCHAR(190) NOT NULL,
    message    VARCHAR(1000) NOT NULL DEFAULT '',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- Seed accounts (same as the app's offline SQLite database).
-- Passwords are stored in plain text ONLY for these seed rows so you can log
-- in immediately; the API accepts both plain and hashed passwords, and every
-- account created through the app/portal is stored bcrypt-hashed.
-- Change these passwords after your first login!
-- ----------------------------------------------------------------------------
INSERT INTO users (name, email, password, role) VALUES
    ('System Admin',  'admin@slems.lk',    'admin123',    'admin'),
    ('Demo Lecturer', 'lecturer@slems.lk', 'lecturer123', 'lecturer'),
    ('Demo Student',  'student@slems.lk',  'student123',  'student');

INSERT INTO subjects (code, name, description, lecturer_id) VALUES
    ('IT101', 'Introduction to Programming',      '', 2),
    ('IT202', 'Mobile Application Development',   '', 2);
