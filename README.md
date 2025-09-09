# File Storage Service

A minimal Spring Boot-based file storage application with user authentication and file management
capabilities.

## Features

- ✅ User registration and authentication
- ✅ File upload/download
- ✅ File metadata storage
- ✅ Basic access control
- ✅ Local filesystem storage

## Technologies

- Java 21
- Spring Boot 3.5.3
- Spring Security
- JWTs
- Spring Data JPA
- Postgres Database
- Lombok

## API Endpoints

### Authentication

| Method | Endpoint           | Description       |
|--------|--------------------|-------------------|
| POST   | /api/auth/register | Register new user |
| POST   | /api/auth/login    | User login        |

---

### Account Management

| Method | Endpoint                    | Description                        |
|--------|-----------------------------|------------------------------------|
| GET    | /api/users/account          | Get account details                |
| DELETE | /api/users/account          | Delete account                     |
| GET    | /api/users/storage/used     | Get user's used storage            |
| GET    | /api/users/storage/user-max | Get user's maximum allowed storage |
| GET    | /api/users/                 | Search user by username or email   |

---

### File Management

| Method | Endpoint                  | Description                                                    |
|--------|---------------------------|----------------------------------------------------------------|
| POST   | /api/files                | Upload file                                                    |
| GET    | /api/files                | List all user's files                                          |
| GET    | /api/files/{id}           | Get file metadata                                              |
| GET    | /api/files/{id}/download  | Download file                                                  |
| PATCH  | /api/files/{id}/rename    | Rename file                                                    |
| DELETE | /api/files/{id}           | Delete file                                                    |
| POST   | /api/files/{fileId}/share | Share file with another user                                   |
| GET    | /api/files/paginated      | List all files that the given user can access, with pagination |

---

### Admin Operations

| Method | Endpoint                      | Description                                 |
|--------|-------------------------------|---------------------------------------------|
| POST   | /api/admin/register           | Register new admin                          |
| POST   | /api/admin/ban/{id}           | Ban user by ID                              |
| POST   | /api/admin/unban/{id}         | Unban user by ID                            |
| POST   | /api/admin/role/{id}/{role}   | Change user's role                          |
| GET    | /api/admin/users              | Get all users                               |
| GET    | /api/admin/users/{keyword}    | Search users by username or email           |
| GET    | /api/admin/files              | Get metadata for all uploaded files         |
| GET    | /api/admin/large-files/{size} | Get files larger than given size (in bytes) |
| GET    | /api/admin/storage            | Get total storage used                      |
| GET    | /api/admin/logs/{lines}       | Get recent application logs (last N lines)  |
| DELETE | /api/admin/users/{username}   | Delete a user's account                     |

## Setup

1. Clone the repository
2. Configure application.yaml (Optional)
3. Add .env file based on .env.example (Optional)
4. Start the containers `docker compose up -d`

## TODOs:

* Add monitoring (elk?)
* GUI
