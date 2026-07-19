import { useEffect, useRef, useState } from "react";
import { ArrowRight, LockKeyhole, Mail } from "lucide-react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function Login() {
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [googleError, setGoogleError] = useState("");
  const [googleReady, setGoogleReady] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const googleCodeClientRef = useRef(null);
  const { login, googleLogin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  useEffect(() => {
    if (!googleClientId) {
      return undefined;
    }

    let attempts = 0;
    const initializeGoogle = () => {
      if (!window.google?.accounts?.oauth2) {
        attempts += 1;
        return attempts < 50;
      }

      googleCodeClientRef.current = window.google.accounts.oauth2.initCodeClient({
        client_id: googleClientId,
        scope: "openid email profile",
        ux_mode: "popup",
        callback: async ({ code, error: googleResponseError }) => {
          if (googleResponseError || !code) {
            setGoogleLoading(false);
            setGoogleError("Không thể nhận mã đăng nhập từ Google.");
            return;
          }

          setGoogleError("");
          try {
            await googleLogin({ code, redirectUri: window.location.origin });
            navigate("/dashboard");
          } catch (err) {
            setGoogleError(err.response?.data?.message || "Đăng nhập Google thất bại");
          } finally {
            setGoogleLoading(false);
          }
        },
        error_callback: () => {
          setGoogleLoading(false);
          setGoogleError("Cửa sổ đăng nhập Google đã đóng hoặc không thể mở.");
        }
      });
      setGoogleReady(true);
      return false;
    };

    if (!initializeGoogle()) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      if (!initializeGoogle()) {
        window.clearInterval(timer);
      }
    }, 100);
    return () => window.clearInterval(timer);
  }, [googleClientId, googleLogin, navigate]);

  const handleGoogleLogin = () => {
    if (!googleClientId) {
      setGoogleError("Đăng nhập Google chưa được cấu hình.");
      return;
    }
    if (!googleReady || !googleCodeClientRef.current) {
      setGoogleError("Google đang tải, vui lòng thử lại sau vài giây.");
      return;
    }

    setGoogleError("");
    setGoogleLoading(true);
    googleCodeClientRef.current.requestCode();
  };

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
      await login(form);
      navigate("/dashboard");
    } catch (err) {
      setError(err.response?.data?.message || "Đăng nhập thất bại");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-showcase" aria-labelledby="login-showcase-title">
        <Link className="login-brand" to="/" aria-label="Về trang chủ UniTask">
          <span className="login-brand-mark" aria-hidden="true">U</span>
          <span>UniTask</span>
        </Link>

        <div className="login-showcase-copy">
          <p className="login-showcase-label">Cùng nhau tiến xa hơn</p>
          <h1 id="login-showcase-title">
            Bắt đầu ngày mới với mọi công việc trong một nơi.
          </h1>
          <p>
            Dự án, nhiệm vụ, tài liệu và cuộc trò chuyện được tổ chức rõ ràng,
            giúp cả nhóm tập trung vào những mục tiêu quan trọng.
          </p>
        </div>

        <img
          className="login-showcase-art"
          src="/images/Anh2.png"
          alt=""
          aria-hidden="true"
        />

        <blockquote className="login-quote">
          “Khi mọi người cùng nhìn về một tiến độ, phối hợp trở nên đơn giản hơn.”
          <footer>UniTask - Đồng hành cùng đội nhóm</footer>
        </blockquote>
      </section>

      <section className="login-form-side" aria-labelledby="login-title">
        <div className="login-form-wrap">
          <Link className="login-mobile-brand" to="/" aria-label="Về trang chủ UniTask">
            <span className="login-brand-mark" aria-hidden="true">U</span>
            <span>UniTask</span>
          </Link>

          <h2 id="login-title">Chào mừng bạn trở lại</h2>
          <p className="login-subtitle">Đăng nhập để tiếp tục làm việc cùng nhóm của bạn.</p>

          {location.state?.message && (
            <div className="alert alert-success" role="status">{location.state.message}</div>
          )}
          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          {googleError && <div className="alert alert-danger" role="alert">{googleError}</div>}

          <form className="login-form" onSubmit={handleSubmit} aria-busy={loading}>
            <div className="login-field">
              <label htmlFor="email">Địa chỉ email</label>
              <div className="login-input-shell">
                <Mail size={19} strokeWidth={1.8} aria-hidden="true" />
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
            </div>

            <div className="login-field">
              <label htmlFor="password">Mật khẩu</label>
              <div className="login-input-shell">
                <LockKeyhole size={19} strokeWidth={1.8} aria-hidden="true" />
                <input
                  id="password"
                  name="password"
                  type="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Nhập mật khẩu"
                  autoComplete="current-password"
                  required
                />
              </div>
            </div>

            <button className="login-submit" type="submit" disabled={loading}>
              <span>{loading ? "Đang đăng nhập..." : "Đăng nhập"}</span>
              {!loading && <ArrowRight size={19} strokeWidth={2} aria-hidden="true" />}
            </button>
          </form>

          <div className="login-divider"><span>hoặc</span></div>
          <button
            className="login-google-unavailable"
            type="button"
            onClick={handleGoogleLogin}
            disabled={googleLoading}
          >
            <span className="login-google-mark" aria-hidden="true">G</span>
            {googleLoading ? "Đang kết nối Google..." : "Đăng nhập bằng Google"}
          </button>

          <p className="login-register-text">
            Chưa có tài khoản? <Link to="/register">Tạo tài khoản</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

export default Login;
