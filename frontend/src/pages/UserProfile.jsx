import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth-context";

export default function UserProfile() {
  const { user, updateProfile, changePassword } = useAuth();
  const [profile, setProfile] = useState({ fullName: user.fullName, email: user.email });
  const [profileMessage, setProfileMessage] = useState("");
  const [profileError, setProfileError] = useState("");
  const [password, setPassword] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [passwordMessage, setPasswordMessage] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  const handleProfileSubmit = async (event) => {
    event.preventDefault();
    setProfileError("");
    setProfileMessage("");
    setSavingProfile(true);
    try {
      const updatedUser = await updateProfile(profile);
      setProfile({ fullName: updatedUser.fullName, email: updatedUser.email });
      setProfileMessage("Profile updated.");
    } catch (err) {
      setProfileError(err.message);
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (event) => {
    event.preventDefault();
    setPasswordError("");
    setPasswordMessage("");
    if (password.newPassword !== password.confirmPassword) {
      setPasswordError("New passwords do not match.");
      return;
    }

    setSavingPassword(true);
    try {
      const result = await changePassword(password);
      setPassword({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setPasswordMessage(result.message);
    } catch (err) {
      setPasswordError(err.message);
    } finally {
      setSavingPassword(false);
    }
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
            <p className="dashboard-eyebrow">ACCOUNT SETTINGS</p>
            <h1>User Profile</h1>
          </div>
          <Link className="secondary-button profile-back-button" to={user.dashboardPath}>Back to dashboard</Link>
        </div>
        <section className="settings-grid" aria-label="Profile settings">
          <div className="dashboard-card settings-card">
            <h2>Profile information</h2>
            <p className="profile-role">Account type: <strong>{user.role}</strong></p>
            {profileError && <p className="notice notice-error" role="alert">{profileError}</p>}
            {profileMessage && <p className="notice notice-success" role="status">{profileMessage}</p>}
            <form className="auth-form" onSubmit={handleProfileSubmit}>
              <div className="form-field">
                <label htmlFor="profile-name">Full name</label>
                <input id="profile-name" value={profile.fullName} onChange={(event) => setProfile({ ...profile, fullName: event.target.value })} maxLength={100} required />
              </div>
              <div className="form-field">
                <label htmlFor="profile-email">Email</label>
                <input id="profile-email" type="email" autoComplete="email" value={profile.email} onChange={(event) => setProfile({ ...profile, email: event.target.value })} maxLength={254} required />
              </div>
              <button className="primary-button" type="submit" disabled={savingProfile}>{savingProfile ? "Saving..." : "Save profile"}</button>
            </form>
          </div>
          <div className="dashboard-card settings-card">
            <h2>Change password</h2>
            {passwordError && <p className="notice notice-error" role="alert">{passwordError}</p>}
            {passwordMessage && <p className="notice notice-success" role="status">{passwordMessage}</p>}
            <form className="auth-form" onSubmit={handlePasswordSubmit}>
              <div className="form-field">
                <label htmlFor="current-password">Current password</label>
                <input id="current-password" type="password" autoComplete="current-password" value={password.currentPassword} onChange={(event) => setPassword({ ...password, currentPassword: event.target.value })} required />
              </div>
              <div className="form-field">
                <label htmlFor="new-password">New password (8-72 characters)</label>
                <input id="new-password" type="password" autoComplete="new-password" value={password.newPassword} onChange={(event) => setPassword({ ...password, newPassword: event.target.value })} minLength={8} maxLength={72} required />
              </div>
              <div className="form-field">
                <label htmlFor="confirm-new-password">Confirm new password</label>
                <input id="confirm-new-password" type="password" autoComplete="new-password" value={password.confirmPassword} onChange={(event) => setPassword({ ...password, confirmPassword: event.target.value })} minLength={8} maxLength={72} required />
              </div>
              <button className="primary-button" type="submit" disabled={savingPassword}>{savingPassword ? "Changing..." : "Change password"}</button>
            </form>
          </div>
        </section>
      </main>
    </div>
  );
}
