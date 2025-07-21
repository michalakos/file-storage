# File Storage Service

A minimal Spring Boot-based file storage application with user authentication and file management capabilities.

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
- Spring Data JPA
- H2 Database (embedded)
- Lombok

## API Endpoints

| Method | Endpoint                | Description                     |
|--------|-------------------------|---------------------------------|
| POST   | /api/auth/register      | Register new user               |
| POST   | /api/auth/login         | User login                      |
| POST   | /api/files              | Upload file                     |
| GET    | /api/files              | List all user's files           |
| GET    | /api/files/{id}         | Get file metadata               |
| GET    | /api/files/{id}/data    | Download file                   |
| DELETE | /api/files/{id}         | Delete file                     |

## Setup

1. Clone the repository
2. Configure application.properties:
   ```properties
   storage.location=./uploads
   spring.datasource.url=jdbc:h2:file:./data/filestore
   spring.h2.console.enabled=true
   ```

## TODOs:
* Secure files (only accept certain types of files / virus scan)
* Compress -> Encrypt files
* Custom exceptions
* Better authentication (jwt?)
* Connect with persistent database (dockerized?)
* Dockerize
* GUI
* Use streams for handling upload/download of large files
