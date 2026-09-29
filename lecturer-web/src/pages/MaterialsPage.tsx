import { useCallback, useEffect, useState } from "react";
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
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
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import { useAuth } from "../auth/AuthContext";
import {
  createMaterial,
  deleteMaterial,
  listMaterialsByLecturer,
  listSubjectsByLecturer,
} from "../services/data";
import type { LearningMaterial, MaterialType, Subject } from "../types";

const TYPES: MaterialType[] = ["PDF", "PPT", "NOTE", "VIDEO"];

export default function MaterialsPage() {
  const { profile } = useAuth();
  const [materials, setMaterials] = useState<LearningMaterial[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  // Form state
  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState<MaterialType>("PDF");
  const [videoUrl, setVideoUrl] = useState("");
  const [file, setFile] = useState<File | null>(null);

  const refresh = useCallback(async () => {
    if (!profile) return;
    const [mats, subs] = await Promise.all([
      listMaterialsByLecturer(profile.id),
      listSubjectsByLecturer(profile.id),
    ]);
    setMaterials(mats);
    setSubjects(subs);
  }, [profile]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const subjectName = (id: string) =>
    subjects.find((s) => s.id === id)?.name ?? "—";

  async function handleCreate() {
    if (!profile) return;
    setBusy(true);
    try {
      await createMaterial({
        subjectId,
        title,
        description,
        type,
        uploadedBy: profile.id,
        videoUrl: type === "VIDEO" ? videoUrl : undefined,
        file: type === "VIDEO" ? undefined : file ?? undefined,
      });
      setSubjectId("");
      setTitle("");
      setDescription("");
      setType("PDF");
      setVideoUrl("");
      setFile(null);
      setOpen(false);
      await refresh();
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(m: LearningMaterial) {
    await deleteMaterial(m);
    await refresh();
  }

  const canSave =
    !!subjectId && !!title && (type === "VIDEO" ? !!videoUrl : !!file);

  return (
    <>
      <Box sx={{ display: "flex", justifyContent: "space-between", mb: 2 }}>
        <Typography variant="h4">Learning Materials</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          Upload material
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Subject</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>File</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {materials.map((m) => (
              <TableRow key={m.id}>
                <TableCell>{m.title}</TableCell>
                <TableCell>{subjectName(m.subjectId)}</TableCell>
                <TableCell>
                  <Chip label={m.type} size="small" />
                </TableCell>
                <TableCell>
                  {m.fileUrl ? (
                    <MuiLink href={m.fileUrl} target="_blank" rel="noreferrer">
                      Open
                    </MuiLink>
                  ) : (
                    "—"
                  )}
                </TableCell>
                <TableCell align="right">
                  <IconButton color="error" onClick={() => void handleDelete(m)}>
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
            {materials.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  No materials yet.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Upload material</DialogTitle>
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
              select
              label="Type"
              value={type}
              onChange={(e) => setType(e.target.value as MaterialType)}
            >
              {TYPES.map((t) => (
                <MenuItem key={t} value={t}>
                  {t}
                </MenuItem>
              ))}
            </TextField>

            {type === "VIDEO" ? (
              <TextField
                label="Video link (e.g. YouTube URL)"
                value={videoUrl}
                onChange={(e) => setVideoUrl(e.target.value)}
              />
            ) : (
              <Button variant="outlined" component="label">
                {file ? file.name : "Choose file"}
                <input
                  hidden
                  type="file"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                />
              </Button>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!canSave || busy}
            onClick={() => void handleCreate()}
          >
            {busy ? "Uploading…" : "Save"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
