import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../AuthContext";

// One component reused by all three roles; apiPath decides which backend endpoint it calls
export default function Dashboard({ title, apiPath }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api(apiPath).then(setData).catch((err) => setError(err.message));
  }, [apiPath]);

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  return (
    <div>
      <h1>{title}</h1>
      <p>Logged in as {user.fullName} ({user.email}), role: {user.role}</p>
      <button onClick={handleLogout}>Logout</button>

      {error && <p style={{ color: "red" }}>{error}</p>}
      {data && (
        <>
          <h3>Menu</h3>
          <ul>{data.menu.map((item) => <li key={item}>{item}</li>)}</ul>
          <p>{data.message}</p>
        </>
      )}
    </div>
  );
}
