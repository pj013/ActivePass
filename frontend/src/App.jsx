import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth-context";
import ProtectedRoute from "./components/ProtectedRoute";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import ForgotPassword from "./pages/ForgotPassword";
import ResetPassword from "./pages/ResetPassword";
import UserProfile from "./pages/UserProfile";

function Home() {
  const { user, loading } = useAuth();
  if (loading) return <p>Loading...</p>;
  return <Navigate to={user ? user.dashboardPath : "/login"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/profile" element={
        <ProtectedRoute>
          <UserProfile />
        </ProtectedRoute>
      } />

      <Route path="/student/dashboard" element={
        <ProtectedRoute roles={["STUDENT"]}>
          <Dashboard title="Student Dashboard" apiPath="/api/student/dashboard" />
        </ProtectedRoute>
      } />
      <Route path="/organizer/dashboard" element={
        <ProtectedRoute roles={["ORGANIZER"]}>
          <Dashboard title="Organizer Dashboard" apiPath="/api/organizer/dashboard" />
        </ProtectedRoute>
      } />
      <Route path="/admin/dashboard" element={
        <ProtectedRoute roles={["ADMINISTRATOR"]}>
          <Dashboard title="Administrator Dashboard" apiPath="/api/admin/dashboard" />
        </ProtectedRoute>
      } />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}