import { useEffect, useMemo, useState } from "react";
import * as fileApi from "../api/fileApi";
import * as meetingApi from "../api/meetingApi";
import { useToast } from "../context/ToastContext";
import TaskForm from "./TaskForm";

function MeetingDetail({
  meeting,
  members,
  canManageMeetings,
  onMeetingUpdated,
  onTaskCreated,
  onContributionChanged
}) {
  const [noteForm, setNoteForm] = useState({ content: "", decisions: "" });
  const [participantUserId, setParticipantUserId] = useState("");
  const [savingNote, setSavingNote] = useState(false);
  const [addingParticipant, setAddingParticipant] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const { showToast } = useToast();

  useEffect(() => {
    setNoteForm({
      content: meeting.note?.content || "",
      decisions: meeting.note?.decisions || ""
    });
    setParticipantUserId("");
    setError("");
    setSuccess("");
  }, [meeting.id, meeting.note]);

  const participantIds = useMemo(
    () => new Set((meeting.participants || []).map((participant) => participant.user.id)),
    [meeting.participants]
  );

  const availableMembers = members.filter((member) => !participantIds.has(member.userId));

  const handleNoteChange = (event) => {
    setNoteForm((current) => ({
      ...current,
      [event.target.name]: event.target.value
    }));
  };

  const handleSaveNote = async (event) => {
    event.preventDefault();
    setSavingNote(true);
    setError("");
    setSuccess("");

    try {
      const response = await meetingApi.updateMeetingNotes(meeting.id, noteForm);
      onMeetingUpdated(response.data);
      setSuccess("Da luu bien ban hop");
      showToast("Da luu bien ban hop");
    } catch (err) {
      setError(err.response?.data?.message || "Khong the luu bien ban hop");
    } finally {
      setSavingNote(false);
    }
  };

  const handleAddParticipant = async (event) => {
    event.preventDefault();
    if (!participantUserId) {
      return;
    }

    setAddingParticipant(true);
    setError("");
    setSuccess("");

    try {
      const response = await meetingApi.addMeetingParticipant(meeting.id, participantUserId);
      onMeetingUpdated(response.data);
      onContributionChanged?.();
      setParticipantUserId("");
      setSuccess("Da them nguoi tham gia");
      showToast("Da them nguoi tham gia");
    } catch (err) {
      setError(err.response?.data?.message || "Khong the them nguoi tham gia");
    } finally {
      setAddingParticipant(false);
    }
  };

  const handleCreateTask = async (payload, attachmentFile) => {
    setError("");
    const createPayload = Object.fromEntries(Object.entries(payload).filter(([key]) => key !== "version"));
    const response = await meetingApi.createTaskFromMeeting(meeting.id, createPayload);
    let attachmentError = "";
    if (attachmentFile) {
      try {
        await fileApi.uploadTaskFile(response.data.id, attachmentFile);
      } catch (err) {
        attachmentError = err.response?.data?.message || "Không thể tải tệp đính kèm";
      }
    }
    onTaskCreated(response.data);
    setSuccess(attachmentError ? "Đã tạo task; tệp đính kèm chưa được tải lên." : "Đã tạo task từ biên bản họp");
    if (attachmentError) {
      setError(`Task đã được tạo nhưng ${attachmentError.toLowerCase()}`);
      showToast("Đã tạo task nhưng không thể tải tệp đính kèm", "warning");
    } else {
      showToast("Đã tạo task từ biên bản họp");
    }
  };

  return (
    <div className="border rounded p-3 h-100">
      <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
        <div>
          <h3 className="h5 fw-bold mb-1">{meeting.title}</h3>
          <div className="small text-secondary">
            {new Date(meeting.startTime).toLocaleString()} - {new Date(meeting.endTime).toLocaleString()}
          </div>
          {meeting.description && <p className="text-secondary mt-2 mb-0">{meeting.description}</p>}
        </div>
        <span className="badge text-bg-light border">{meeting.myRole}</span>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="row g-3">
        <div className="col-lg-6">
          <div className="border rounded p-3 h-100">
            <h4 className="h6 fw-bold mb-3">Bien ban hop</h4>
            {canManageMeetings ? (
              <form className="d-grid gap-3" onSubmit={handleSaveNote}>
                <div>
                  <label className="form-label" htmlFor={`meetingContent-${meeting.id}`}>
                    Noi dung
                  </label>
                  <textarea
                    className="form-control"
                    id={`meetingContent-${meeting.id}`}
                    name="content"
                    rows={5}
                    value={noteForm.content}
                    onChange={handleNoteChange}
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor={`meetingDecisions-${meeting.id}`}>
                    Quyet dinh
                  </label>
                  <textarea
                    className="form-control"
                    id={`meetingDecisions-${meeting.id}`}
                    name="decisions"
                    rows={4}
                    value={noteForm.decisions}
                    onChange={handleNoteChange}
                  />
                </div>
                <button className="btn btn-primary btn-sm" type="submit" disabled={savingNote}>
                  {savingNote ? "Dang luu..." : "Luu bien ban"}
                </button>
              </form>
            ) : (
              <div className="text-secondary">
                <p className="mb-3">{meeting.note?.content || "Chua co noi dung bien ban."}</p>
                <div className="fw-semibold text-dark mb-1">Quyet dinh</div>
                <p className="mb-0">{meeting.note?.decisions || "Chua co quyet dinh."}</p>
              </div>
            )}
          </div>
        </div>

        <div className="col-lg-6">
          <div className="border rounded p-3 h-100">
            <h4 className="h6 fw-bold mb-3">Nguoi tham gia</h4>
            {(meeting.participants || []).length === 0 ? (
              <div className="text-secondary small mb-3">Chua co nguoi tham gia.</div>
            ) : (
              <div className="d-grid gap-2 mb-3">
                {meeting.participants.map((participant) => (
                  <div className="border rounded p-2" key={participant.id}>
                    <div className="fw-semibold">{participant.user.fullName}</div>
                    <div className="small text-secondary">
                      {participant.user.email} - {new Date(participant.joinedAt).toLocaleString()}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <form className="d-flex gap-2" onSubmit={handleAddParticipant}>
              <select
                className="form-select form-select-sm"
                value={participantUserId}
                onChange={(event) => setParticipantUserId(event.target.value)}
              >
                <option value="">Chon thanh vien</option>
                {availableMembers.map((member) => (
                  <option key={member.userId} value={member.userId}>
                    {member.fullName} ({member.role})
                  </option>
                ))}
              </select>
              <button className="btn btn-outline-primary btn-sm" type="submit" disabled={addingParticipant}>
                Them
              </button>
            </form>
          </div>
        </div>
      </div>

      {canManageMeetings && (
        <div className="border rounded p-3 mt-3">
          <h4 className="h6 fw-bold mb-3">Tao task tu bien ban</h4>
          <TaskForm
            members={members}
            canAssign={canManageMeetings}
            submitLabel="Tao task"
            loadingLabel="Dang tao..."
            onSubmit={handleCreateTask}
          />
        </div>
      )}
    </div>
  );
}

export default MeetingDetail;
