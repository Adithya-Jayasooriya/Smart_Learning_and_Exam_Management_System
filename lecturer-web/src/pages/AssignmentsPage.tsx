import { useCallback, useEffect, useState } from "react";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import AddIcon from "@mui/icons-material/Add";
import { useAuth } from "../auth/AuthContext";
import {
  createAssignment,
  listAssignmentsByLecturer,
  listSubjectsByLecturer,
} from "../services/data";
import type { Assignment, Subject } from "../types";

export default function AssignmentsPage() {
  const { profile } = useAuth();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [open, setOpen] = useState(false);

  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [dueLocal, setDueLocal] = useState("");
  const [totalMarks, setTotalMarks] = useState(100);

  const refresh = useCallback(async () => {
    if (!profile) return;
    const [a, subs] = await Promise.all([
      listAssignmentsByLecturer(profile.id),
      listSubjectsByLecturer(profile.id),
    ]);
    setAssignments(a);
    setSubjects(subs);
  }, [profile]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const subjectName = (id: string) => subjects.find((s) => s.id === id)?.name ?? "—";

  async function handleCreate() {
    if (!profile) return;
    await createAssignment({
      subjectId,
      title,
      description,
      dueAt: dueLocal ? new Date(dueLocal).getTime() : 0,
      totalMarks: Number(totalMarks),
      createdBy: profile.id,
    });
    setSubjectId("");
    setTitle("");
    setDescription("");
    setDueLocal("");
    setTotalMarks(100);
    setOpen(false);
    await refresh();
  }

  return (
    <>
      <Box sx={{ display: "flex", justifyContent: "space-between", mb: 2 }}>
        <Typography variant="h4">Assignments</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          Create assignment
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Subject</TableCell>
              <TableCell>Due</TableCell>
              <TableCell>Marks</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {assignments.map((a) => (
              <TableRow key={a.id}>
                <TableCell>{a.title}</TableCell>
                <TableCell>{subjectName(a.subjectId)}</TableCell>
                <TableCell>{a.dueAt ? new Date(a.dueAt).toLocaleString() : "—"}</TableCell>
                <TableCell>{a.totalMarks}</TableCell>
              </TableRow>
            ))}
            {assignments.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  No assignments yet.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create assignment</DialogTitle>
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
              label="Due date & time"
              type="datetime-local"
              value={dueLocal}
              onChange={(e) => setDueLocal(e.target.value)}
              InputLabelProps={{ shrink: true }}
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
