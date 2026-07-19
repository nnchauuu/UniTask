import { useEffect, useState } from "react";
import * as meetingApi from "../api/meetingApi";
import * as meetingRoomApi from "../api/meetingRoomApi";
import { useToast } from "../context/ToastContext";
import MeetingDetail from "./MeetingDetail";
import MeetingRoom from "./MeetingRoom";

const emptyForm = {
  title: "",
  description: "",
  startTime: "",
  endTime: ""
};

function MeetingList({
  projectId,
  members,
  canManageMeetings,
  refreshKey = 0,
  onMeetingCreated,
  onMeetingTaskCreated,
  onContributionChanged
}) {
  const [meetings, setMeetings] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [selectedMeetingId, setSelectedMeetingId] = useState(null);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [roomName, setRoomName] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const { showToast } = useToast();

  const loadMeetings = async () => {
    setLoading(true);
    setError("");

    try {
      const [meetingResponse, roomResponse] = await Promise.all([
        meetingApi.getProjectMeetings(projectId),
        meetingRoomApi.getProjectMeetingRooms(projectId)
      ]);
      setMeetings(meetingResponse.data);
      setRooms(roomResponse.data);
      setSelectedMeetingId((current) => current || meetingResponse.data[0]?.id || null);
    } catch (err) {
      setError(err.response?.data?.message || "Khong the tai danh sach meeting");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMeetings();
  }, [projectId, refreshKey]);

  const selectedMeeting = meetings.find((meeting) => meeting.id === selectedMeetingId) || meetings[0] || null;

  const handleChange = (event) => {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value
    }));
  };

  const handleCreateMeeting = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const response = await meetingApi.createMeeting(projectId, form);
      setMeetings((current) => [response.data, ...current]);
      setSelectedMeetingId(response.data.id);
      setForm(emptyForm);
      setSuccess("Da tao meeting");
      showToast("Tao meeting thanh cong");
      onMeetingCreated?.();
      onContributionChanged?.();
    } catch (err) {
      const validation = err.response?.data?.data;
      const firstValidationError = validation && Object.values(validation)[0];
      setError(firstValidationError || err.response?.data?.message || "Khong the tao meeting");
    } finally {
      setSaving(false);
    }
  };

  const handleMeetingUpdated = (updatedMeeting) => {
    setMeetings((current) => current.map((meeting) => (meeting.id === updatedMeeting.id ? updatedMeeting : meeting)));
    setSelectedMeetingId(updatedMeeting.id);
  };

  const handleCreateRoom = async (event) => {
    event.preventDefault();
    if (!roomName.trim()) {
      return;
    }

    setError("");
    setSuccess("");

    try {
      const response = await meetingRoomApi.createMeetingRoom(projectId, { name: roomName });
      setRooms((current) => [response.data, ...current]);
      setRoomName("");
      setSuccess("Da tao phong hop online");
      showToast("Tao phong hop online thanh cong");
    } catch (err) {
      const validation = err.response?.data?.data;
      const firstValidationError = validation && Object.values(validation)[0];
      setError(firstValidationError || err.response?.data?.message || "Khong the tao phong hop online");
    }
  };

  return (
    <section className="bg-white border rounded p-4 shadow-sm mb-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <p className="text-uppercase text-primary fw-semibold small mb-1">Meetings</p>
          <h2 className="h5 fw-bold mb-0">Bien ban hop</h2>
        </div>
        <button className="btn btn-outline-secondary btn-sm" onClick={loadMeetings}>
          Lam moi
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {selectedRoom && <MeetingRoom room={selectedRoom} onClose={() => setSelectedRoom(null)} />}

      <div className="row g-4">
        <div className="col-lg-4">
          <div className="border rounded p-3 mb-3">
            <h3 className="h6 fw-bold mb-3">Phong hop online</h3>
            <form className="d-flex gap-2 mb-3" onSubmit={handleCreateRoom}>
              <input
                className="form-control form-control-sm"
                maxLength={150}
                placeholder="Ten phong hop"
                value={roomName}
                onChange={(event) => setRoomName(event.target.value)}
              />
              <button className="btn btn-primary btn-sm" type="submit">
                Tao
              </button>
            </form>
            {rooms.length === 0 ? (
              <div className="text-secondary small">Chua co phong hop online.</div>
            ) : (
              <div className="d-grid gap-2">
                {rooms.map((room) => (
                  <div className="border rounded p-2" key={room.id}>
                    <div className="fw-semibold">{room.name}</div>
                    <div className="small text-secondary mb-2">
                      Tao boi {room.createdBy?.fullName || "TeamSpace"}
                    </div>
                    <button className="btn btn-outline-primary btn-sm" type="button" onClick={() => setSelectedRoom(room)}>
                      Join Meeting
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {canManageMeetings && (
            <div className="border rounded p-3 mb-3">
              <h3 className="h6 fw-bold mb-3">Tao meeting</h3>
              <form className="d-grid gap-3" onSubmit={handleCreateMeeting}>
                <div>
                  <label className="form-label" htmlFor="meetingTitle">
                    Tieu de
                  </label>
                  <input
                    className="form-control"
                    id="meetingTitle"
                    name="title"
                    maxLength={200}
                    value={form.title}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="meetingDescription">
                    Mo ta
                  </label>
                  <textarea
                    className="form-control"
                    id="meetingDescription"
                    name="description"
                    rows={3}
                    maxLength={1000}
                    value={form.description}
                    onChange={handleChange}
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="meetingStart">
                    Bat dau
                  </label>
                  <input
                    className="form-control"
                    id="meetingStart"
                    name="startTime"
                    type="datetime-local"
                    value={form.startTime}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="meetingEnd">
                    Ket thuc
                  </label>
                  <input
                    className="form-control"
                    id="meetingEnd"
                    name="endTime"
                    type="datetime-local"
                    value={form.endTime}
                    onChange={handleChange}
                    required
                  />
                </div>
                <button className="btn btn-primary btn-sm" type="submit" disabled={saving}>
                  {saving ? "Dang tao..." : "Tao meeting"}
                </button>
              </form>
            </div>
          )}

          <div className="border rounded p-3">
            <h3 className="h6 fw-bold mb-3">Danh sach meeting</h3>
            {loading && <div className="alert alert-info">Dang tai meeting...</div>}
            {!loading && meetings.length === 0 && (
              <div className="text-secondary small text-center py-3">Chua co meeting.</div>
            )}
            <div className="d-grid gap-2">
              {meetings.map((meeting) => (
                <button
                  className={`btn text-start ${selectedMeeting?.id === meeting.id ? "btn-primary" : "btn-outline-secondary"}`}
                  key={meeting.id}
                  type="button"
                  onClick={() => setSelectedMeetingId(meeting.id)}
                >
                  <div className="fw-semibold">{meeting.title}</div>
                  <div className="small">
                    {new Date(meeting.startTime).toLocaleDateString()} - {meeting.participants?.length || 0} tham gia
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="col-lg-8">
          {selectedMeeting ? (
            <MeetingDetail
              meeting={selectedMeeting}
              members={members}
              canManageMeetings={canManageMeetings}
              onMeetingUpdated={handleMeetingUpdated}
              onTaskCreated={onMeetingTaskCreated}
              onContributionChanged={onContributionChanged}
            />
          ) : (
            <div className="text-secondary small border rounded p-4 text-center">Chon meeting de xem bien ban.</div>
          )}
        </div>
      </div>
    </section>
  );
}

export default MeetingList;
