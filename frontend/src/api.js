const BASE = "http://localhost:8080";

// Spring needs a fresh CSRF token for every POST/PUT/DELETE
async function getCsrf() {
  const res = await fetch(`${BASE}/api/auth/csrf`, { credentials: "include" });
  return res.json(); // { headerName, token }
}

export async function api(path, { method = "GET", body } = {}) {
  const headers = {};
  if (body) headers["Content-Type"] = "application/json";
  if (method !== "GET") {
    const csrf = await getCsrf();
    headers[csrf.headerName] = csrf.token;
  }

  const res = await fetch(BASE + path, {
    method,
    headers,
    credentials: "include", // sends/receives the session cookie
    body: body ? JSON.stringify(body) : undefined,
  });

  let data = null;
  try { data = await res.json(); } catch { /* empty body */ }

  if (!res.ok) {
    const err = new Error(data?.error || "Something went wrong");
    err.status = res.status;
    throw err;
  }
  return data;
}
