import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", password: "", role: "STUDENT" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await register(form);
      navigate("/login", { state: { message: "Registration successful. Please log in." } });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <h1>Register</h1>
      {error && <p style={{ color: "red" }}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div>
          <label>Full name</label><br />
          <input name="fullName" value={form.fullName} onChange={update} required />
        </div>
        <div>
          <label>Email</label><br />
          <input name="email" type="email" value={form.email} onChange={update} required />
        </div>
        <div>
          <label>Password (min 8 characters)</label><br />
          <input name="password" type="password" value={form.password} onChange={update} required minLength={8} />
        </div>
        <div>
          <label>I am a</label><br />
          <select name="role" value={form.role} onChange={update}>
            <option value="STUDENT">Student</option>
            <option value="ORGANIZER">Organizer</option>
          </select>
        </div>
        <button type="submit" disabled={submitting}>{submitting ? "Registering..." : "Register"}</button>
      </form>
      <p>Already have an account? <Link to="/login">Login</Link></p>
    </div>
  );
}
