import { useState } from "react";
import { getHealth } from "../api/healthApi";

function HealthCheckPage() {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleCheckHealth = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getHealth();
      setHealth(data);
    } catch (err) {
      setHealth(null);
      setError(err.response?.data?.message || "Không thể kết nối tới backend");
    } finally {
      setLoading(false);
    }
  };

  const isHealthy = health?.success;

  return (
    <main className="container py-5">
      <div className="row justify-content-center">
        <div className="col-lg-8">
          <div className="bg-white border rounded p-4 p-md-5 shadow-sm">
            <p className="text-uppercase text-primary fw-semibold mb-2">Giai đoạn 1</p>
            <h1 className="h3 fw-bold mb-3">Kiểm tra trạng thái backend</h1>
            <p className="text-secondary mb-4">
              Trang này gọi <code>GET /api/health</code> từ backend Spring Boot.
            </p>

            <button className="btn btn-primary" onClick={handleCheckHealth} disabled={loading}>
              {loading ? "Đang kiểm tra..." : "Kiểm tra backend"}
            </button>

            {error && (
              <div className="alert alert-danger mt-4 mb-0" role="alert">
                {error}
              </div>
            )}

            {health && (
              <div className="mt-4">
                <div
                  className={`status-pill ${isHealthy ? "bg-success-subtle text-success" : "bg-danger-subtle text-danger"}`}
                >
                  <span className={`status-dot ${isHealthy ? "bg-success" : "bg-danger"}`} />
                  {health.message}
                </div>
                <pre className="bg-light border rounded p-3 mt-3 mb-0">
                  {JSON.stringify(health, null, 2)}
                </pre>
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}

export default HealthCheckPage;
