import { Navigate, Route, Routes } from "react-router-dom";
import { Box, CircularProgress } from "@mui/material";
import { useAuth } from "./auth/AuthContext";
import { Layout } from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import SubjectsPage from "./pages/SubjectsPage";
import MaterialsPage from "./pages/MaterialsPage";
import ExamsPage from "./pages/ExamsPage";
import SubmissionsPage from "./pages/SubmissionsPage";
import AssignmentsPage from "./pages/AssignmentsPage";
import AssignmentSubmissionsPage from "./pages/AssignmentSubmissionsPage";
import ProfilePage from "./pages/ProfilePage";

export default function App() {
  const { profile, loading } = useAuth();

  if (loading) {
    return (
      <Box
        sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh" }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (!profile) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/subjects" element={<SubjectsPage />} />
        <Route path="/materials" element={<MaterialsPage />} />
        <Route path="/exams" element={<ExamsPage />} />
        <Route path="/submissions" element={<SubmissionsPage />} />
        <Route path="/assignments" element={<AssignmentsPage />} />
        <Route path="/assignment-submissions" element={<AssignmentSubmissionsPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}