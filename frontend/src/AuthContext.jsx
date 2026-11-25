import { useEffect, useState } from "react";
import { AuthContext } from "./auth-context";
import { api } from "./api";

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

  const updateProfile = async (data) => {
    const updatedUser = await api("/api/auth/profile", { method: "PUT", body: data });
    setUser(updatedUser);
    return updatedUser;
  };

  const changePassword = (data) =>
    api("/api/auth/change-password", { method: "POST", body: data });

  const logout = async () => {
    try { await api("/api/auth/logout", { method: "POST" }); } finally { setUser(null); }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, updateProfile, changePassword, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
