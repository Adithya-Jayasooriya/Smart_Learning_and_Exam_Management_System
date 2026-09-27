# Smart Learning & Exam Management System

A digital platform for **students, lecturers and admins** to manage learning
materials and examinations, with an Android app, a lecturer web portal, and a
shared cPanel (PHP + MySQL) backend.

## Architecture
```
 ┌─────────────┐        ┌──────────────────┐        ┌──────────────────┐
 │ Android app │ ─────► │  PHP REST API    │ ─────► │  MySQL (cPanel)  │
 │ (Kotlin)    │  HTTP  │  /api/*.php      │  PDO   │  one database    │
 └─────────────┘        │                  │        └──────────────────┘
 ┌─────────────┐        │                  │
 │ Lecturer web│ ─────► │                  │
 │ (HTML/JS)   │  fetch └──────────────────┘
 └─────────────┘
```

## Repository layout
| Path                         | What it is                                              |
|------------------------------|---------------------------------------------------------|
| `app/`                       | Android app (Kotlin) — student/lecturer/admin           |
| `api/`                       | PHP REST API for cPanel (one file per resource)         |
| `database/schema.sql`        | MySQL schema — import via phpMyAdmin                     |
| `DATABASE.md`                | Full database structure documentation                   |
| `webapp/lecturer-portal/`    | Lecturer web portal (talks to `/api`)                   |

## Set-up order
1. **Database** — create a DB + user in cPanel, import `database/schema.sql`.
2. **API** — upload `api/` to `public_html/api`, edit `api/config.php` with your
   DB details, open `/api/install.php` once, then delete it. (See `api/README.md`.)
3. **Web portal** — upload `webapp/lecturer-portal/`, set `API_BASE`.
4. **Android** — point the app's base URL at `https://yourdomain.com/api/`.

## Default accounts
| Role     | Email              | Password |
|----------|--------------------|----------|
| Admin    | admin@slems.com    | admin123 |
| Lecturer | lecturer@slems.com | lec123   |

Students self-register from the app (or `auth.php?action=register`).

## Status
- ✅ Database schema + documentation
- ✅ PHP REST API (auth, users, subjects, materials, exams, submissions,
     assignments, results, notifications, reports)
- ✅ Lecturer web portal wired to the API
- ✅ Android app with new indigo/teal UI (currently uses local SQLite)
- ⏳ **Next:** repoint the Android app from local SQLite to the cPanel API
     (add `ApiConfig.kt` + Volley calls), and add `upload.php` for real file
     storage of scanned answer sheets.
