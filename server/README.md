# Smart Learning — cPanel Setup Guide

This folder contains everything needed to run the backend on **cPanel hosting**:

| Folder / file    | What it is                                                        |
|------------------|-------------------------------------------------------------------|
| `database.sql`   | MySQL tables + demo accounts (import once in phpMyAdmin)          |
| `api/`           | REST API used by the **Android app** (Volley)                     |
| `portal/`        | **Lecturer web portal** (login, create exams, grade submissions)  |

Both the Android app and the web portal use the **same MySQL database**.

---

## Step 1 — Create the MySQL database (cPanel)

1. Log in to cPanel and open **MySQL® Databases**.
2. Under *Create New Database*, enter a name (e.g. `slems`) → **Create Database**.
   cPanel will prefix it with your username, e.g. `myuser_slems` — note the full name.
3. Under *MySQL Users → Add New User*, create a user (e.g. `slems`) with a strong
   password → **Create User**. Note the full username, e.g. `myuser_slems`.
4. Under *Add User To Database*, add that user to the database and tick
   **ALL PRIVILEGES** → Make Changes.

## Step 2 — Import the tables

1. In cPanel open **phpMyAdmin**.
2. Click your database (`myuser_slems`) in the left sidebar.
3. Open the **Import** tab, choose `database.sql` from this folder → **Go**.
4. You should now see 6 tables: `users`, `subjects`, `materials`, `exams`,
   `submissions`, `notifications` — with 3 demo accounts and 2 demo subjects.

Demo logins (change the passwords after first login!):

| Email               | Password     | Role     |
|---------------------|--------------|----------|
| admin@slems.lk      | admin123     | Admin    |
| lecturer@slems.lk   | lecturer123  | Lecturer |
| student@slems.lk    | student123   | Student  |

## Step 3 — Upload the files

1. In cPanel open **File Manager** and go to `public_html`.
2. Upload the whole **`api`** folder and the whole **`portal`** folder
   (easiest: zip them, upload the zip, then *Extract*). You should end with:
   ```
   public_html/api/auth.php, users.php, subjects.php, ...
   public_html/portal/index.php, dashboard.php, ...
   ```
3. Uploaded files (exam papers, answer sheets) are stored in
   `public_html/api/uploads/` — the API creates this folder automatically.
   If uploads fail, create `api/uploads` yourself and set its permissions to **755**.

## Step 4 — Configure the database connection

Edit **`public_html/api/config.php`** (File Manager → right-click → Edit) and fill in:

```php
define('DB_HOST', 'localhost');                     // almost always localhost on cPanel
define('DB_NAME', 'myuser_slems');                  // from Step 1
define('DB_USER', 'myuser_slems');                  // from Step 1
define('DB_PASS', 'the-password-you-set');          // from Step 1
define('BASE_URL', 'https://yourdomain.com/api/');  // public URL of the api folder, ends with /
```

Both the API **and** the portal read this one file — there is nothing else to configure.

## Step 5 — Test the API in a browser

Open `https://yourdomain.com/api/subjects.php` — you should see JSON like:

```json
{"success":true,"message":"","data":[{"id":"1","code":"IT101", ... }]}
```

If you see a database error, re-check Step 4.

## Step 6 — Connect the Android app

Open `app/src/main/java/com/example/mycamerafinal/data/Api.kt` and set:

```kotlin
var BASE = "https://yourdomain.com/api/"
```

That single line switches the whole app from the built-in offline SQLite
database to your cPanel MySQL server. (Leave the placeholder to keep working
offline — useful for demos with no internet, or for students with data
connection problems.)

> Note: data does NOT sync between the offline SQLite database and the server —
> they are separate stores. Pick one mode for your demo.

## Step 7 — The lecturer web portal

It is already live: open `https://yourdomain.com/portal/` and log in with a
lecturer account. The portal lets lecturers:

- see their subjects and pending-grading counts,
- create exams with **date & time pickers** and an optional PDF paper upload,
- open every student's answer sheet and **enter marks** — the student instantly
  sees the result and a notification in the Android app.

## Troubleshooting

- **"Cannot reach the server" in the app** — check `BASE` ends with `/`, the
  phone has internet, and Step 5 works in the phone's browser too.
- **Free hosts** (InfinityFree, 000webhost…) often block requests that don't
  come from a browser, which breaks mobile apps. Use real cPanel hosting or a
  free tier that allows API traffic.
- **HTTP instead of HTTPS** — the app's manifest already allows cleartext HTTP
  (`android:usesCleartextTraffic="true"`), but use HTTPS whenever possible;
  most cPanel hosts give free AutoSSL certificates.
- **Uploads fail** — check `api/uploads` exists with permission 755, and raise
  `upload_max_filesize` / `post_max_size` in cPanel ▸ *MultiPHP INI Editor*
  (e.g. to 25M) for large scanned PDFs.
- **Security note (for your report):** the API endpoints are open (no API key),
  matching the coursework scope. For production you would add token
  authentication (e.g. issue a token at login and check it on every request).
