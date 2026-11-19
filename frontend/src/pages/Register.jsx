import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ firstName: "", lastName: "", email: "", password: "", role: "STUDENT" });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
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
    <div>
      <h1>Register</h1>
      {error && <p style={{ color: "red" }}>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div>
          <label>First Name</label><br />
          <input name="firstName" autoComplete="given-name" value={form.firstName} onChange={update} required />
        </div>
        <div>
          <label>Last Name</label><br />
          <input name="lastName" autoComplete="family-name" value={form.lastName} onChange={update} required />
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
