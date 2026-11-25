import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");
    setSubmitting(true);
    try {
      const result = await api("/api/auth/forgot-password", { method: "POST", body: { email } });
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
        <h1>Forgot password?</h1>
        <p className="page-description">Enter your account email and we'll send a secure password reset link if an account exists.</p>
        {error && <p className="notice notice-error" role="alert">{error}</p>}
        {message && <p className="notice notice-success" role="status">{message}</p>}
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </div>
          <button className="primary-button" type="submit" disabled={submitting}>{submitting ? "Sending..." : "Send reset link"}</button>
        </form>
        <p className="auth-footer"><Link to="/login">Back to login</Link></p>
      </section>
    </main>
  );
}
