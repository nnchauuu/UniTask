import { useEffect, useRef, useState } from "react";
import * as meetingRoomApi from "../api/meetingRoomApi";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";

const rtcConfiguration = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
};

function createClientId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function MeetingRoom({ room, onClose }) {
  const { token } = useAuth();
  const { showToast } = useToast();
  const clientIdRef = useRef(createClientId());
  const stompClientRef = useRef(null);
  const peerConnectionRef = useRef(null);
  const localStreamRef = useRef(null);
  const remoteStreamRef = useRef(null);
  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);
  const joinedRef = useRef(false);
  const [joined, setJoined] = useState(false);
  const [micEnabled, setMicEnabled] = useState(true);
  const [cameraEnabled, setCameraEnabled] = useState(true);
  const [remoteName, setRemoteName] = useState("");
  const [status, setStatus] = useState("Chua vao phong");
  const [error, setError] = useState("");

  const sendSignal = (payload) => {
    const client = stompClientRef.current;
    if (!client?.connected) {
      return;
    }

    client.publish({
      destination: `/app/meeting-rooms/${room.id}/signal`,
      body: JSON.stringify({
        ...payload,
        clientId: clientIdRef.current
      })
    });
  };

  const ensurePeerConnection = () => {
    if (peerConnectionRef.current) {
      return peerConnectionRef.current;
    }

    const peerConnection = new RTCPeerConnection(rtcConfiguration);
    remoteStreamRef.current = new MediaStream();

    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = remoteStreamRef.current;
    }

    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        sendSignal({
          type: "ICE_CANDIDATE",
          candidate: event.candidate.toJSON()
        });
      }
    };

    peerConnection.ontrack = (event) => {
      event.streams[0].getTracks().forEach((track) => {
        remoteStreamRef.current.addTrack(track);
      });
      setStatus("Da ket noi remote video");
    };

    localStreamRef.current?.getTracks().forEach((track) => {
      peerConnection.addTrack(track, localStreamRef.current);
    });

    peerConnectionRef.current = peerConnection;
    return peerConnection;
  };

  const createAndSendOffer = async () => {
    const peerConnection = ensurePeerConnection();
    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);
    sendSignal({ type: "OFFER", sdp: offer.sdp });
    setStatus("Da gui offer");
  };

  const handleSignal = async (signal) => {
    if (signal.clientId === clientIdRef.current) {
      return;
    }

    try {
      if (signal.type === "JOIN") {
        setRemoteName(signal.senderName || "Nguoi tham gia");
        setStatus(`${signal.senderName || "Nguoi tham gia"} da vao phong`);
        if (joinedRef.current) {
          await createAndSendOffer();
        }
        return;
      }

      if (signal.type === "LEAVE") {
        setRemoteName("");
        setStatus(`${signal.senderName || "Nguoi tham gia"} da roi phong`);
        if (remoteVideoRef.current) {
          remoteVideoRef.current.srcObject = null;
        }
        remoteStreamRef.current = null;
        peerConnectionRef.current?.close();
        peerConnectionRef.current = null;
        return;
      }

      const peerConnection = ensurePeerConnection();

      if (signal.type === "OFFER") {
        setRemoteName(signal.senderName || "Nguoi tham gia");
        await peerConnection.setRemoteDescription({ type: "offer", sdp: signal.sdp });
        const answer = await peerConnection.createAnswer();
        await peerConnection.setLocalDescription(answer);
        sendSignal({ type: "ANSWER", sdp: answer.sdp });
        setStatus("Da nhan offer va gui answer");
      }

      if (signal.type === "ANSWER") {
        await peerConnection.setRemoteDescription({ type: "answer", sdp: signal.sdp });
        setStatus("Da nhan answer");
      }

      if (signal.type === "ICE_CANDIDATE" && signal.candidate) {
        await peerConnection.addIceCandidate(new RTCIceCandidate(signal.candidate));
      }
    } catch (err) {
      setError(err.message || "Loi xu ly signaling");
    }
  };

  const joinRoom = async () => {
    setError("");
    setStatus("Dang xin quyen camera/mic...");

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      localStreamRef.current = stream;

      if (localVideoRef.current) {
        localVideoRef.current.srcObject = stream;
      }

      ensurePeerConnection();

      const client = meetingRoomApi.createMeetingRoomClient({
        token,
        roomId: room.id,
        onSignal: handleSignal,
        onError: setError
      });

      client.onConnect = () => {
        client.subscribe(`/topic/meeting-rooms/${room.id}/signal`, (message) => {
          handleSignal(JSON.parse(message.body));
        });
        joinedRef.current = true;
        setJoined(true);
        setStatus("Da vao phong, dang cho nguoi thu hai...");
        sendSignal({ type: "JOIN" });
      };

      stompClientRef.current = client;
      client.activate();
      showToast("Da vao phong hop");
    } catch (err) {
      setError(err.message || "Khong the truy cap camera/mic");
      setStatus("Khong the vao phong");
    }
  };

  const leaveRoom = () => {
    sendSignal({ type: "LEAVE" });
    joinedRef.current = false;
    setJoined(false);
    setRemoteName("");
    setStatus("Da roi phong");

    peerConnectionRef.current?.close();
    peerConnectionRef.current = null;

    localStreamRef.current?.getTracks().forEach((track) => track.stop());
    localStreamRef.current = null;
    remoteStreamRef.current = null;

    if (localVideoRef.current) {
      localVideoRef.current.srcObject = null;
    }
    if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = null;
    }

    stompClientRef.current?.deactivate();
    stompClientRef.current = null;
  };

  const toggleMic = () => {
    const next = !micEnabled;
    localStreamRef.current?.getAudioTracks().forEach((track) => {
      track.enabled = next;
    });
    setMicEnabled(next);
  };

  const toggleCamera = () => {
    const next = !cameraEnabled;
    localStreamRef.current?.getVideoTracks().forEach((track) => {
      track.enabled = next;
    });
    setCameraEnabled(next);
  };

  useEffect(() => {
    return () => {
      leaveRoom();
    };
  }, []);

  return (
    <section className="bg-white border rounded p-4 shadow-sm mb-4">
      <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
        <div>
          <p className="text-uppercase text-primary fw-semibold small mb-1">Online meeting</p>
          <h2 className="h5 fw-bold mb-1">{room.name}</h2>
          <div className="small text-secondary">{status}</div>
          {remoteName && <div className="small text-secondary">Remote: {remoteName}</div>}
        </div>
        <button className="btn btn-outline-secondary btn-sm" type="button" onClick={onClose}>
          Dong
        </button>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="meeting-video-grid mb-3">
        <div className="meeting-video-panel">
          <video ref={localVideoRef} autoPlay playsInline muted />
          <span className="badge text-bg-primary">Local</span>
        </div>
        <div className="meeting-video-panel">
          <video ref={remoteVideoRef} autoPlay playsInline />
          <span className="badge text-bg-success">Remote</span>
        </div>
      </div>

      <div className="d-flex flex-wrap gap-2">
        {!joined ? (
          <button className="btn btn-primary" type="button" onClick={joinRoom}>
            Join Meeting
          </button>
        ) : (
          <>
            <button className="btn btn-outline-primary" type="button" onClick={toggleMic}>
              {micEnabled ? "Tat mic" : "Bat mic"}
            </button>
            <button className="btn btn-outline-primary" type="button" onClick={toggleCamera}>
              {cameraEnabled ? "Tat camera" : "Bat camera"}
            </button>
            <button className="btn btn-outline-danger" type="button" onClick={leaveRoom}>
              Roi phong
            </button>
          </>
        )}
      </div>
    </section>
  );
}

export default MeetingRoom;
