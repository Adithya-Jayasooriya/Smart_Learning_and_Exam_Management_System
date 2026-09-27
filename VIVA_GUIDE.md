`# 🎓 VIVA PREPARATION GUIDE — Smart Learning & Exam Management System

This guide has two parts:
**Part 1** — where to change things when the lecturer says "change X and show me".
**Part 2** — answers to the technical questions lecturers usually ask.

---

## Part 1 — "Change this and show me" (live-change cheat sheet)

### Change the BUTTON COLOUR (most common question!)
1. Open `app/src/main/res/values/colors.xml`
2. Find: `<color name="primary">#4361EE</color>`
3. Change the hex value, e.g. `#E91E63` (pink), `#2E7D32` (green), `#FF9800` (orange)
4. Run the app — **every** button, the action bar and highlights change together,
   because all of them refer to `@color/primary`.

> Explain to the lecturer: *"All colours are defined once in colors.xml and the
> rest of the app refers to them by name — so one change updates the whole app.
> This is called centralising the design system."*

### Change ONLY the button shape/roundness
`app/src/main/res/drawable/btn_primary.xml` → `<corners android:radius="14dp"/>`
(bigger = rounder; `0dp` = square corners).

### Change the button TEXT colour or size
`app/src/main/res/values/themes.xml` → the `AppButton` style
(`android:textColor`, add `android:textSize` if asked).

### Change the screen background colour
`colors.xml` → `<color name="bg">#EEF1FB</color>`

### Change the app name shown in the action bar / launcher
`app/src/main/AndroidManifest.xml` → `android:label="Smart Learning"`

### Change a label / text on one screen
Texts live in the layout files: `app/src/main/res/layout/activity_login.xml`,
`activity_register.xml`, etc. Find the `android:text="..."` and edit it.

### Change the grading scale (A/B/C/S/F)
`app/src/main/java/com/example/mycamerafinal/data/Fire.kt` → function `gradeFor()`
(75+ = A, 65 = B, 55 = C, 40 = S, else F).

### Change the seeded demo accounts
`app/src/main/java/com/example/mycamerafinal/data/LocalDb.kt` → function `seed()`
(note: uninstall + reinstall the app to re-create the database with new seeds).

### The back arrow (←) on inner pages — how it works
**Mobile app:** one small class, `BackArrowActivity.kt`, turns on the action-bar
arrow (`setDisplayHomeAsUpEnabled(true)`) and closes the screen when tapped
(`onSupportNavigateUp() → finish()`). Every inner screen `extends BackArrowActivity`;
dashboards and Login don't, so they have no arrow.
**Web portal:** the shared `pageHeader()` function in `server/portal/db.php` prints
a small round ← link when the page passes a back target
(e.g. `pageHeader('Exams', $user, 'dashboard.php')`); styled by `header .back` in `style.css`.

### Add one more button to a dashboard
Open e.g. `StudentDashboardActivity.kt` and add one line inside `buildMenu()`:
```kotlin
menu("🆕   My New Feature") { open(SomeActivity::class.java) }
```

---

## Part 2 — Technical questions & suggested answers

**Q: Explain the architecture of your system.**
A: Three parts. (1) An Android app in Kotlin — the screens never touch a
database directly; they all call one data layer (`Api` object). (2) That data
layer has two modes: with no server configured it uses an on-device **SQLite**
database, so the app works fully offline; with a server URL set it sends the
same requests as JSON over **Volley** to a **PHP REST API** on cPanel with a
**MySQL** database. (3) A **lecturer web portal** in PHP shares that same
MySQL database, so grades entered in the browser appear instantly in the app.

**Q: What is Volley and why did you use it?**
A: Volley is Google's HTTP networking library for Android. It runs requests on
background threads automatically, queues them, retries on failure, and returns
results on the main thread so I can update the UI directly. I use
`JsonObjectRequest` for JSON and a custom multipart request for file uploads.

**Q: Why both SQLite and MySQL?**
A: SQLite is embedded in the phone — perfect for offline use and demos without
internet; students with poor data connections can still use every feature.
MySQL on cPanel is the central shared database so all users see the same data
and the web portal can work. Both have the same six tables, and the app
switches between them by changing one URL — the screens don't change at all.

**Q: How does login work, step by step?**
A: The user types email+password → `AuthManager.login()` posts them to
`auth.php?action=login` (or the SQLite handler offline) → the database looks
up the email, verifies the password, checks the account is active → returns
the user's id, name, email and **role** → the app saves these in
SharedPreferences (`SessionManager`) → `LoginActivity.routeTo()` opens the
Student/Lecturer/Admin dashboard based on the role.

**Q: How are passwords stored?**
A: On the server every account created through the app is stored as a
**bcrypt hash** using PHP's `password_hash()` — the real password is never
saved, and `password_verify()` checks it at login. (The three seeded demo
accounts use plain text only so the system works on first import; they should
be changed after installation.)

**Q: How do you prevent SQL injection?**
A: Every SQL statement uses **prepared statements** — PDO `prepare()/execute()`
in PHP and parameterised queries (`?` placeholders / ContentValues) in Android
SQLite. User input is passed as data, never joined into the SQL text.

**Q: How does the answer-sheet scanning work?**
A: I use the **Google ML Kit Document Scanner**. The scanning screen itself is
hosted by Google Play services, so my app needs no camera permission. It does
automatic edge detection, cropping and enhancement, supports up to 10 pages,
and returns everything as **one PDF**. My code receives the PDF URI, copies it
to app storage, uploads it, and records a `submissions` row for that exam and
student.

**Q: How do you stop students seeing the paper early or submitting late?**
A: Two layers. In the UI, `FsExam.hasStarted()` and `isOverdue()` decide which
options appear — before the start time the paper option is locked and
submission is disabled; after the deadline submission (and re-submission) is
blocked. As a backstop, the database layer itself (SQLite handler and
`submissions.php`) rejects any submission outside the [start, deadline]
window, so even bypassing the UI cannot break the rule.

**Q: What happens when a lecturer grades a submission?**
A: The submission row is updated (`status = 'graded'`, marks, graded_by), a
notification row is inserted for that student, and the student's Results
screen calculates the letter grade from the percentage. This works identically
from the app and from the web portal because both write to the same table.

**Q: What is the role of SharedPreferences here?**
A: It stores the session (id, name, email, role) in a small private file, so
the user stays logged in after closing the app. Logout clears it.

**Q: What is REST / JSON?**
A: REST is a style of API where each URL represents a resource and standard
HTTP methods act on it. My endpoints (subjects.php, exams.php, …) accept GET
for reading and POST for changes, and always answer the same JSON envelope:
`{ "success": true/false, "message": "...", "data": ... }` — the app checks
`success` and shows `message` when something goes wrong.

**Q: Which technologies did you use overall?**
A: Kotlin, Android SDK (min 24 / compile 35), AppCompat, SQLite, Volley,
Google ML Kit Document Scanner (Play services), FileProvider and
DownloadManager for files, PHP 8 with PDO, MySQL, cPanel hosting, HTML/CSS
for the portal, Gradle/AGP for building, Android Studio + a real Android 16
and Android 9 device for testing.

**Q: What are the limitations / future work?**
A: Offline SQLite and online MySQL don't synchronise; the API has no token
authentication yet; notifications are in-app only (no push). Future work:
FCM push notifications, JWT tokens, offline-first sync, an admin web portal,
and OCR to read index numbers from scanned sheets automatically.

---

## Demo logins (for the presentation)

| Role | Email | Password |
|---|---|---|
| Admin | admin@slems.lk | admin123 |
| Lecturer | lecturer@slems.lk | lecturer123 |
| Student | student@slems.lk | student123 |

**Demo tip:** the app works with NO internet (SQLite mode) — if the venue WiFi
fails, everything still works. Say this proudly; it's a feature you designed.
