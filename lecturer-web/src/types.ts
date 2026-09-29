// These mirror the Android app's Firestore data classes field-for-field so both
// clients read/write the same documents. Keep them in sync.

export type UserRole = "STUDENT" | "LECTURER" | "ADMIN";

export type MaterialType = "PDF" | "PPT" | "NOTE" | "VIDEO";

export type SubmissionStatus = "SUBMITTED" | "GRADED";

export interface UserProfile {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  phone: string;
  active: boolean;
  photoUrl: string;
  createdAt: number;
}

export interface Subject {
  id: string;
  code: string;
  name: string;
  description: string;
  lecturerId: string;
  createdAt: number;
}

export interface LearningMaterial {
  id: string;
  subjectId: string;
  title: string;
  description: string;
  type: MaterialType;
  fileUrl: string;
  uploadedBy: string;
  approved: boolean;
  createdAt: number;
}

export interface Exam {
  id: string;
  subjectId: string;
  title: string;
  description: string;
  startAt: number;
  /** Answer-sheet submission deadline (epoch millis). */
  deadline: number;
  /** Uploaded exam paper download URL (empty if none). */
  paperUrl: string;
  durationMinutes: number;
  totalMarks: number;
  createdBy: string;
  published: boolean;
}

export interface ExamSubmission {
  id: string;
  examId: string;
  studentId: string;
  answerSheetUrls: string[];
  submittedAt: number;
  status: SubmissionStatus;
  marks: number | null;
  feedback: string;
}

export interface Assignment {
  id: string;
  subjectId: string;
  title: string;
  description: string;
  dueAt: number;
  totalMarks: number;
  createdBy: string;
}

export interface AssignmentSubmission {
  id: string;
  assignmentId: string;
  studentId: string;
  fileUrls: string[];
  submittedAt: number;
  status: SubmissionStatus;
  marks: number | null;
  feedback: string;
}

export interface Result {
  id: string;
  studentId: string;
  subjectId: string;
  examId: string;
  marks: number;
  totalMarks: number;
  grade: string;
  publishedAt: number;
}

// Firestore collection names (shared with the Android client).
export const Collections = {
  users: "users",
  subjects: "subjects",
  materials: "materials",
  exams: "exams",
  examSubmissions: "examSubmissions",
  assignments: "assignments",
  assignmentSubmissions: "assignmentSubmissions",
  results: "results",
  notifications: "notifications",
} as const;
