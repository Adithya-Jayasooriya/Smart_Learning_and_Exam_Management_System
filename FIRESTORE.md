# Database Guide — Cloud Firestore (Android app + Lecturer web)

The **single shared database** for both clients is **Cloud Firestore**, with files in
**Firebase Storage** and login via **Firebase Authentication**. One Firebase project
(`smart-learning-dcebc`) powers everything.

```
   Android app  ─┐
                 ├──►  Firebase Auth        (who you are)
   Lecturer web ─┤ ──►  Cloud Firestore      (all data — shared, live)
                 └──►  Firebase Storage      (PDF papers / answer sheets / materials)
```

> The earlier `DATABASE.md`, `database/schema.sql` and `api/*.php` (MySQL/cPanel) are
> **legacy** and no longer used — keep them for reference only.

---

## 1. Connect the apps to the database

Both apps are **already configured** to your project. You only need to turn on the
services in the Firebase console (once).

### A. Firebase console (one-time)
Open <https://console.firebase.google.com> → project **smart-learning-dcebc**:

1. **Authentication** → *Sign-in method* → enable **Email/Password**.
2. **Firestore Database** → *Create database* → **Start in test mode** → pick a
   location (e.g. `asia-south1`) → Enable.
3. **Storage** → *Get started* → accept the default bucket. (If it asks for the
   **Blaze** plan, enabling it is free under the limits — it's needed to upload
   files. If you don't want Storage, lecturers can paste file URLs instead.)
4. **Authentication → Users** → add the privileged accounts, then set their role in
   Firestore (step 3 below): `admin@slems.com`, `lecturer@slems.com`.

### B. Android app — already connected
- `app/google-services.json` ← your project file (present ✅).
- Plugins/deps already added: `com.google.gms.google-services`,
  `firebase-auth`, `firebase-firestore`, `firebase-storage`.
- Firebase auto-initializes from `google-services.json` — `FirebaseFirestore`,
  `FirebaseStorage`, `FirebaseAuth` just work. Nothing else to do.
- Build & run from Android Studio (needs internet).

### C. Lecturer web — already connected
- `lecturer-web/.env` holds the **web** config (apiKey, authDomain, projectId,
  storageBucket, messagingSenderId, appId). `src/firebase.ts` reads it.
- Run it:
  ```bash
  cd lecturer-web
  npm install
  npm run dev        # local dev at http://localhost:5173
  # or deploy:
  npm run build && firebase deploy --only hosting
  ```
- Log in with a **lecturer** or **admin** account.

---

## 2. Security rules

### Firestore (console → Firestore → Rules)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function signedIn() { return request.auth != null; }
    function myRole() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role;
    }

    // Anyone signed in can read; users manage their own profile; admins manage all.
    match /users/{uid} {
      allow read: if signedIn();
      allow create: if request.auth.uid == uid;
      allow update: if request.auth.uid == uid || myRole() == 'ADMIN';
    }

    // Students read; lecturers/admins write the academic data.
    match /{col}/{doc} {
      allow read: if signedIn();
      allow write: if signedIn();   // tighten to lecturer/admin later if needed
    }
  }
}
```
(Start permissive for development; tighten `write` to `myRole() in ['LECTURER','ADMIN']`
for content, and student-owned writes for submissions, before going live.)

### Storage (console → Storage → Rules)
```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## 3. Set roles (important)
New sign-ups default to **STUDENT**. To make an account a lecturer/admin, set its
role in Firestore → `users` → open the user document → set `role` to `LECTURER`
or `ADMIN`. (The app's **Admin → Add Lecturer** screen does this automatically.)

---

## Full database structure

All timestamps are **epoch milliseconds** (a `number`). Document ids are
auto-generated unless noted. Field names are identical on Android (`Fire.kt`) and
web (`src/types.ts`).

### `users`  · doc id = Firebase Auth **uid**
| Field      | Type                                  | Notes                         |
|------------|---------------------------------------|-------------------------------|
| id         | string                                | same as the uid               |
| fullName   | string                                |                               |
| email      | string                                |                               |
| role       | `"STUDENT"` \| `"LECTURER"` \| `"ADMIN"` | drives app routing          |
| phone      | string                                |                               |
| active     | boolean                               | admin can deactivate          |
| photoUrl   | string                                |                               |
| createdAt  | number                                |                               |

### `subjects`
| Field       | Type   | Notes                          |
|-------------|--------|--------------------------------|
| code        | string | e.g. `CS101`                   |
| name        | string |                                |
| description | string |                                |
| lecturerId  | string | owning lecturer's uid (or "")  |
| createdAt   | number |                                |

### `materials`
| Field       | Type                                       | Notes                         |
|-------------|--------------------------------------------|-------------------------------|
| subjectId   | string                                     | → subjects                    |
| title       | string                                     |                               |
| description | string                                     |                               |
| type        | `"PDF"` \| `"PPT"` \| `"VIDEO"` \| `"NOTE"` |                               |
| fileUrl     | string                                      | Storage URL, or video link    |
| uploadedBy  | string                                      | lecturer uid                  |
| approved    | boolean                                     | admin approval flag           |
| createdAt   | number                                      |                               |

### `exams`
| Field           | Type    | Notes                                |
|-----------------|---------|--------------------------------------|
| subjectId       | string  | → subjects                           |
| title           | string  |                                      |
| description     | string  |                                      |
| startAt         | number  | when the exam is held                |
| **deadline**    | number  | **answer-sheet submission deadline** |
| **paperUrl**    | string  | uploaded exam paper (Storage URL)    |
| durationMinutes | number  | default 60                           |
| totalMarks      | number  | default 100                          |
| createdBy       | string  | lecturer uid                         |
| published       | boolean | visible to students                  |

### `examSubmissions`  (scanned answer sheets)
| Field            | Type                       | Notes                              |
|------------------|----------------------------|------------------------------------|
| examId           | string                     | → exams                            |
| studentId        | string                     | student uid                        |
| studentName      | string                     | denormalized for the grading list  |
| examTitle        | string                     | denormalized                       |
| answerSheetUrls  | string[]                   | Storage URLs (the scanned PDF)     |
| status           | `"SUBMITTED"` \| `"GRADED"`|                                    |
| marks            | number \| null             | filled when graded                 |
| feedback         | string                     |                                    |
| submittedAt      | number                     |                                    |

### `results`  (published grades)
| Field       | Type   | Notes                |
|-------------|--------|----------------------|
| studentId   | string | → users              |
| subjectId   | string | → subjects           |
| examId      | string | → exams              |
| examTitle   | string | denormalized         |
| marks       | number |                      |
| totalMarks  | number |                      |
| grade       | string | A / B / C / S / F    |
| publishedAt | number |                      |

### `assignments`  *(web app)*
| Field | Type | Notes |
|-------|------|-------|
| subjectId | string | → subjects |
| title | string | |
| description | string | |
| dueAt | number | |
| totalMarks | number | |
| createdBy | string | lecturer uid |

### `assignmentSubmissions`  *(web app)*
| Field | Type | Notes |
|-------|------|-------|
| assignmentId | string | → assignments |
| studentId | string | |
| fileUrls | string[] | Storage URLs |
| submittedAt | number | |
| status | `"SUBMITTED"` \| `"GRADED"` | |
| marks | number \| null | |
| feedback | string | |

### `notifications`
| Field     | Type   | Notes                          |
|-----------|--------|--------------------------------|
| userId    | string | a uid, or `"ALL"` for everyone |
| title     | string |                                |
| message   | string |                                |
| createdAt | number |                                |

---

## Storage (Firebase Storage) layout
| Path                                   | What                         | Written by |
|----------------------------------------|------------------------------|------------|
| `examPapers/{uid}/{ts}_{name}`         | Exam paper PDFs              | lecturer   |
| `answerSheets/{uid}/{ts}_{name}`       | Scanned answer-sheet PDFs    | student    |
| `materials/{uid}/{ts}_{name}`          | Learning material files      | lecturer   |

The download URL returned by Storage is stored in the matching Firestore field
(`paperUrl`, `answerSheetUrls[]`, `fileUrl`).

---

## Relationships (overview)
```
users (uid) ──< subjects (lecturerId)
subjects ──< materials
subjects ──< exams ──< examSubmissions >── users (studentId)
exams ──< results >── users (studentId)
subjects ──< assignments ──< assignmentSubmissions >── users
users ──< notifications   (userId, or "ALL")
```

## Who writes what
| Action | Android | Web |
|--------|:------:|:---:|
| Sign up / login / role profile | ✅ | ✅ |
| Create/manage subjects | ✅ (admin/lecturer) | ✅ |
| Upload materials | ✅ | ✅ |
| Create exams (paper + deadline) | ✅ | ✅ |
| Scan & upload answer sheets | ✅ (student) | — |
| Grade + publish results | ✅ | ✅ |
| Notifications | ✅ | reads |

Because both clients use the same collections and fields, data created on one
appears on the other in real time.
