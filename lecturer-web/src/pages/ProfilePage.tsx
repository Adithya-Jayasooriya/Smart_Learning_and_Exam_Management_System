import { useState } from "react";
import {
  Alert,
  Button,
  Card,
  CardContent,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import { useAuth } from "../auth/AuthContext";
import { updateUserProfile } from "../services/data";

export default function ProfilePage() {
  const { profile } = useAuth();
  const [fullName, setFullName] = useState(profile?.fullName ?? "");
  const [phone, setPhone] = useState(profile?.phone ?? "");
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  async function handleSave() {
    if (!profile) return;
    setBusy(true);
    setSaved(false);
    try {
      await updateUserProfile(profile.id, { fullName, phone });
      setSaved(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Typography variant="h4" gutterBottom>
        My Profile
      </Typography>
      <Card sx={{ maxWidth: 480 }}>
        <CardContent>
          <Stack spacing={2}>
            {saved && <Alert severity="success">Profile updated.</Alert>}
            <TextField label="Email" value={profile?.email ?? ""} disabled />
            <TextField
              label="Full name"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
            <TextField
              label="Phone"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
            <Button
              variant="contained"
              disabled={busy || !fullName}
              onClick={() => void handleSave()}
            >
              {busy ? "Saving…" : "Save changes"}
            </Button>
          </Stack>
        </CardContent>
      </Card>
    </>
  );
}
