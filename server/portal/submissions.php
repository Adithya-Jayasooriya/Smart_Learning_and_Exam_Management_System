<?php
/**
 * WEB PORTAL — SUBMISSIONS / GRADING PAGE for one exam.
 * Lists every student's scanned answer sheet with a link to open the PDF
 * and a small form to enter marks (0–100). Saving a grade:
 *   1. updates the submissions row (status = 'graded', marks, graded_by),
 *   2. inserts a notification for that student,
 * so the student instantly sees the result in the Android app.
 */
require_once __DIR__ . '/db.php';
$user = requireLecturer();

$examId = (int)($_GET['exam_id'] ?? 0);   // which exam we are grading

$st = db()->prepare(
    'SELECT e.*, s.code AS subject_code, s.name AS subject_name
     FROM exams e LEFT JOIN subjects s ON s.id = e.subject_id
     WHERE e.id = ?'
);
$st->execute([$examId]);
$exam = $st->fetch();
if (!$exam) {
    header('Location: exams.php');
    exit;
}

$message = '';

// Save a grade and notify the student — same behavior as the Android app.
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $subId = (int)($_POST['submission_id'] ?? 0);
    $marks = (int)($_POST['marks'] ?? -1);
    if ($marks < 0 || $marks > 100) {
        $message = 'Marks must be between 0 and 100.';
    } else {
        $st = db()->prepare("UPDATE submissions SET marks = ?, status = 'graded', graded_by = ? WHERE id = ? AND exam_id = ?");
        $st->execute([$marks, $user['id'], $subId, $examId]);

        $st = db()->prepare('SELECT student_id FROM submissions WHERE id = ?');
        $st->execute([$subId]);
        if ($row = $st->fetch()) {
            $st = db()->prepare('INSERT INTO notifications (user_id, title, message) VALUES (?, ?, ?)');
            $st->execute([(int)$row['student_id'], 'Result published',
                          "Your result for '" . $exam['title'] . "' is " . $marks . '/100']);
        }
        $message = 'Grade saved and published.';
    }
}

$st = db()->prepare(
    'SELECT sub.*, u.name AS student_name, u.email AS student_email
     FROM submissions sub LEFT JOIN users u ON u.id = sub.student_id
     WHERE sub.exam_id = ? ORDER BY u.name'
);
$st->execute([$examId]);
$submissions = $st->fetchAll();

pageHeader('Submissions', $user, 'exams.php');   // ← back arrow leads to the exams page
?>
<h2>Submissions — <?= e($exam['title']) ?></h2>
<p class="sub"><?= e($exam['subject_code'] . ' ' . $exam['subject_name']) ?> •
   Exam: <?= e($exam['exam_date']) ?> • Deadline: <?= e($exam['deadline']) ?></p>

<?php if ($message): ?><p class="ok"><?= e($message) ?></p><?php endif; ?>

<?php if (!$submissions): ?>
    <p class="empty">No submissions yet.</p>
<?php else: ?>
    <table>
        <tr><th>Student</th><th>Status</th><th>Answer sheet</th><th>Grade (marks / 100)</th></tr>
        <?php foreach ($submissions as $s): ?>
            <tr>
                <td><?= e($s['student_name']) ?><br><small><?= e($s['student_email']) ?></small></td>
                <td><span class="badge <?= e($s['status']) ?>"><?= e(strtoupper($s['status'])) ?></span></td>
                <td><?php if ($s['file_url']): ?><a href="<?= e($s['file_url']) ?>" target="_blank">open</a><?php else: ?>—<?php endif; ?></td>
                <td>
                    <form method="post" class="grade-form">
                        <input type="hidden" name="submission_id" value="<?= (int)$s['id'] ?>">
                        <input class="marks" type="number" name="marks" min="0" max="100"
                               value="<?= $s['marks'] !== null ? (int)$s['marks'] : '' ?>" required>
                        <button type="submit" class="btn small">Save grade</button>
                    </form>
                </td>
            </tr>
        <?php endforeach; ?>
    </table>
<?php endif; ?>

<p><a href="exams.php">← Back to exams</a></p>
<?php pageFooter(); ?>
