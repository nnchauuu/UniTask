import { useState } from "react";

function FileUpload({ onUpload }) {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!file) {
      setError("Vui lòng chọn tệp");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await onUpload(file);
      setFile(null);
      event.target.reset();
    } catch (err) {
      setError(err.response?.data?.message || "Không thể tải tệp lên");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="d-grid gap-2" onSubmit={handleSubmit}>
      {error && <div className="alert alert-danger">{error}</div>}
      <label className="form-label" htmlFor="fileUpload">
        Tải tệp lên
      </label>
      <input
        className="form-control"
        id="fileUpload"
        type="file"
        onChange={(event) => setFile(event.target.files?.[0] || null)}
      />
      <button className="btn btn-primary btn-sm" type="submit" disabled={loading}>
        {loading ? "Đang tải lên..." : "Tải lên"}
      </button>
    </form>
  );
}

export default FileUpload;
