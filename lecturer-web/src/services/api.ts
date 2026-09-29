/**
 * Thin HTTP client for the cPanel PHP/MySQL REST API.
 * Every endpoint returns { success, message, data }.
 * Set VITE_API_BASE in .env to where you uploaded the /api folder.
 */

const BASE: string =
  (import.meta.env.VITE_API_BASE as string | undefined)?.replace(/\/?$/, "/") ?? "/api/";

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(BASE + path, init);
  } catch {
    throw new Error("Cannot reach the server. Check VITE_API_BASE.");
  }
  let json: ApiResponse<T>;
  try {
    json = (await res.json()) as ApiResponse<T>;
  } catch {
    throw new Error(`Unexpected server response (HTTP ${res.status}).`);
  }
  if (!json.success) throw new Error(json.message || "Request failed");
  return json.data;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

/** Multipart upload to upload.php; returns the stored file's public URL. */
export async function apiUpload(
  file: File,
  folder: string,
  owner: string,
): Promise<string> {
  const fd = new FormData();
  fd.append("file", file);
  fd.append("folder", folder);
  fd.append("owner", owner);
  const data = await request<{ url: string }>("upload.php", { method: "POST", body: fd });
  return data.url;
}

/** The signed-in user's id (from the cached session), for "created_by"/"graded_by". */
export function currentUserId(): string {
  try {
    return (JSON.parse(localStorage.getItem("slems_profile") || "{}").id as string) || "";
  } catch {
    return "";
  }
}

/* ---- date helpers: JS millis <-> MySQL DATETIME strings ---- */
export function toSqlDate(ms: number): string {
  if (!ms) return "";
  const d = new Date(ms);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

export function fromSqlDate(s: string | null | undefined): number {
  if (!s) return 0;
  const t = new Date(s.replace(" ", "T")).getTime();
  return isNaN(t) ? 0 : t;
}
