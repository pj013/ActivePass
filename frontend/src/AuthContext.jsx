import { createContext, useContext, useEffect, useState } from "react";
import { api } from "./api";

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Restore the session after a page refresh
  useEffect(() => {
    api("/api/auth/me")
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = async (email, password) => {
    const u = await api("/api/auth/login", { method: "POST", body: { email, password } });
    setUser(u);
    return u;
  };

  const register = (data) => api("/api/auth/register", { method: "POST", body: data });

  const logout = async () => {
    try { await api("/api/auth/logout", { method: "POST" }); } finally { setUser(null); }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
