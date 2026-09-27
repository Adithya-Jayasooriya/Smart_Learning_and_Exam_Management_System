<?php
/**
 * WEB PORTAL — LOGIN PAGE (the entry point of the lecturer portal).
 * Uses the SAME users table as the Android app, so a lecturer signs in
 * here with exactly the same email/password as in the mobile app.
 * A successful login is remembered in a PHP SESSION (server-side memory
 * tied to a browser cookie) until the lecturer logs out.
 */
require_once __DIR__ . '/db.php';

// Already logged in? Skip the form and go straight to the dashboard.
if (!empty($_SESSION['user'])) {
    header('Location: dashboard.php');
    exit;
}

$error = '';
// This block runs only when the form below was submitted (POST).
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = trim($_POST['email'] ?? '');
    $pass  = (string)($_POST['password'] ?? '');

    // Look up the account (prepared statement = safe from SQL injection).
    $st = db()->prepare('SELECT * FROM users WHERE email = ?');
    $st->execute([$email]);
    $u = $st->fetch();

    // Same checks as the mobile app login, plus a role check:
    // students cannot enter the lecturer portal.
    if (!$u || !passwordMatches($pass, $u['password'])) {
        $error = 'Incorrect email or password.';
    } elseif ((int)$u['active'] !== 1) {
        $error = 'This account has been deactivated.';
    } elseif (!in_array($u['role'], ['lecturer', 'admin'], true)) {
        $error = 'This portal is for lecturers only.';
    } else {
        // Success: remember who is logged in, then open the dashboard.
        $_SESSION['user'] = ['id' => (int)$u['id'], 'name' => $u['name'], 'role' => $u['role']];
        header('Location: dashboard.php');
        exit;
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lecturer Login — Smart Learning</title>
    <link rel="stylesheet" href="style.css">
</head>
<body class="login-page">
    <div class="login-card">
        <h1>Smart Learning</h1>
        <p class="sub">Lecturer Portal — sign in to continue</p>
        <?php if ($error): ?><p class="error"><?= e($error) ?></p><?php endif; ?>
        <form method="post">
            <label>Email
                <input type="email" name="email" required value="<?= e($_POST['email'] ?? '') ?>">
            </label>
            <label>Password
                <input type="password" name="password" required>
            </label>
            <button type="submit">Login</button>
        </form>
    </div>
</body>
</html>
