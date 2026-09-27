# Firebase Authentication — Setup Guide

The app's **login / signup / logout** now use **Firebase Authentication** (email &
password), matching the `AuthManager` (AuthViewModel) design. Firebase verifies the
credentials; the app keeps a local profile row (id + role) matched by **email**, so
the student / lecturer / admin routing still works.

> ⚠️ The project ships with a **placeholder** `app/google-services.json` so it
> compiles. You **must replace it** with your own file from the Firebase console,
> or login will not connect to a real project.

---

## 1. Create a Firebase project
1. Go to <https://console.firebase.google.com> → **Add project** → name it
   (e.g. *Smart Learning*) → continue (Analytics optional).

## 2. Register the Android app
1. In the project, click the **Android** icon → **Add app**.
2. **Android package name:** `com.example.mycamerafinal`  (must match exactly).
3. Register → **Download `google-services.json`**.
4. Put that file in the app module folder, replacing the placeholder:
   ```
   D:\MyCameraFinal\app\google-services.json
   ```

## 3. Enable Email/Password sign-in
1. Firebase console → **Build → Authentication → Get started**.
2. **Sign-in method** tab → enable **Email/Password** → Save.

## 4. Create the admin & lecturer accounts
Students self-register from the app. For the privileged roles, create the accounts
once so they can sign in, **using the same emails the app seeds locally**:

1. Authentication → **Users** tab → **Add user**:
   - `admin@slems.com`  + a password (min 6 chars)
   - `lecturer@slems.com` + a password
2. These emails already exist in the app's local profile table with roles
   `admin` / `lecturer`, so logging in with them routes to the right dashboard.

> Any other new sign-up defaults to the **student** role. To make some other email a
> lecturer/admin, sign in once (creates the local row), then change its `role` in the
> local `users` table — or tell me and I'll add an admin “promote user” screen.

## 5. Build & run
- In Android Studio: **Sync Project with Gradle Files**, then Run.
- Internet is required the first time so Gradle can download the Firebase libraries.

---

## How it works in code
| Piece | File | Role |
|-------|------|------|
| Auth layer (login/signup/signout/checkAuthStatus) | `data/AuthManager.kt` | wraps `FirebaseAuth` |
| Login screen | `LoginActivity.kt` | calls `AuthManager.login` |
| Sign-up screen | `RegisterActivity.kt` | calls `AuthManager.signup` |
| Logout | `BaseDashboardActivity.kt` | calls `AuthManager.signout` |
| Profile/role bridge | `AppDatabaseHelper.findUserByEmail` | maps email → id + role |
| Gradle | root + `app/build.gradle.kts` | google-services plugin + `firebase-auth` |

## Notes
- Firebase requires passwords of **at least 6 characters** (enforced on the sign-up
  screen).
- Firebase needs **Google Play Services** on the device — fine on the Galaxy A6+.
- The rest of the app (subjects, exams, materials, scanning) still uses the local
  SQLite database; only authentication moved to Firebase.
- If you later want everything in the cloud, the same Firebase project can add
  **Firestore** for profiles/roles — ask and I'll migrate it.
