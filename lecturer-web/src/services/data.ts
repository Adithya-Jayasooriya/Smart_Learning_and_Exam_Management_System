import {
  apiGet,
  apiPost,
  apiUpload,
  currentUserId,
  fromSqlDate,
  toSqlDate,
} from "./api";
import type {
  Assignment,
  AssignmentSubmission,
  Exam,
  ExamSubmission,
  LearningMaterial,
  MaterialType,
  Subject,
} from "./../types";

/**
 * Data access for the cPanel PHP/MySQL API. Function names & return types are the
 * same the pages already use; only the implementation changed from Firestore to
 * HTTP. Snake_case rows from PHP are mapped to the camelCase app types here.
 */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Row = Record<string, any>;

/* ---------------- mappers ---------------- */

const mapSubject = (r: Row): Subject => ({
  id: String(r.id),
  code: r.code ?? "",
  name: r.name ?? "",
  description: r.description ?? "",
  lecturerId: r.lecturer_id != null ? String(r.lecturer_id) : "",
  createdAt: fromSqlDate(r.created_at),
});

const mapMaterial = (r: Row): LearningMaterial => ({
  id: String(r.id),
  subjectId: String(r.subject_id),
  title: r.title ?? "",
  description: r.description ?? "",
  type: (r.type ?? "PDF") as MaterialType,
  fileUrl: r.file_url ?? "",
  uploadedBy: r.uploaded_by != null ? String(r.uploaded_by) : "",
  approved: Number(r.approved ?? 1) === 1,
  createdAt: fromSqlDate(r.created_at),
});

const mapExam = (r: Row): Exam => ({
  id: String(r.id),
  subjectId: String(r.subject_id),
  title: r.title ?? "",
  description: r.description ?? "",
  startAt: fromSqlDate(r.exam_date),
  deadline: fromSqlDate(r.deadline),
  paperUrl: r.paper_url ?? "",
  durationMinutes: Number(r.duration_minutes ?? 60),
  totalMarks: 100,
  createdBy: r.created_by != null ? String(r.created_by) : "",
  published: true,
});

const mapExamSubmission = (r: Row): ExamSubmission => ({
  id: String(r.id),
  examId: String(r.exam_id),
  studentId: String(r.student_id),
  answerSheetUrls: r.file_url ? [String(r.file_url)] : [],
  submittedAt: fromSqlDate(r.submitted_at),
  status: String(r.status ?? "submitted").toUpperCase() as ExamSubmission["status"],
  marks: r.marks == null ? null : Number(r.marks),
  feedback: r.feedback ?? "",
});

const mapAssignment = (r: Row): Assignment => ({
  id: String(r.id),
  subjectId: String(r.subject_id),
  title: r.title ?? "",
  description: r.description ?? "",
  dueAt: fromSqlDate(r.due_date),
  totalMarks: 100,
  createdBy: r.created_by != null ? String(r.created_by) : "",
});

const mapAssignmentSubmission = (r: Row): AssignmentSubmission => ({
  id: String(r.id),
  assignmentId: String(r.assignment_id),
  studentId: String(r.student_id),
  fileUrls: r.file_url ? [String(r.file_url)] : [],
  submittedAt: fromSqlDate(r.submitted_at),
  status: String(r.status ?? "submitted").toUpperCase() as AssignmentSubmission["status"],
  marks: r.marks == null ? null : Number(r.marks),
  feedback: r.feedback ?? "",
});

/* ---------------- Profile ---------------- */

export async function updateUserProfile(
  uid: string,
  data: { fullName: string; phone: string },
): Promise<void> {
  await apiPost("users.php?action=update", { id: uid, name: data.fullName, phone: data.phone });
}

/* ---------------- Subjects ---------------- */

export async function listSubjectsByLecturer(lecturerId: string): Promise<Subject[]> {
  const rows = await apiGet<Row[]>(`subjects.php?lecturer_id=${lecturerId}`);
  return rows.map(mapSubject);
}

export async function createSubject(
  data: Omit<Subject, "id" | "createdAt">,
): Promise<void> {
  await apiPost("subjects.php?action=add", {
    code: data.code, name: data.name, description: data.description, lecturer_id: data.lecturerId,
  });
}

export async function deleteSubject(id: string): Promise<void> {
  await apiPost("subjects.php?action=delete", { id });
}

/* ---------------- Learning materials ---------------- */

export async function listMaterialsByLecturer(uploadedBy: string): Promise<LearningMaterial[]> {
  const rows = await apiGet<Row[]>("materials.php");
  return rows.filter((r) => String(r.uploaded_by) === uploadedBy).map(mapMaterial);
}

interface NewMaterial {
  subjectId: string;
  title: string;
  description: string;
  type: MaterialType;
  uploadedBy: string;
  videoUrl?: string;
  file?: File;
}

export async function createMaterial(input: NewMaterial): Promise<void> {
  let fileUrl = input.videoUrl ?? "";
  if (input.file) fileUrl = await apiUpload(input.file, "materials", input.uploadedBy);
  await apiPost("materials.php?action=add", {
    subject_id: input.subjectId, title: input.title, type: input.type,
    file_url: fileUrl, uploaded_by: input.uploadedBy,
  });
}

export async function deleteMaterial(material: LearningMaterial): Promise<void> {
  await apiPost("materials.php?action=delete", { id: material.id });
}

/* ---------------- Exams ---------------- */

export async function listExamsByLecturer(createdBy: string): Promise<Exam[]> {
  const rows = await apiGet<Row[]>("exams.php");
  return rows.filter((r) => String(r.created_by) === createdBy).map(mapExam);
}

export async function createExam(
  data: Omit<Exam, "id" | "published" | "paperUrl">,
  paperFile?: File,
): Promise<void> {
  let paperUrl = "";
  if (paperFile) paperUrl = await apiUpload(paperFile, "examPapers", data.createdBy);
  await apiPost("exams.php?action=add", {
    subject_id: data.subjectId, title: data.title, description: data.description,
    exam_date: toSqlDate(data.startAt), deadline: toSqlDate(data.deadline),
    paper_url: paperUrl, duration_minutes: data.durationMinutes, created_by: data.createdBy,
  });
}

// The MySQL exams table has no "published" column — publishing is implicit.
export async function setExamPublished(_id: string, _published: boolean): Promise<void> {
  return Promise.resolve();
}

/* ---------------- Assignments ---------------- */

export async function listAssignmentsByLecturer(createdBy: string): Promise<Assignment[]> {
  const rows = await apiGet<Row[]>("assignments.php");
  return rows.filter((r) => String(r.created_by) === createdBy).map(mapAssignment);
}

export async function createAssignment(data: Omit<Assignment, "id">): Promise<void> {
  await apiPost("assignments.php?action=add", {
    subject_id: data.subjectId, title: data.title, description: data.description,
    due_date: toSqlDate(data.dueAt), created_by: data.createdBy,
  });
}

export async function listAssignmentSubmissions(assignmentId: string): Promise<AssignmentSubmission[]> {
  const rows = await apiGet<Row[]>(`assignments.php?assignment_id=${assignmentId}&submissions=1`);
  return rows.map(mapAssignmentSubmission).sort((a, b) => b.submittedAt - a.submittedAt);
}

export async function gradeAssignmentSubmission(args: {
  submissionId: string;
  marks: number;
  feedback: string;
}): Promise<void> {
  await apiPost("assignments.php?action=grade", { id: args.submissionId, marks: args.marks });
}

/* ---------------- Exam submissions / grading ---------------- */

export async function listSubmissionsByExam(examId: string): Promise<ExamSubmission[]> {
  const rows = await apiGet<Row[]>(`submissions.php?exam_id=${examId}`);
  return rows.map(mapExamSubmission).sort((a, b) => b.submittedAt - a.submittedAt);
}

export async function gradeSubmission(args: {
  submissionId: string;
  studentId: string;
  exam: Exam;
  marks: number;
  feedback: string;
}): Promise<void> {
  await apiPost("submissions.php?action=grade", {
    id: args.submissionId, marks: args.marks, graded_by: currentUserId(),
  });
}
