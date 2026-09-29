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
  gradeAssignmentSubmission,
  listAssignmentSubmissions,
  listAssignmentsByLecturer,
} from "../services/data";
import type { Assignment, AssignmentSubmission } from "../types";

export default function AssignmentSubmissionsPage() {
  const { profile } = useAuth();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [assignmentId, setAssignmentId] = useState("");
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([]);

  // Grading dialog
  const [grading, setGrading] = useState<AssignmentSubmission | null>(null);
  const [marks, setMarks] = useState(0);
  const [feedback, setFeedback] = useState("");

  useEffect(() => {
    if (!profile) return;
    void (async () => setAssignments(await listAssignmentsByLecturer(profile.id)))();
  }, [profile]);

  const loadSubmissions = useCallback(async (id: string) => {
    if (!id) {
      setSubmissions([]);
      return;
    }
    setSubmissions(await listAssignmentSubmissions(id));
  }, []);

  useEffect(() => {
    void loadSubmissions(assignmentId);
  }, [assignmentId, loadSubmissions]);

  const selectedAssignment = assignments.find((a) => a.id === assignmentId);

  function openGrade(sub: AssignmentSubmission) {
    setGrading(sub);
    setMarks(sub.marks ?? 0);
    setFeedback(sub.feedback ?? "");
  }

  async function submitGrade() {
    if (!grading) return;
    await gradeAssignmentSubmission({
      submissionId: grading.id,
      marks: Number(marks),
      feedback,
    });
    setGrading(null);
    await loadSubmissions(assignmentId);
  }

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Assignment Submissions
      </Typography>

      <TextField
        select
        label="Select assignment"
        value={assignmentId}
        onChange={(e) => setAssignmentId(e.target.value)}
        sx={{ minWidth: 320, mb: 2 }}
      >
        {assignments.map((a) => (
          <MenuItem key={a.id} value={a.id}>
            {a.title}
          </MenuItem>
        ))}
      </TextField>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Student ID</TableCell>
              <TableCell>Submitted</TableCell>
              <TableCell>Files</TableCell>
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
                    {s.fileUrls.length === 0 && "—"}
                    {s.fileUrls.map((url, i) => (
                      <MuiLink key={url} href={url} target="_blank" rel="noreferrer">
                        file{i + 1}
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
            {assignmentId && submissions.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No submissions for this assignment.
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
                Out of {selectedAssignment?.totalMarks ?? 100} marks
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
            Save grade
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
