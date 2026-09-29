import { useEffect, useState } from "react";
import { Card, CardContent, Grid, Typography } from "@mui/material";
import { useAuth } from "../auth/AuthContext";
import {
  listAssignmentsByLecturer,
  listExamsByLecturer,
  listMaterialsByLecturer,
  listSubjectsByLecturer,
} from "../services/data";

interface Counts {
  subjects: number;
  materials: number;
  exams: number;
  assignments: number;
}

export default function DashboardPage() {
  const { profile } = useAuth();
  const [counts, setCounts] = useState<Counts | null>(null);

  useEffect(() => {
    if (!profile) return;
    void (async () => {
      const [subjects, materials, exams, assignments] = await Promise.all([
        listSubjectsByLecturer(profile.id),
        listMaterialsByLecturer(profile.id),
        listExamsByLecturer(profile.id),
        listAssignmentsByLecturer(profile.id),
      ]);
      setCounts({
        subjects: subjects.length,
        materials: materials.length,
        exams: exams.length,
        assignments: assignments.length,
      });
    })();
  }, [profile]);

  const cards = [
    { label: "Subjects", value: counts?.subjects },
    { label: "Materials", value: counts?.materials },
    { label: "Exams", value: counts?.exams },
    { label: "Assignments", value: counts?.assignments },
  ];

  return (
    <>
      <Typography variant="h4" gutterBottom>
        Welcome, {profile?.fullName}
      </Typography>
      <Grid container spacing={2}>
        {cards.map((c) => (
          <Grid item xs={12} sm={6} md={3} key={c.label}>
            <Card>
              <CardContent>
                <Typography variant="h3">{c.value ?? "—"}</Typography>
                <Typography color="text.secondary">{c.label}</Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
}
