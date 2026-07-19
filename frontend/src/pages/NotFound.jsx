import { Link } from "react-router-dom";

function NotFound() {
  return (
    <main className="container-fluid">
      <section className="bg-white border rounded p-5 text-center shadow-sm">
        <p className="text-uppercase text-primary fw-semibold small mb-2">404</p>
        <h1 className="h3 fw-bold">Khong tim thay trang</h1>
        <p className="text-secondary mb-4">Duong dan nay khong ton tai hoac ban khong co quyen truy cap.</p>
        <Link className="btn btn-primary" to="/dashboard">
          Ve Dashboard
        </Link>
      </section>
    </main>
  );
}

export default NotFound;
