import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function Register() {
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (event) => {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      await register(form);
      navigate("/dashboard");
    } catch (err) {
      const data = err.response?.data?.data;
      const firstValidationError = data && Object.values(data)[0];
      setError(firstValidationError || err.response?.data?.message || "Đăng ký thất bại");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page register-page">
      <section className="login-showcase" aria-labelledby="register-showcase-title">
        <Link className="login-brand" to="/" aria-label="Về trang chủ UniTask">
          <span className="login-brand-mark" aria-hidden="true">U</span>
          <span>UniTask</span>
        </Link>

        <div className="login-showcase-copy">
          <p className="login-showcase-label">Bắt đầu làm việc cùng nhau</p>
          <h1 id="register-showcase-title">
            Biến ý tưởng của nhóm thành kết quả thực tế.
          </h1>
          <p>
            Tạo tài khoản UniTask để quản lý dự án, phân công nhiệm vụ và kết
            nối mọi thành viên trong một không gian làm việc chung.
          </p>
        </div>

        <blockquote className="login-quote">
          “Một nhóm hiệu quả bắt đầu từ cách tổ chức công việc rõ ràng.”
          <footer>UniTask - Cùng nhau hoàn thành mục tiêu</footer>
        </blockquote>
      </section>

      <section className="login-form-side" aria-labelledby="register-title">
        <div className="login-form-wrap register-form-wrap">
          <Link className="login-mobile-brand" to="/" aria-label="Về trang chủ UniTask">
            <span className="login-brand-mark" aria-hidden="true">U</span>
            <span>UniTask</span>
          </Link>

          <h2 id="register-title">Tạo tài khoản UniTask</h2>
          <p className="login-subtitle">
            Bắt đầu tổ chức công việc và cộng tác cùng nhóm của bạn.
          </p>

          {error && <div className="alert alert-danger" role="alert">{error}</div>}

          <form className="login-form" onSubmit={handleSubmit}>
            <div className="login-field">
              <label htmlFor="fullName">Họ và tên</label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                value={form.fullName}
                onChange={handleChange}
                placeholder="Nguyễn Văn An"
                autoComplete="name"
                required
              />
            </div>

            <div className="login-field">
              <label htmlFor="email">Địa chỉ email</label>
              <input
                id="email"
                name="email"
                type="email"
                value={form.email}
                onChange={handleChange}
                placeholder="ban@unitask.vn"
                autoComplete="email"
                required
              />
            </div>

            <div className="login-field">
              <label htmlFor="password">Mật khẩu</label>
              <input
                id="password"
                name="password"
                type="password"
                minLength={6}
                value={form.password}
                onChange={handleChange}
                placeholder="Tối thiểu 6 ký tự"
                autoComplete="new-password"
                aria-describedby="password-hint"
                required
              />
              <small id="password-hint" className="register-password-hint">
                Sử dụng ít nhất 6 ký tự để bảo vệ tài khoản.
              </small>
            </div>

            <button className="login-submit" type="submit" disabled={loading}>
              {loading ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
            </button>
          </form>

          <p className="login-register-text">
            Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

export default Register;
