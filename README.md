# TeamSpace

TeamSpace la ung dung lam viec nhom cho do an sinh vien: quan ly workspace, project, task, file, chat realtime, notification, dashboard, calendar, meeting notes va hop online WebRTC 2 nguoi.

## Cong nghe

- Backend: Spring Boot 3, Spring Security, JWT, MySQL, JPA/Hibernate, WebSocket STOMP.
- Frontend: React, Vite, Bootstrap, STOMP client, WebRTC.
- Database: MySQL 8.
- Upload file: luu local trong thu muc `uploads`.

## Kien truc he thong

- React frontend goi REST API qua `VITE_API_BASE_URL`.
- Spring Boot backend xu ly auth, phan quyen, business logic va metadata.
- MySQL luu user, workspace, project, task, comment, file metadata, meeting, notification, activity log.
- WebSocket `/ws` dung cho chat, notification va WebRTC signaling.
- WebRTC ket noi video/audio truc tiep giua 2 trinh duyet; backend chi lam signaling server.

## Chay nhanh bang Docker Compose

Yeu cau: Docker Desktop.

```bash
docker compose up --build
```

Sau khi chay:

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- MySQL: localhost:3306, database `teamspace`, user `root`, password `123456`

Dung lenh sau de dung he thong:

```bash
docker compose down
```

Neu muon xoa database/upload:

```bash
docker compose down -v
```

## Chay local de phat trien

### 1. Chay MySQL bang Docker

```bash
docker compose up mysql
```

### 2. Chay backend

```bash
cd teamspace
./mvnw spring-boot:run
```

Tren Windows:

```powershell
cd teamspace
.\mvnw.cmd spring-boot:run
```

Backend mac dinh ket noi:

- URL: `jdbc:mysql://localhost:3306/teamspace`
- Username: `root`
- Password: `123456`

Co the override bang bien moi truong trong `teamspace/.env.example`.

### 3. Chay frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend local: http://localhost:5173

Neu can, tao `frontend/.env` theo mau:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

De bat dang nhap Google, tao OAuth 2.0 Client ID loai Web application trong Google Cloud,
them `http://localhost:5173` va `http://localhost:3000` vao Authorized JavaScript origins.
Tao file `.env` tai thu muc goc va dien Client ID mot lan cho ca frontend va backend:

```env
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
```

Client Secret chi duoc backend doc va khong duoc dua vao bundle frontend. File nay cung
duoc Docker Compose tu dong su dung khi build.

## Tai khoan demo goi y

He thong chua seed user mau. Khi demo, dang ky cac tai khoan sau tren giao dien:

| Vai tro demo | Email | Mat khau |
| --- | --- | --- |
| Owner | owner@teamspace.local | 123456 |
| Leader | leader@teamspace.local | 123456 |
| Member | member@teamspace.local | 123456 |

Sau khi Owner tao workspace, them Leader va Member vao workspace, doi role Leader trong man hinh workspace.

## Kich ban demo

1. Register/Login bang tai khoan Owner.
2. Tao workspace `Nhom Do An`.
3. Them `leader@teamspace.local` va `member@teamspace.local` vao workspace.
4. Doi role Leader cho tai khoan leader.
5. Tao project `TeamSpace MVP`.
6. Vao project, mo tab `Tasks`, tao task va giao cho Member.
7. Dang nhap Member o trinh duyet khac, vao `My Tasks`, doi trang thai task.
8. Upload file trong tab `Files`, download va xoa file.
9. Mo tab `Chat`, test chat realtime bang 2 trinh duyet.
10. Kiem tra `Notifications` khi giao task/comment/upload file/tao meeting.
11. Mo tab `Dashboard`, xem thong ke task va progress.
12. Mo tab `Calendar`, tao lich hop va xem deadline task/project.
13. Mo tab `Meetings`, tao meeting, ghi bien ban, them participant, tao task tu bien ban.
14. Tao `Phong hop online`, dung 2 trinh duyet bam `Join Meeting` de test WebRTC.
15. Mo tab `Activity` va `Contribution` de xem lich su va diem dong gop.

## Checklist testing

- Register/Login thanh cong, token duoc luu, `/api/auth/me` khong tra password.
- 401: truy cap API khi chua login bi chan.
- 403: Member khong duoc tao/sua/xoa nhung chuc nang chi OWNER/LEADER.
- 404: truy cap id khong ton tai tra message loi ro rang.
- OWNER: tao workspace/project/task, quan ly thanh vien, xoa project/file.
- LEADER: tao/sua project/task, tao meeting, ghi bien ban.
- MEMBER: xem workspace/project/task, cap nhat task duoc giao, comment, upload file, join meeting.
- Chat realtime hoat dong giua 2 trinh duyet.
- Notification realtime hoat dong giua 2 tai khoan.
- WebRTC 2 nguoi hien local video va remote video.
- Responsive: kiem tra laptop, tablet va mobile width tren DevTools.

## API chinh

Auth:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/google`
- `GET /api/auth/me`

Workspace:

- `POST /api/workspaces`
- `GET /api/workspaces`
- `GET /api/workspaces/{workspaceId}`
- `PUT /api/workspaces/{workspaceId}`
- `DELETE /api/workspaces/{workspaceId}`
- `POST /api/workspaces/{workspaceId}/members`
- `PUT /api/workspaces/{workspaceId}/members/{userId}/role`
- `DELETE /api/workspaces/{workspaceId}/members/{userId}`

Project:

- `POST /api/workspaces/{workspaceId}/projects`
- `GET /api/workspaces/{workspaceId}/projects`
- `GET /api/projects/{projectId}`
- `PUT /api/projects/{projectId}`
- `DELETE /api/projects/{projectId}`

Task:

- `POST /api/projects/{projectId}/tasks`
- `GET /api/projects/{projectId}/tasks`
- `GET /api/tasks/{taskId}`
- `PUT /api/tasks/{taskId}`
- `PATCH /api/tasks/{taskId}/status`
- `GET /api/tasks/my`
- `POST /api/tasks/{taskId}/comments`
- `GET /api/tasks/{taskId}/comments`

File:

- `POST /api/projects/{projectId}/files`
- `POST /api/tasks/{taskId}/files`
- `GET /api/projects/{projectId}/files`
- `GET /api/tasks/{taskId}/files`
- `GET /api/files/{fileId}/download`
- `DELETE /api/files/{fileId}`

Realtime va dashboard:

- `GET /api/projects/{projectId}/messages`
- WebSocket chat: `/app/projects/{projectId}/chat`, `/topic/projects/{projectId}/chat`
- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `PATCH /api/notifications/read-all`
- WebSocket notification: `/topic/users/{userId}/notifications`
- `GET /api/projects/{projectId}/dashboard`
- `GET /api/projects/{projectId}/activity-logs`
- `GET /api/projects/{projectId}/contributions`

Calendar, meeting va WebRTC:

- `GET /api/projects/{projectId}/calendar-events`
- `POST /api/projects/{projectId}/calendar-events`
- `PUT /api/calendar-events/{eventId}`
- `DELETE /api/calendar-events/{eventId}`
- `POST /api/projects/{projectId}/meetings`
- `GET /api/projects/{projectId}/meetings`
- `GET /api/meetings/{meetingId}`
- `PUT /api/meetings/{meetingId}/notes`
- `POST /api/meetings/{meetingId}/participants/{userId}`
- `POST /api/meetings/{meetingId}/tasks`
- `POST /api/projects/{projectId}/meeting-rooms`
- `GET /api/projects/{projectId}/meeting-rooms`
- `GET /api/meeting-rooms/{roomId}`
- WebRTC signaling: `/app/meeting-rooms/{roomId}/signal`, `/topic/meeting-rooms/{roomId}/signal`

## Deploy ghi chu

- Docker Compose trong repo phu hop demo local.
- Khi deploy that, nen doi `JWT_SECRET`, password MySQL va domain CORS.
- Upload file local chi phu hop demo, khong phu hop scale nhieu server.
- WebRTC qua internet co the can TURN server, ban hien tai chi dung STUN public va phu hop test local/LAN.

## Huong phat trien sau nay

- AWS S3: luu file upload thay cho local disk.
- AWS RDS MySQL: database production co backup va monitoring.
- AWS EC2/ECS: deploy backend/frontend bang Docker.
- AWS CloudWatch: log, metric, alert cho backend.
- AWS SES: gui email invite workspace, quen mat khau, thong bao.
- Redis/RabbitMQ: scale notification/chat va background job.
- TURN server: cai thien WebRTC khi 2 user khac mang/NAT kho.
