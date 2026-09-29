import { useCallback, useEffect, useState } from "react";
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import { useAuth } from "../auth/AuthContext";
import {
  createExam,
  listExamsByLecturer,
  listSubjectsByLecturer,
  setExamPublished,
} from "../services/data";
import type { Exam, Subject } from "../types";

export default function ExamsPage() {
  const { profile } = useAuth();
  const [exams, setExams] = useState<Exam[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [open, setOpen] = useState(false);

  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startLocal, setStartLocal] = useState("");
  const [deadlineLocal, setDeadlineLocal] = useState("");
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [totalMarks, setTotalMarks] = useState(100);
  const [paperFile, setPaperFile] = useState<File | null>(null);

  const refresh = useCallback(async () => {
    if (!profile) return;
    const [ex, subs] = await Promise.all([
      listExamsByLecturer(profile.id),
      listSubjectsByLecturer(profile.id),
    ]);
    setExams(ex);
    setSubjects(subs);
  }, [profile]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const subjectName = (id: string) => subjects.find((s) => s.id === id)?.name ?? "—";

  async function handleCreate() {
    if (!profile) return;
    await createExam(
      {
        subjectId,
        title,
        description,
        startAt: startLocal ? new Date(startLocal).getTime() : 0,
        deadline: deadlineLocal ? new Date(deadlineLocal).getTime() : 0,
        durationMinutes: Number(durationMinutes),
        totalMarks: Number(totalMarks),
        createdBy: profile.id,
      },
      paperFile ?? undefined,
    );
    setSubjectId("");
    setTitle("");
    setDescription("");
    setStartLocal("");
    setDeadlineLocal("");
    setDurationMinutes(60);
    setTotalMarks(100);
    setPaperFile(null);
    setOpen(false);
    await refresh();
  }

  async function togglePublish(exam: Exam) {
    await setExamPublished(exam.id, !exam.published);
    await refresh();
  }

  return (
    <>
      <Box sx={{ display: "flex", justifyContent: "space-between", mb: 2 }}>
        <Typography variant="h4">Exams</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          Create exam
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Subject</TableCell>
              <TableCell>Starts</TableCell>
              <TableCell>Deadline</TableCell>
              <TableCell>Paper</TableCell>
              <TableCell>Marks</TableCell>
              <TableCell>Published</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {exams.map((e) => (
              <TableRow key={e.id}>
                <TableCell>{e.title}</TableCell>
                <TableCell>{subjectName(e.subjectId)}</TableCell>
                <TableCell>
                  {e.startAt ? new Date(e.startAt).toLocaleString() : "—"}
                </TableCell>
                <TableCell>
                  {e.deadline ? new Date(e.deadline).toLocaleString() : "—"}
                </TableCell>
                <TableCell>
                  {e.paperUrl ? (
                    <a href={e.paperUrl} target="_blank" rel="noreferrer">
                      open
                    </a>
                  ) : (
                    "—"
                  )}
                </TableCell>
                <TableCell>{e.totalMarks}</TableCell>
                <TableCell>
                  <Switch checked={e.published} onChange={() => void togglePublish(e)} />
                  <Chip
                    size="small"
                    label={e.published ? "Live" : "Draft"}
                    color={e.published ? "success" : "default"}
                  />
                </TableCell>
              </TableRow>
            ))}
            {exams.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  No exams yet.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create exam</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              label="Subject"
              value={subjectId}
              onChange={(e) => setSubjectId(e.target.value)}
            >
              {subjects.map((s) => (
                <MenuItem key={s.id} value={s.id}>
                  {s.code} — {s.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
            <TextField
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              multiline
              minRows={2}
            />
            <TextField
              label="Start date & time"
              type="datetime-local"
              value={startLocal}
              onChange={(e) => setStartLocal(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label="Answer submission deadline"
              type="datetime-local"
              value={deadlineLocal}
              onChange={(e) => setDeadlineLocal(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <Button variant="outlined" component="label">
              {paperFile ? `Paper: ${paperFile.name}` : "Upload exam paper (PDF)"}
              <input
                hidden
                type="file"
                accept="application/pdf,image/*"
                onChange={(e) => setPaperFile(e.target.files?.[0] ?? null)}
              />
            </Button>
            <TextField
              label="Duration (minutes)"
              type="number"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(Number(e.target.value))}
            />
            <TextField
              label="Total marks"
              type="number"
              value={totalMarks}
              onChange={(e) => setTotalMarks(Number(e.target.value))}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!subjectId || !title}
            onClick={() => void handleCreate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
