import { BrowserRouter, Navigate, Route, Routes, useParams } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import ProtectedRoute from "../components/ProtectedRoute";
import { AuthProvider } from "../context/AuthContext";
import { ToastProvider } from "../context/ToastContext";
import Dashboard from "../pages/Dashboard";
import HealthCheckPage from "../pages/HealthCheckPage";
import HomePage from "../pages/HomePage";
import Login from "../pages/Login";
import MyTasks from "../pages/MyTasks";
import NotFound from "../pages/NotFound";
import Notifications from "../pages/Notifications";
import Profile from "../pages/Profile";
import ProjectDetail from "../pages/ProjectDetail";
import Register from "../pages/Register";
import WorkspaceList from "../pages/WorkspaceList";

function WorkspaceLegacyRedirect() {
  const { workspaceId } = useParams();
  return <Navigate replace to={`/workspaces?workspace=${workspaceId}`} />;
}

function AppRouter() {
  const protectedPage = (page) => (
    <ProtectedRoute>
      <AppLayout>{page}</AppLayout>
    </ProtectedRoute>
  );

  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/health" element={<HealthCheckPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/dashboard" element={protectedPage(<Dashboard />)} />
            <Route path="/workspaces" element={protectedPage(<WorkspaceList />)} />
            <Route path="/workspaces/:workspaceId" element={protectedPage(<WorkspaceLegacyRedirect />)} />
            <Route path="/projects/:projectId" element={protectedPage(<ProjectDetail />)} />
            <Route path="/my-tasks" element={protectedPage(<MyTasks />)} />
            <Route path="/notifications" element={protectedPage(<Notifications />)} />
            <Route path="/profile" element={protectedPage(<Profile />)} />
            <Route path="*" element={protectedPage(<NotFound />)} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default AppRouter;
