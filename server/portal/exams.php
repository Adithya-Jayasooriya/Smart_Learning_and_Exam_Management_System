<?php
/**
 * WEB PORTAL — EXAMS PAGE.
 * Top half: a form to CREATE an exam (subject dropdown, title, HTML
 * datetime-local pickers for the exam date and deadline, optional PDF paper).
 * Bottom half: a table of the exams THIS lecturer created, with a link to
 * each exam's submissions/grading page.
 * Everything is written to the same MySQL tables the Android app reads,
 * so a new exam appears in the students' phones immediately.
 */
require_once __DIR__ . '/db.php';
$user = requireLecturer();   // redirects to the login page if not signed in

$message = '';
$error = '';

// Create a new exam (optionally with an uploaded paper).
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $subjectId = trim($_POST['subject_id'] ?? '');
    $title     = trim($_POST['title'] ?? '');
    $examDate  = trim($_POST['exam_date'] ?? '');   // from <input type="datetime-local">
    $deadline  = trim($_POST['deadline'] ?? '');

    if ($subjectId === '' || $title === '' || $examDate === '' || $deadline === '') {
        $error = 'Please fill in the subject, title, exam date and deadline.';
    } elseif ($deadline <= $examDate) {
        $error = 'The deadline must be after the exam date.';
    } else {
        // datetime-local gives "2026-07-15T09:30" → MySQL DATETIME "2026-07-15 09:30:00"
        $examDateSql = str_replace('T', ' ', $examDate) . ':00';
        $deadlineSql = str_replace('T', ' ', $deadline) . ':00';

        $paperUrl = '';
        if (!empty($_FILES['paper']['name']) && $_FILES['paper']['error'] === UPLOAD_ERR_OK) {
            $name = preg_replace('/[^A-Za-z0-9._-]/', '_', basename($_FILES['paper']['name']));
            $name = time() . '_' . $name;
            $dir  = __DIR__ . '/../api/uploads/examPapers';
            if (!is_dir($dir)) { mkdir($dir, 0755, true); }
            if (move_uploaded_file($_FILES['paper']['tmp_name'], $dir . '/' . $name)) {
                $paperUrl = BASE_URL . 'uploads/examPapers/' . $name;
            }
        }

        $st = db()->prepare(
            'INSERT INTO exams (subject_id, title, description, exam_date, deadline, paper_url, duration_minutes, created_by)
             VALUES (?, ?, ?, ?, ?, ?, 60, ?)'
        );
        $st->execute([$subjectId, $title, '', $examDateSql, $deadlineSql, $paperUrl, $user['id']]);

        // Broadcast notification, same as the Android app does.
        $st = db()->prepare('INSERT INTO notifications (user_id, title, message) VALUES (0, ?, ?)');
        $st->execute(['New exam', "'" . $title . "' — submit answer sheets by " . str_replace('T', ' ', $deadline)]);

        $message = 'Exam created.';
    }
}

$subjects = db()->query('SELECT * FROM subjects ORDER BY code')->fetchAll();

$st = db()->prepare(
    'SELECT e.*, s.code AS subject_code, s.name AS subject_name,
            (SELECT COUNT(*) FROM submissions sub WHERE sub.exam_id = e.id) AS submission_count
     FROM exams e LEFT JOIN subjects s ON s.id = e.subject_id
     WHERE e.created_by = ?
     ORDER BY e.exam_date DESC'
);
$st->execute([$user['id']]);
$exams = $st->fetchAll();

pageHeader('Exams', $user, 'dashboard.php');   // ← back arrow leads to the dashboard
?>
<h2>Create Exam</h2>
<?php if ($message): ?><p class="ok"><?= e($message) ?></p><?php endif; ?>
<?php if ($error): ?><p class="error"><?= e($error) ?></p><?php endif; ?>

<form method="post" enctype="multipart/form-data" class="form-card">
    <label>Subject
        <select name="subject_id" required>
            <option value="">— choose a subject —</option>
            <?php foreach ($subjects as $s): ?>
                <option value="<?= (int)$s['id'] ?>"><?= e($s['code'] . ' - ' . $s['name']) ?></option>
            <?php endforeach; ?>
        </select>
    </label>
    <label>Exam title <input type="text" name="title" required></label>
    <label>Exam date &amp; time <input type="datetime-local" name="exam_date" required></label>
    <label>Submission deadline <input type="datetime-local" name="deadline" required></label>
    <label>Exam paper (PDF, optional) <input type="file" name="paper" accept="application/pdf,image/*"></label>
    <button type="submit">Create Exam</button>
</form>

<h2>My Exams</h2>
<?php if (!$exams): ?>
    <p class="empty">You have not created any exams yet.</p>
<?php else: ?>
    <table>
        <tr><th>Subject</th><th>Title</th><th>Exam date</th><th>Deadline</th><th>Paper</th><th>Submissions</th></tr>
        <?php foreach ($exams as $x): ?>
            <tr>
                <td><?= e($x['subject_code']) ?></td>
                <td><?= e($x['title']) ?></td>
                <td><?= e($x['exam_date']) ?></td>
                <td><?= e($x['deadline']) ?></td>
                <td><?php if ($x['paper_url']): ?><a href="<?= e($x['paper_url']) ?>" target="_blank">open</a><?php else: ?>—<?php endif; ?></td>
                <td><a class="btn small" href="submissions.php?exam_id=<?= (int)$x['id'] ?>">
                    <?= (int)$x['submission_count'] ?> — grade</a></td>
            </tr>
        <?php endforeach; ?>
    </table>
<?php endif; ?>
<?php pageFooter(); ?>
