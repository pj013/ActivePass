import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "", confirmPassword: "", role: "STUDENT" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setSubmitting(true);
    try {
      const { firstName, lastName, email, password, role } = form;
      await register({
        fullName: `${firstName.trim()} ${lastName.trim()}`,
        email,
        password,
        role,
      });
      navigate("/login", { state: { message: "Registration successful. Please log in." } });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">A</span>
          <span>ActivePass</span>
        </div>
        <p className="auth-eyebrow">GET STARTED</p>
        <h1>Register</h1>
        {error && <p className="notice notice-error" role="alert">{error}</p>}
        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="firstName">First Name</label>
            <input id="firstName" name="firstName" autoComplete="given-name" value={form.firstName} onChange={update} required />
          </div>
          <div className="form-field">
            <label htmlFor="lastName">Last Name</label>
            <input id="lastName" name="lastName" autoComplete="family-name" value={form.lastName} onChange={update} required />
          </div>
          <div className="form-field">
            <label htmlFor="email">Email</label>
            <input id="email" name="email" type="email" autoComplete="email" value={form.email} onChange={update} required />
          </div>
          <div className="form-field">
            <label htmlFor="password">Password (min 8 characters)</label>
            <input id="password" name="password" type="password" autoComplete="new-password" value={form.password} onChange={update} required minLength={8} />
          </div>
          <div className="form-field">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" autoComplete="new-password" value={form.confirmPassword} onChange={update} required minLength={8} />
          </div>
          <div className="form-field">
            <label htmlFor="role">I am a</label>
            <select id="role" name="role" value={form.role} onChange={update}>
              <option value="STUDENT">Student</option>
              <option value="ORGANIZER">Organizer</option>
            </select>
          </div>
          <button className="primary-button" type="submit" disabled={submitting}>{submitting ? "Registering..." : "Register"}</button>
        </form>
        <p className="auth-footer">Already have an account? <Link to="/login">Login</Link></p>
      </section>
    </main>
  );
}
