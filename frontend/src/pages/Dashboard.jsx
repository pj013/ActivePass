import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../auth-context";

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
    <div className="dashboard-page">
      <header className="dashboard-topbar">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">A</span>
          <span>ActivePass</span>
        </div>
      </header>
      <main className="dashboard-main">
        <div className="dashboard-heading">
          <div>
            <p className="dashboard-eyebrow">YOUR WORKSPACE</p>
            <h1>{title}</h1>
          </div>
          <button className="secondary-button" onClick={handleLogout}>Logout</button>
        </div>
        <section className="dashboard-card">
          <div className="profile-card">
            <Link className="profile-link" to="/profile" aria-label={`View profile for ${user.fullName}`}>
              <span className="profile-avatar" aria-hidden="true">{user.fullName.charAt(0).toUpperCase()}</span>
              <span className="profile-details">
                <strong>{user.fullName}</strong>
                <span>{user.email}</span>
              </span>
            </Link>
            <span className="role-badge">{user.role}</span>
          </div>
          {error && <p className="notice notice-error" role="alert">{error}</p>}
          {data && (
            <div className="dashboard-content">
              <h2>Menu</h2>
              <ul className="dashboard-menu">{data.menu.map((item) => <li key={item}>{item}</li>)}</ul>
              <p className="dashboard-message">{data.message}</p>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
