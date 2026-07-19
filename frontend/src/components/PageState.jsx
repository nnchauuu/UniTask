export function LoadingState({ message = "Dang tai du lieu..." }) {
  return (
    <div className="page-state border rounded bg-white text-center p-4">
      <div className="spinner-border text-primary mb-3" role="status" aria-hidden="true" />
      <div className="fw-semibold">{message}</div>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="page-state border rounded bg-white p-4">
      <div className="alert alert-danger mb-3">{message || "Co loi xay ra. Vui long thu lai."}</div>
      {onRetry && (
        <button className="btn btn-outline-primary btn-sm" onClick={onRetry}>
          Thu lai
        </button>
      )}
    </div>
  );
}
