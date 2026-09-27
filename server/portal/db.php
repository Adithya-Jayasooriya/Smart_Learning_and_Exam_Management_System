<?php
/**
 * Shared bootstrap for the lecturer web portal.
 * Uses the same database credentials as the API (../api/config.php).
 */
session_start();
require_once __DIR__ . '/../api/config.php';

function db(): PDO {
    static $pdo = null;
    if ($pdo === null) {
        $pdo = new PDO(
            'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
            DB_USER, DB_PASS,
            [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            ]
        );
    }
    return $pdo;
}

/** HTML-escape for safe output. */
function e($value): string {
    return htmlspecialchars((string)$value, ENT_QUOTES, 'UTF-8');
}

/** Seed rows are plain text; accounts made through the app/portal are bcrypt. */
function passwordMatches(string $given, string $stored): bool {
    if (strncmp($stored, '$2', 2) === 0) { return password_verify($given, $stored); }
    return hash_equals($stored, $given);
}

/** Every page except the login page starts with this. */
function requireLecturer(): array {
    if (empty($_SESSION['user'])) {
        header('Location: index.php');
        exit;
    }
    return $_SESSION['user'];
}

/**
 * Prints the top of every portal page (HTML head + blue header bar).
 * $backHref: where the small ← back arrow should lead. Inner pages pass
 * their parent page (e.g. exams.php passes 'dashboard.php'); the dashboard
 * itself passes nothing, so no arrow is shown on the top-level page —
 * exactly like the mobile app, where dashboards have no back arrow.
 */
function pageHeader(string $title, array $user, string $backHref = ''): void {
    echo '<!DOCTYPE html><html lang="en"><head><meta charset="utf-8">';
    echo '<meta name="viewport" content="width=device-width, initial-scale=1">';
    echo '<title>' . e($title) . ' — Smart Learning</title>';
    echo '<link rel="stylesheet" href="style.css"></head><body>';
    echo '<header><div class="wrap"><h1>';
    if ($backHref !== '') {
        // The small round ← button (styled by "header .back" in style.css).
        echo '<a class="back" href="' . e($backHref) . '" title="Back">&#8592;</a>';
    }
    echo 'Smart Learning — Lecturer Portal</h1>';
    echo '<nav><a href="dashboard.php">Dashboard</a> <a href="exams.php">Exams</a> ';
    echo '<span class="who">' . e($user['name']) . '</span> <a class="logout" href="logout.php">Logout</a></nav>';
    echo '</div></header><main class="wrap">';
}

function pageFooter(): void {
    echo '</main></body></html>';
}
