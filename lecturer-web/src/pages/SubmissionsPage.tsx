import { useCallback, useEffect, useState } from "react";
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Link as MuiLink,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import { useAuth } from "../auth/AuthContext";
import {
  gradeSubmission,
  listExamsByLecturer,
  listSubmissionsByExam,
} from "../services/data";
import type { Exam, ExamSubmission } from "../types";

export default function SubmissionsPage() {
  const { profile } = useAuth();
  const [exams, setExams] = useState<Exam[]>([]);
  const [examId, setExamId] = useState("");
  const [submissions, setSubmissions] = useState<ExamSubmission[]>([]);

  // Grading dialog
  const [grading, setGrading] = useState<ExamSubmission | null>(null);
  const [marks, setMarks] = useState(0);
  const [feedback, setFeedback] = useState("");

  useEffect(() => {
    if (!profile) return;
    void (async () => setExams(await listExamsByLecturer(profile.id)))();
  }, [profile]);

  const loadSubmissions = useCallback(async (id: string) => {
    if (!id) {
      setSubmissions([]);
      return;
    }
    setSubmissions(await listSubmissionsByExam(id));
  }, []);

  useEffect(() => {
    void loadSubmissions(examId);
  }, [examId, loadSubmissions]);

  const selectedExam = exams.find((e) => e.id === examId);

  function openGrade(sub: ExamSubmission) {
    setGrading(sub);
    setMarks(sub.marks ?? 0);
    setFeedback(sub.feedback ?? "");
  }

  async function submitGrade() {
    if (!grading || !selectedExam) return;
    await gradeSubmission({
      submissionId: grading.id,
      studentId: grading.studentId,
      exam: selectedExam,
      marks: Number(marks),
      feedback,
    });
    setGrading(null);
    await loadSubmissions(examId);
  }

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Exam Submissions
      </Typography>

      <TextField
        select
        label="Select exam"
        value={examId}
        onChange={(e) => setExamId(e.target.value)}
        sx={{ minWidth: 320, mb: 2 }}
      >
        {exams.map((e) => (
          <MenuItem key={e.id} value={e.id}>
            {e.title}
          </MenuItem>
        ))}
      </TextField>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Student ID</TableCell>
              <TableCell>Submitted</TableCell>
              <TableCell>Answer sheets</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Marks</TableCell>
              <TableCell align="right">Action</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {submissions.map((s) => (
              <TableRow key={s.id}>
                <TableCell>{s.studentId}</TableCell>
                <TableCell>
                  {s.submittedAt ? new Date(s.submittedAt).toLocaleString() : "—"}
                </TableCell>
                <TableCell>
                  <Stack direction="row" spacing={1}>
                    {s.answerSheetUrls.length === 0 && "—"}
                    {s.answerSheetUrls.map((url, i) => (
                      <MuiLink key={url} href={url} target="_blank" rel="noreferrer">
                        p{i + 1}
                      </MuiLink>
                    ))}
                  </Stack>
                </TableCell>
                <TableCell>
                  <Chip
                    size="small"
                    label={s.status}
                    color={s.status === "GRADED" ? "success" : "warning"}
                  />
                </TableCell>
                <TableCell>{s.marks ?? "—"}</TableCell>
                <TableCell align="right">
                  <Button size="small" variant="outlined" onClick={() => openGrade(s)}>
                    Grade
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {examId && submissions.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No submissions for this exam.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={!!grading} onClose={() => setGrading(null)} fullWidth maxWidth="xs">
        <DialogTitle>Grade submission</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Box>
              <Typography variant="body2" color="text.secondary">
                Out of {selectedExam?.totalMarks ?? 100} marks
              </Typography>
            </Box>
            <TextField
              label="Marks"
              type="number"
              value={marks}
              onChange={(e) => setMarks(Number(e.target.value))}
            />
            <TextField
              label="Feedback"
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
              multiline
              minRows={2}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setGrading(null)}>Cancel</Button>
          <Button variant="contained" onClick={() => void submitGrade()}>
            Save &amp; publish result
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
