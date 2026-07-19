import { useState } from "react";

function AddMemberForm({ myRole, onAdd }) {
  const [form, setForm] = useState({ email: "", role: "MEMBER" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const roleOptions = myRole === "OWNER" ? ["MEMBER", "LEADER", "OWNER"] : ["MEMBER", "LEADER"];

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
      await onAdd(form);
      setForm({ email: "", role: "MEMBER" });
    } catch (err) {
      const validation = err.response?.data?.data;
      const firstValidationError = validation && Object.values(validation)[0];
      setError(firstValidationError || err.response?.data?.message || "Không thể thêm thành viên");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="border rounded p-3">
      <h2 className="h5 fw-bold mb-3">Thêm thành viên</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      <form className="row g-3" onSubmit={handleSubmit}>
        <div className="col-md-7">
          <label className="form-label" htmlFor="memberEmail">
            Email
          </label>
          <input
            className="form-control"
            id="memberEmail"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            required
          />
        </div>
        <div className="col-md-3">
          <label className="form-label" htmlFor="memberRole">
            Vai trò
          </label>
          <select
            className="form-select"
            id="memberRole"
            name="role"
            value={form.role}
            onChange={handleChange}
          >
            {roleOptions.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-2 d-flex align-items-end">
          <button className="btn btn-primary w-100" type="submit" disabled={loading}>
            {loading ? "Đang thêm..." : "Thêm"}
          </button>
        </div>
      </form>
    </div>
  );
}

export default AddMemberForm;
