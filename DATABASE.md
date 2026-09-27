# Database Structure — Smart Learning & Exam Management System

**Engine:** MySQL (InnoDB) · **Charset:** `utf8mb4` · **Host:** cPanel (phpMyAdmin)

The system uses **one shared MySQL database** on cPanel. Both clients talk to it
through the PHP REST API in `/api`:

```
  Android app  ─┐
                ├──►  PHP REST API (/api)  ──►  MySQL (cPanel)
  Lecturer web ─┘
```

> Import `database/schema.sql` via phpMyAdmin, then run `/api/install.php` once to
> create the default admin & lecturer accounts (passwords are bcrypt-hashed).

---

## Entity Relationship Overview

```
users (1) ───< subjects (lecturer_id)
users (1) ───< enrollments >─── (1) subjects
subjects (1) ───< learning_materials
subjects (1) ───< exams ───< exam_submissions >─── (1) users (student)
subjects (1) ───< assignments ───< assignment_submissions >─── (1) users
users (1) ───< results >─── (1) subjects
users (1) ───< notifications
users (1) ─── (1) profiles
```

---

## Tables

### 1. `users`
All accounts (students, lecturers, admins) in one table, separated by `role`.

| Column     | Type                                   | Notes                          |
|------------|----------------------------------------|--------------------------------|
| id         | INT PK AI                              | Primary key                    |
| name       | VARCHAR(120)                           | Full name                      |
| email      | VARCHAR(160) UNIQUE                     | Login id                       |
| password   | VARCHAR(255)                           | **bcrypt** hash                |
| role       | ENUM('student','lecturer','admin')     | Default `student`              |
| phone      | VARCHAR(30) NULL                       |                                |
| active     | TINYINT(1)                             | 1 = active, 0 = deactivated    |
| created_at | TIMESTAMP                              | Auto                           |

### 2. `profiles`  (1:1 with `users`)
| Column     | Type          | Notes                              |
|------------|---------------|------------------------------------|
| id         | INT PK AI     |                                    |
| user_id    | INT FK → users| `ON DELETE CASCADE`, UNIQUE        |
| reg_number | VARCHAR(40)   | Student index / registration no.   |
| department | VARCHAR(120)  |                                    |
| avatar_url | VARCHAR(255)  |                                    |
| bio        | TEXT          |                                    |

### 3. `subjects`
| Column      | Type           | Notes                          |
|-------------|----------------|--------------------------------|
| id          | INT PK AI      |                                |
| code        | VARCHAR(20) UNIQUE | e.g. `CS101`               |
| name        | VARCHAR(160)   |                                |
| description | TEXT NULL      |                                |
| lecturer_id | INT FK → users | Assigned lecturer, `SET NULL`  |
| created_at  | TIMESTAMP      |                                |

### 4. `enrollments`  (students ↔ subjects, many-to-many)
| Column      | Type              | Notes                              |
|-------------|-------------------|------------------------------------|
| id          | INT PK AI         |                                    |
| student_id  | INT FK → users    | `CASCADE`                          |
| subject_id  | INT FK → subjects | `CASCADE`                          |
| enrolled_at | TIMESTAMP         | UNIQUE(student_id, subject_id)     |

### 5. `learning_materials`
| Column      | Type                              | Notes                       |
|-------------|-----------------------------------|-----------------------------|
| id          | INT PK AI                         |                             |
| subject_id  | INT FK → subjects                 | `CASCADE`                   |
| title       | VARCHAR(200)                      |                             |
| type        | ENUM('PDF','PPT','VIDEO','NOTE')  | Material kind               |
| file_url    | VARCHAR(500)                      | File link or video URL      |
| uploaded_by | INT FK → users                    | `SET NULL`                  |
| approved    | TINYINT(1)                        | Admin approval (1 default)  |
| created_at  | TIMESTAMP                         |                             |

### 6. `exams`
| Column           | Type              | Notes                                  |
|------------------|-------------------|----------------------------------------|
| id               | INT PK AI         |                                        |
| subject_id       | INT FK → subjects | `CASCADE`                              |
| title            | VARCHAR(200)      |                                        |
| description      | TEXT NULL         |                                        |
| exam_date        | DATETIME          | When the exam is held                  |
| deadline         | DATETIME NULL     | Answer-sheet **submission deadline**   |
| paper_url        | VARCHAR(500) NULL | Uploaded **exam paper** (PDF/link)     |
| duration_minutes | INT               | Default 60                             |
| created_by       | INT FK → users    | `SET NULL`                             |
| created_at       | TIMESTAMP         |                                        |

> The submission deadline drives student reminders (Android **Notifications**)
> and the colour-coded **Overdue / Due-soon** tags in the lecturer web portal.
> `submissions.php` rejects uploads received after `deadline`.

### 7. `exam_submissions`  (scanned answer sheets)
| Column       | Type                          | Notes                            |
|--------------|-------------------------------|----------------------------------|
| id           | INT PK AI                     |                                  |
| exam_id      | INT FK → exams                | `CASCADE`                        |
| student_id   | INT FK → users               | `CASCADE`                        |
| file_url     | VARCHAR(500)                  | Uploaded scan                    |
| status       | ENUM('submitted','graded')    | Default `submitted`              |
| marks        | INT NULL                      | 0–100                            |
| graded_by    | INT FK → users               | Lecturer, `SET NULL`             |
| submitted_at | TIMESTAMP                     | UNIQUE(exam_id, student_id)      |

### 8. `assignments`
| Column      | Type              | Notes      |
|-------------|-------------------|------------|
| id          | INT PK AI         |            |
| subject_id  | INT FK → subjects | `CASCADE`  |
| title       | VARCHAR(200)      |            |
| description | TEXT NULL         |            |
| due_date    | DATETIME          |            |
| created_by  | INT FK → users    | `SET NULL` |
| created_at  | TIMESTAMP         |            |

### 9. `assignment_submissions`
| Column        | Type                       | Notes                                |
|---------------|----------------------------|--------------------------------------|
| id            | INT PK AI                  |                                      |
| assignment_id | INT FK → assignments       | `CASCADE`                            |
| student_id    | INT FK → users            | `CASCADE`                            |
| file_url      | VARCHAR(500)               |                                      |
| status        | ENUM('submitted','graded') |                                      |
| marks         | INT NULL                   |                                      |
| submitted_at  | TIMESTAMP                  | UNIQUE(assignment_id, student_id)    |

### 10. `results`
| Column     | Type              | Notes                       |
|------------|-------------------|-----------------------------|
| id         | INT PK AI         |                             |
| student_id | INT FK → users    | `CASCADE`                   |
| subject_id | INT FK → subjects | `CASCADE`                   |
| exam_id    | INT FK → exams    | `SET NULL`                  |
| marks      | INT               |                             |
| grade      | VARCHAR(5)        | e.g. `A`, `B+`              |
| published  | TINYINT(1)        | 1 = visible to student      |
| created_at | TIMESTAMP         |                             |

### 11. `notifications`
| Column     | Type         | Notes                              |
|------------|--------------|------------------------------------|
| id         | INT PK AI    |                                    |
| user_id    | INT          | `0` = broadcast to everyone        |
| title      | VARCHAR(160) |                                    |
| message    | TEXT         |                                    |
| is_read    | TINYINT(1)   | 0 / 1                              |
| created_at | TIMESTAMP    |                                    |

---

## Feature → Table mapping

| Feature area                         | Tables used                                        |
|--------------------------------------|----------------------------------------------------|
| Registration / Login / Profile       | `users`, `profiles`                                |
| View subjects / enrolment            | `subjects`, `enrollments`                          |
| Learning materials (PDF/PPT/Video)   | `learning_materials`                               |
| Exams & schedule                     | `exams`                                            |
| Scan & upload answer sheets          | `exam_submissions`                                 |
| Grading & marks                      | `exam_submissions`, `results`                      |
| Assignments                          | `assignments`, `assignment_submissions`            |
| Results                              | `results`                                          |
| Notifications                        | `notifications`                                    |
| Admin user/subject/report management | `users`, `subjects`, all tables (counts)           |

---

## Default accounts (created by `/api/install.php`)

| Role     | Email                | Password  |
|----------|----------------------|-----------|
| Admin    | admin@slems.com      | admin123  |
| Lecturer | lecturer@slems.com   | lec123    |

> Change these immediately after first login on a real deployment.
