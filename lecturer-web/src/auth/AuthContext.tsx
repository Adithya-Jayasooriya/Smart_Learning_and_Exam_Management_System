import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiPost } from "../services/api";
import type { UserProfile, UserRole } from "../types";

const STORAGE_KEY = "slems_profile";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function toProfile(u: Record<string, any>): UserProfile {
  return {
    id: String(u.id),
    fullName: u.name ?? "",
    email: u.email ?? "",
    role: String(u.role ?? "student").toUpperCase() as UserRole,
    phone: u.phone ?? "",
    active: Number(u.active ?? 1) === 1,
    photoUrl: "",
    createdAt: 0,
  };
}

interface AuthContextValue {
  profile: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  resetPassword: (email: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  // Restore the cached session on load.
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) setProfile(JSON.parse(raw) as UserProfile);
    } catch {
      /* ignore */
    }
    setLoading(false);
  }, []);

  async function login(email: string, password: string) {
    const user = await apiPost<Record<string, unknown>>("auth.php?action=login", {
      email: email.trim(),
      password,
    });
    const loaded = toProfile(user);
    if (loaded.role !== "LECTURER" && loaded.role !== "ADMIN") {
      throw new Error("This account is not allowed to use the lecturer portal.");
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(loaded));
    setProfile(loaded);
  }

  async function logout() {
    localStorage.removeItem(STORAGE_KEY);
    setProfile(null);
  }

  async function resetPassword(_email: string) {
    throw new Error("Please contact the administrator to reset your password.");
  }

  const value = useMemo(
    () => ({ profile, loading, login, logout, resetPassword }),
    [profile, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
