-- ============================================================================
--  Smart Learning & Exam Management System
--  MySQL schema for cPanel hosting (import via phpMyAdmin)
--  Engine: InnoDB   Charset: utf8mb4
-- ============================================================================
--  HOW TO USE ON cPANEL
--  1. cPanel > MySQL Databases > create a database + user, add user to DB
--     (all privileges).  Note the FULL names, e.g.  myacct_slems
--  2. cPanel > phpMyAdmin > select that database > Import > choose this file.
--  3. Run /api/install.php once to create the default admin & lecturer logins.
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------- users ---
-- Single table for students, lecturers and admins (separated by `role`).
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    email       VARCHAR(160)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,                 -- bcrypt hash
    role        ENUM('student','lecturer','admin') NOT NULL DEFAULT 'student',
    phone       VARCHAR(30)   DEFAULT NULL,
    active      TINYINT(1)    NOT NULL DEFAULT 1,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------- profiles ---
-- Optional 1:1 extra info per user (student index no, department, avatar…).
CREATE TABLE IF NOT EXISTS profiles (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    reg_number  VARCHAR(40)  DEFAULT NULL,
    department  VARCHAR(120) DEFAULT NULL,
    avatar_url  VARCHAR(255) DEFAULT NULL,
    bio         TEXT         DEFAULT NULL,
    UNIQUE KEY uq_profile_user (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------- subjects ---
CREATE TABLE IF NOT EXISTS subjects (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(160) NOT NULL,
    description TEXT         DEFAULT NULL,
    lecturer_id INT          DEFAULT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subject_lecturer FOREIGN KEY (lecturer_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------- enrollments ---
-- Which students are enrolled in which subjects (many-to-many).
CREATE TABLE IF NOT EXISTS enrollments (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT NOT NULL,
    subject_id  INT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_enroll (student_id, subject_id),
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_enroll_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------- learning_materials ---
CREATE TABLE IF NOT EXISTS learning_materials (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    subject_id  INT NOT NULL,
    title       VARCHAR(200) NOT NULL,
    type        ENUM('PDF','PPT','VIDEO','NOTE') NOT NULL DEFAULT 'PDF',
    file_url    VARCHAR(500) NOT NULL,
    uploaded_by INT          DEFAULT NULL,
    approved    TINYINT(1)   NOT NULL DEFAULT 1,        -- admin approval flag
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mat_subject FOREIGN KEY (subject_id)  REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_mat_user    FOREIGN KEY (uploaded_by) REFERENCES users(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------- exams ---
CREATE TABLE IF NOT EXISTS exams (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    subject_id       INT NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT DEFAULT NULL,
    exam_date        DATETIME NOT NULL,
    deadline         DATETIME DEFAULT NULL,          -- answer-sheet submission deadline
    paper_url        VARCHAR(500) DEFAULT NULL,       -- uploaded exam paper
    duration_minutes INT DEFAULT 60,
    created_by       INT DEFAULT NULL,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_user    FOREIGN KEY (created_by) REFERENCES users(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------- exam_submissions ---
-- Scanned answer sheets uploaded by students from the mobile app.
CREATE TABLE IF NOT EXISTS exam_submissions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    exam_id      INT NOT NULL,
    student_id   INT NOT NULL,
    file_url     VARCHAR(500) NOT NULL,
    status       ENUM('submitted','graded') NOT NULL DEFAULT 'submitted',
    marks        INT DEFAULT NULL,
    graded_by    INT DEFAULT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_exam_student (exam_id, student_id),
    CONSTRAINT fk_sub_exam    FOREIGN KEY (exam_id)    REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_grader  FOREIGN KEY (graded_by)  REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------- assignments ---
CREATE TABLE IF NOT EXISTS assignments (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    subject_id  INT NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT DEFAULT NULL,
    due_date    DATETIME NOT NULL,
    created_by  INT DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_asg_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_asg_user    FOREIGN KEY (created_by) REFERENCES users(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------- assignment_submissions ---
CREATE TABLE IF NOT EXISTS assignment_submissions (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    assignment_id INT NOT NULL,
    student_id    INT NOT NULL,
    file_url      VARCHAR(500) NOT NULL,
    status        ENUM('submitted','graded') NOT NULL DEFAULT 'submitted',
    marks         INT DEFAULT NULL,
    submitted_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_asg_student (assignment_id, student_id),
    CONSTRAINT fk_asgsub_asg     FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_asgsub_student FOREIGN KEY (student_id)    REFERENCES users(id)        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------------- results ---
CREATE TABLE IF NOT EXISTS results (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    student_id  INT NOT NULL,
    subject_id  INT NOT NULL,
    exam_id     INT DEFAULT NULL,
    marks       INT NOT NULL,
    grade       VARCHAR(5) DEFAULT NULL,
    published   TINYINT(1) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_student FOREIGN KEY (student_id) REFERENCES users(id)    ON DELETE CASCADE,
    CONSTRAINT fk_res_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_exam    FOREIGN KEY (exam_id)    REFERENCES exams(id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------- notifications ---
-- user_id = 0  means a broadcast to everyone.
CREATE TABLE IF NOT EXISTS notifications (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL DEFAULT 0,
    title       VARCHAR(160) NOT NULL,
    message     TEXT NOT NULL,
    is_read     TINYINT(1) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------- demo data ---
-- Default users are created by /api/install.php (so passwords are hashed).
INSERT INTO subjects (code, name, description, lecturer_id) VALUES
    ('CS101', 'Introduction to Computing', 'Foundation computing concepts', NULL),
    ('CS102', 'Programming Fundamentals',  'Basics of programming',         NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO notifications (user_id, title, message) VALUES
    (0, 'Welcome', 'Welcome to the Smart Learning & Exam Management System!');
