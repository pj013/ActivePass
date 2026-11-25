import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { api } from "../api";

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState(token ? "" : "This reset link is invalid or has expired.");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");
    if (password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    try {
      const result = await api("/api/auth/reset-password", {
        method: "POST",
        body: { token, newPassword: password, confirmPassword },
      });
      setMessage(result.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="brand-lockup"><span className="brand-mark" aria-hidden="true">A</span><span>ActivePass</span></div>
        <p className="auth-eyebrow">ACCOUNT RECOVERY</p>
        <h1>Set a new password</h1>
        {error && <p className="notice notice-error" role="alert">{error}</p>}
        {message ? (
          <>
            <p className="notice notice-success" role="status">{message}</p>
            <p className="auth-footer"><Link to="/login">Continue to login</Link></p>
          </>
        ) : token && (
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="form-field">
              <label htmlFor="new-password">New password (8-72 characters)</label>
              <input id="new-password" type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={8} maxLength={72} required />
            </div>
            <div className="form-field">
              <label htmlFor="confirm-password">Confirm new password</label>
              <input id="confirm-password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} maxLength={72} required />
            </div>
            <button className="primary-button" type="submit" disabled={submitting}>{submitting ? "Resetting..." : "Reset password"}</button>
          </form>
        )}
        {!token && <p className="auth-footer"><Link to="/forgot-password">Request another reset link</Link></p>}
      </section>
    </main>
  );
}
