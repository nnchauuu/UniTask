import { X } from "lucide-react";
import { useState } from "react";

function CreateWorkspaceForm({ onCreate, onClose }) {
  const [form, setForm] = useState({ name: "", description: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");

    try {
      await onCreate(form);
      setForm({ name: "", description: "" });
      onClose?.();
    } catch (err) {
      const validation = err.response?.data?.data;
      const firstValidationError = validation && Object.values(validation)[0];
      setError(firstValidationError || err.response?.data?.message || "Không thể tạo không gian làm việc");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="workspace-modal-backdrop" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose?.()}>
      <section className="workspace-create-modal" role="dialog" aria-modal="true" aria-labelledby="create-workspace-title">
        <header>
          <div>
            <h2 id="create-workspace-title">Tạo không gian làm việc</h2>
            <p>Thiết lập nơi cộng tác mới cho nhóm của bạn.</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng"><X size={20} /></button>
        </header>
        {error && <div className="alert alert-danger mx-4 mt-3 mb-0">{error}</div>}
        <form onSubmit={handleSubmit}>
          <label htmlFor="workspaceName">Tên không gian</label>
          <input
            id="workspaceName"
            name="name"
            maxLength={100}
            value={form.name}
            onChange={handleChange}
            placeholder="Ví dụ: Nhóm đồ án tốt nghiệp"
            autoFocus
            required
          />
          <label htmlFor="workspaceDescription">Mô tả</label>
          <textarea
            id="workspaceDescription"
            name="description"
            maxLength={500}
            rows={4}
            value={form.description}
            onChange={handleChange}
            placeholder="Mục tiêu và nội dung làm việc của nhóm"
          />
          <footer>
            <button className="workspace-cancel-button" type="button" onClick={onClose}>Hủy</button>
            <button className="workspace-submit-button" type="submit" disabled={loading}>
              {loading ? "Đang tạo..." : "Tạo không gian"}
            </button>
          </footer>
        </form>
      </section>
    </div>
  );
}

export default CreateWorkspaceForm;
