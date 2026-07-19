import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";

function Profile() {
  const { user, refreshUser } = useAuth();
  const [loading, setLoading] = useState(!user);
  const [error, setError] = useState("");

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true);
      setError("");

      try {
        await refreshUser();
      } catch (err) {
        setError(err.response?.data?.message || "Khong the tai thong tin tai khoan");
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  return (
    <main className="container-fluid">
      <div className="page-header mb-4">
        <div>
          <p className="text-uppercase text-primary fw-semibold small mb-1">Profile</p>
          <h1 className="h3 fw-bold mb-0">Thong tin tai khoan</h1>
        </div>
      </div>

      {loading && <div className="alert alert-info">Dang tai thong tin tai khoan...</div>}
      {error && <div className="alert alert-danger">{error}</div>}

      {user && (
        <section className="bg-white border rounded p-4 shadow-sm">
          <div className="row g-3">
            <div className="col-md-6">
              <div className="profile-tile">
                <div className="small text-secondary">Ho va ten</div>
                <div className="fw-semibold">{user.fullName}</div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="profile-tile">
                <div className="small text-secondary">Email</div>
                <div className="fw-semibold">{user.email}</div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="profile-tile">
                <div className="small text-secondary">Vai tro he thong</div>
                <div className="d-flex flex-wrap gap-2">
                  {user.roles?.map((role) => (
                    <span className="badge text-bg-primary" key={role}>
                      {role}
                    </span>
                  ))}
                </div>
              </div>
            </div>
            <div className="col-md-6">
              <div className="profile-tile">
                <div className="small text-secondary">Trang thai</div>
                <span className={`badge ${user.enabled ? "text-bg-success" : "text-bg-danger"}`}>
                  {user.enabled ? "Dang hoat dong" : "Da khoa"}
                </span>
              </div>
            </div>
            <div className="col-md-6">
              <div className="profile-tile">
                <div className="small text-secondary">Ngay tao</div>
                <div className="fw-semibold">
                  {user.createdAt ? new Date(user.createdAt).toLocaleString() : "Chua co du lieu"}
                </div>
              </div>
            </div>
          </div>
        </section>
      )}
    </main>
  );
}

export default Profile;
