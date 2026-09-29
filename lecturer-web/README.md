# Smart Learning — Lecturer Web Portal

A React + TypeScript web app for the **Lecturer** role of the Smart Learning and
Exam Management System. It uses the **same Firebase project** as the Android app
for **Authentication** (lecturers sign in with their Firebase account), and stores
its data in **Cloud Firestore + Storage**.

> ⚠️ **Data note:** the Android app currently keeps its app data (subjects, exams,
> materials, submissions) in an **on-device SQLite** database, while this portal uses
> **Firestore**. They share *login* (Firebase Auth) but **not data yet** — so exams
> you create here won't appear on the phone until the Android app is migrated to
> Firestore. That migration is the recommended next step to fully unify the system.

> This is the lecturer companion to the Android app in the parent folder. Lecturers
> get a desktop browser UI for the things that are awkward on a phone — uploading
> materials, creating exams (with **deadline + exam paper**), and grading answer
> sheets in a table.

## Features

- **Login** (lecturer accounts only — other roles are rejected) + password reset
- **Dashboard** with counts of your subjects / materials / exams / assignments
- **Subjects** — create, list, delete
- **Learning Materials** — upload PDF / PPT / notes to Firebase Storage, or add a
  video link; list and delete
- **Exams** — create, list, toggle published (draft ↔ live)
- **Exam Submissions** — pick an exam, view students' uploaded answer-sheet
  pages, enter marks + feedback, and publish a result (writes to `results`)
- **Assignments** — create and list
- **Assignment Submissions** — pick an assignment, view submitted files, enter
  marks + feedback (writes back to `assignmentSubmissions`)
- **Profile** — update name and phone

## Tech stack

React 18 · TypeScript · Vite · Material UI 5 · React Router 6 · Firebase Web SDK 10

## Setup

```bash
cd lecturer-web
npm install
cp .env.example .env   # then fill in your Firebase web config
npm run dev
```

Then open http://localhost:5173.

### Firebase config

1. In the [Firebase console](https://console.firebase.google.com), open the same
   project used by the Android app.
2. Add a **Web app** (</> icon) and copy its config values into `.env`
   (see `.env.example`). All keys must start with `VITE_`.
3. Make sure **Email/Password** auth, **Firestore**, and **Storage** are enabled.

### Logging in (auto-bootstrap)

You just need a Firebase **Authentication** account:

1. Firebase console → **Authentication → Users → Add user** (e.g.
   `lecturer@slems.com` + a password of 6+ chars). The same account works in the
   Android app.
2. Log in here with those credentials. On first sign-in the portal **creates the
   `users/{uid}` profile document automatically** with role `LECTURER` — no manual
   Firestore editing needed.

> To restrict who can enter (e.g. block students), set that user's `role` field in
> Firestore to something other than `LECTURER`/`ADMIN` and they'll be rejected.

### Firestore & Storage rules (so it can read/write)

For development, open the rules to signed-in users:

**Firestore** (`Rules` tab):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /{document=**} { allow read, write: if request.auth != null; }
  }
}
```
**Storage** (`Rules` tab):
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} { allow read, write: if request.auth != null; }
  }
}
```
Tighten these before any real deployment.

## Project structure

```
src/
├─ firebase.ts            Firebase init (auth, db, storage)
├─ types.ts               Shared data models + collection names (mirror Android)
├─ theme.ts               MUI theme (indigo/teal, matches Android)
├─ auth/AuthContext.tsx   Auth state + lecturer-role guard
├─ services/data.ts       Firestore/Storage data access
├─ components/Layout.tsx  App bar + sidebar navigation
└─ pages/                 Login, Dashboard, Subjects, Materials, Exams,
                          Submissions, Assignments, Profile
```

## Deploying to Firebase Hosting

Hosting is pre-configured ([`firebase.json`](firebase.json) serves `dist/` with
SPA rewrites so React Router deep links work on refresh).

```bash
npm install -g firebase-tools   # one-time
firebase login                  # one-time

# Point .firebaserc at your project (replace YOUR_FIREBASE_PROJECT_ID),
# or run:  firebase use --add

npm run deploy                  # builds, then deploys hosting
```

Your portal goes live at `https://YOUR_FIREBASE_PROJECT_ID.web.app`.

## Notes / next steps

- Add server-side **Firestore indexes** only if you reintroduce `orderBy` with a
  `where` filter — current queries sort client-side to avoid that.
- The bundle is one ~900 kB chunk (MUI + Firebase); split with dynamic
  `import()` / `manualChunks` if first-load time matters.
