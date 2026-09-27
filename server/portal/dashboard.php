<?php
/**
 * WEB PORTAL — DASHBOARD.
 * Shows three live counters (my subjects, my exams, submissions waiting for
 * grading) and the list of subjects assigned to this lecturer. The counts
 * come from simple COUNT(*) SQL queries on the shared MySQL database.
 */
require_once __DIR__ . '/db.php';
$user = requireLecturer();

$st = db()->prepare('SELECT * FROM subjects WHERE lecturer_id = ? ORDER BY code');
$st->execute([$user['id']]);
$subjects = $st->fetchAll();

$st = db()->prepare('SELECT COUNT(*) c FROM exams WHERE created_by = ?');
$st->execute([$user['id']]);
$examCount = (int)$st->fetch()['c'];

$st = db()->prepare(
    "SELECT COUNT(*) c FROM submissions sub
     JOIN exams e ON e.id = sub.exam_id
     WHERE e.created_by = ? AND sub.status = 'submitted'"
);
$st->execute([$user['id']]);
$pendingCount = (int)$st->fetch()['c'];

pageHeader('Dashboard', $user);
?>
<h2>Welcome, <?= e($user['name']) ?></h2>

<div class="cards">
    <div class="card"><span class="num"><?= count($subjects) ?></span>My Subjects</div>
    <div class="card"><span class="num"><?= $examCount ?></span>My Exams</div>
    <div class="card<?= $pendingCount > 0 ? ' highlight' : '' ?>">
        <span class="num"><?= $pendingCount ?></span>Submissions waiting for grading
    </div>
</div>

<h3>My Subjects</h3>
<?php if (!$subjects): ?>
    <p class="empty">No subjects are assigned to you yet. Ask the admin to assign a subject.</p>
<?php else: ?>
    <table>
        <tr><th>Code</th><th>Name</th></tr>
        <?php foreach ($subjects as $s): ?>
            <tr><td><?= e($s['code']) ?></td><td><?= e($s['name']) ?></td></tr>
        <?php endforeach; ?>
    </table>
<?php endif; ?>

<p><a class="btn" href="exams.php">Manage exams &amp; grading →</a></p>
<?php pageFooter(); ?>
