import { useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Link as MuiLink,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import SchoolIcon from "@mui/icons-material/School";
import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
  const { login, resetPassword } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setInfo(null);
    try {
      await login(email, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setBusy(false);
    }
  }

  async function handleReset() {
    if (!email) {
      setError("Enter your email first, then click reset.");
      return;
    }
    setError(null);
    try {
      await resetPassword(email);
      setInfo("Password reset email sent.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not send reset email");
    }
  }

  return (
    <Box
      sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh" }}
    >
      <Card sx={{ width: 380 }}>
        <CardContent>
          <Stack spacing={2} alignItems="center" sx={{ mb: 1 }}>
            <SchoolIcon color="primary" sx={{ fontSize: 56 }} />
            <Typography variant="h5">Lecturer Portal</Typography>
            <Typography variant="body2" color="text.secondary">
              Smart Learning &amp; Exam Management
            </Typography>
          </Stack>

          <form onSubmit={handleLogin}>
            <Stack spacing={2} sx={{ mt: 2 }}>
              {error && <Alert severity="error">{error}</Alert>}
              {info && <Alert severity="success">{info}</Alert>}
              <TextField
                label="Email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                fullWidth
              />
              <TextField
                label="Password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                fullWidth
              />
              <Button type="submit" variant="contained" disabled={busy} fullWidth>
                {busy ? <CircularProgress size={22} /> : "Log in"}
              </Button>
              <MuiLink
                component="button"
                type="button"
                onClick={handleReset}
                sx={{ alignSelf: "center" }}
              >
                Forgot password?
              </MuiLink>
            </Stack>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
}
