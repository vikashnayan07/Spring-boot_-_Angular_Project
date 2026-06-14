# MachCare

MachCare is an industrial maintenance intelligence platform for machine fault logging, fault analysis, alert generation, maintenance scheduling, inventory visibility, employee management, notifications, and realtime operational updates.

The application is built as a Spring Boot backend with an Angular frontend. In production, the Angular build is packaged into the Spring Boot WAR and served under the `/machcare` context on Tomcat.

## Table Of Contents

- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)
- [Core Modules](#core-modules)
- [User Roles](#user-roles)
- [Local Development Setup](#local-development-setup)
- [Environment Configuration](#environment-configuration)
- [Run Commands](#run-commands)
- [Build Commands](#build-commands)
- [Production Deployment](#production-deployment)
- [Realtime Updates](#realtime-updates)
- [Demo Data](#demo-data)
- [Troubleshooting](#troubleshooting)
- [Git Workflow](#git-workflow)

## Project Structure

```text
machcare_app/
+-- backend/                 Spring Boot backend application
|   +-- src/main/java/       Controllers, services, entities, repositories
|   +-- src/main/resources/  Properties, static frontend assets, templates
|   +-- pom.xml              Maven dependencies and WAR build config
|   +-- mvnw.cmd             Maven wrapper for Windows
+-- frontend/                Angular frontend application
|   +-- src/app/             Angular features, shared components, services
|   +-- src/environments/    Local and production API URLs
|   +-- public/              Public frontend assets
|   +-- package.json         Node scripts and dependencies
+-- scripts/                 Deployment/helper scripts
+-- Jenkinsfile              CI/CD pipeline for build, deploy, verify, rollback
+-- README.md                Project onboarding guide
```

## Technology Stack

Backend:

- Java 17
- Spring Boot 3.5.x
- Spring Security with JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- WAR packaging for Tomcat
- Server-Sent Events and WebSocket-based realtime updates

Frontend:

- Angular 18
- TypeScript
- RxJS
- Tailwind CSS / custom component CSS
- Native browser WebSocket with SSE fallback

Infrastructure:

- DigitalOcean droplet
- Tomcat
- Nginx reverse proxy
- Jenkins CI/CD
- PostgreSQL
- Namecheap DNS
- SSL/HTTPS for `machcare.me`

## Core Modules

- Authentication and role-based access
- Admin dashboard
- Engineer dashboard
- Operator dashboard
- Machine management
- Fault logging
- Fault analysis
- Risk-based alert generation
- Maintenance alert auto-assignment
- Engineer task management
- Maintenance history
- Inventory and batch/expiry tracking
- Employee management
- Notifications
- Realtime updates
- Audit/activity logging

## User Roles

### Admin

Admins manage employees, machines, maintenance alerts, work orders, schedules, inventory, and system-level dashboards.

Important security rules:

- Admin accounts cannot be deleted.
- Admin accounts cannot be suspended.
- A logged-in user cannot delete or suspend their own active account.
- Suspended or inactive engineers must not receive new assigned tasks.

### Maintenance Engineer

Engineers analyze faults, generate machine alerts, view assigned tasks, update work status, request spare parts, and escalate support when required.

### Operator

Operators log machine faults and view operator-focused dashboards.

## Local Development Setup

### Required Software

Install these before running the project locally:

- Java 17
- Node.js 20.x
- npm
- PostgreSQL 14+
- Git
- VS Code, IntelliJ IDEA, or another IDE

### Database

Create a local PostgreSQL database:

```sql
CREATE DATABASE machcare;
```

The default local profile expects:

```text
Database: machcare
Schema: dev
Username: postgres
Password: set through LOCAL_DB_PASSWORD
Port: 5432
```

Hibernate is configured with `ddl-auto=update`, so application tables are created/updated automatically when the backend starts.

## Environment Configuration

### Backend

Default shared configuration:

```text
backend/src/main/resources/application.properties
```

Local configuration:

```text
backend/src/main/resources/application-local.properties
```

Production configuration:

```text
backend/src/main/resources/application-prod.properties
```

Recommended production values should be provided through environment variables:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/machcare?currentSchema=dev
DB_USERNAME=<production-db-user>
DB_PASSWORD=<production-db-password>
JWT_SECRET=<at-least-32-byte-secret>
JWT_EXPIRATION_MS=86400000
DB_SCHEMA=dev
SERVER_PORT=8080
JPA_DDL_AUTO=update
```

### Frontend

Local API URL:

```text
frontend/src/environments/environment.ts
```

Current local setting:

```ts
apiUrl: 'http://localhost:8082/api'
```

Production API URL:

```text
frontend/src/environments/environment.prod.ts
```

Current production setting:

```ts
apiUrl: '/machcare/api'
```

## Run Commands

Open two terminals.

### Terminal 1: Backend

```powershell
cd E:\machcare_app\backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Backend local URL:

```text
http://localhost:8082/api
```

### Terminal 2: Frontend

```powershell
cd E:\machcare_app\frontend
npm install
npm start
```

Frontend local URL:

```text
http://localhost:4200
```

## Build Commands

### Frontend Production Build

```powershell
cd E:\machcare_app\frontend
npm run build
```

Output:

```text
frontend/dist/angular
```

### Backend WAR Build

```powershell
cd E:\machcare_app\backend
.\mvnw.cmd clean package -DskipTests
```

Output:

```text
backend/target/ROOT.war
```

## Production Deployment

Production is deployed through Jenkins.

The pipeline:

1. Installs frontend dependencies with `npm ci`.
2. Builds Angular in production mode.
3. Copies Angular build output into `backend/src/main/resources/static`.
4. Runs backend tests.
5. Builds `ROOT.war`.
6. Deploys the WAR to Tomcat using the server deployment script.
7. Verifies production routes and static assets.
8. Rolls back automatically if deployment verification fails.

Production URL:

```text
https://machcare.me/machcare/
```

API base path:

```text
https://machcare.me/machcare/api
```

## Realtime Updates

MachCare supports realtime updates for:

- New fault logs
- Fault analysis alert generation
- Admin maintenance alert list updates
- Auto-assigned engineer tasks
- Task status changes
- Employee suspension/deactivation/revocation
- Notifications

The frontend connects using:

1. Native WebSocket first
2. Server-Sent Events fallback

Realtime endpoints:

```text
WebSocket: /machcare/api/realtime/ws?token=<jwt>
SSE:       /machcare/api/realtime/stream?token=<jwt>
Status:    /machcare/api/realtime/status
```

### Nginx WebSocket Configuration

If realtime does not work in production, confirm that Nginx forwards WebSocket upgrade headers:

```nginx
location /machcare/ {
    proxy_pass http://localhost:8080/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 3600;
    proxy_send_timeout 3600;
}
```

For SSE fallback, buffering should be disabled:

```nginx
proxy_buffering off;
proxy_cache off;
```

## Demo Data

The production profile includes demo seeding logic for demo-ready users and data.

Seeded demo users:

```text
Admin:     admin.demo@machcare.me
Engineer:  engineer.demo@machcare.me
Operator:  operator.demo@machcare.me
Password:  configured through DEMO_PASSWORD
```

Additional demo engineers/operators may also be created by the seeder.

Do not commit real demo or production credentials to Git.

## Troubleshooting

### Maven says: Unknown lifecycle phase `.run.profiles=local`

Use quotes around the Spring profile argument in PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Frontend calls localhost in production

Check:

```text
frontend/src/environments/environment.prod.ts
```

It should use:

```ts
apiUrl: '/machcare/api'
```

Then rebuild and redeploy.

### Angular build passes with budget warnings

Budget warnings are not deployment failures. They indicate that bundle or component CSS sizes exceed configured Angular budget thresholds.

### UI loads as dark background only

Usually this means JS/CSS assets are not being served from the correct `/machcare/` path.

Check production HTML:

```text
https://machcare.me/machcare/
```

Asset URLs should look like:

```text
/machcare/main-*.js
/machcare/styles-*.css
```

### Realtime updates do not appear

Check these in order:

1. Browser DevTools Network tab should show a connected WebSocket request to:

   ```text
   wss://machcare.me/machcare/api/realtime/ws?token=...
   ```

2. If WebSocket fails, SSE should connect to:

   ```text
   https://machcare.me/machcare/api/realtime/stream?token=...
   ```

3. As an admin, call:

   ```text
   GET /machcare/api/realtime/status
   ```

   The response should show active `ws-role:1` or `role:1` connections.

4. Check backend logs for:

   ```text
   WebSocket connected
   WebSocket emitting
   SSE connected
   SSE emitting
   ```

5. Confirm Nginx has WebSocket upgrade headers configured.

### Login works but notifications are empty

Confirm the `notification` table exists and the logged-in employee is active. Older employee rows with `is_active = null` are treated as active by the code.

### Suspended engineer receives task

Auto-assignment should only consider active, non-suspended engineers. Check employee state:

```text
is_active = true
suspension_end_date = null
role_id = 2
```

## Git Workflow

Recommended workflow:

```powershell
cd E:\machcare_app
git status
git pull --ff-only
git add .
git commit -m "describe the change clearly"
git push origin main
```

Jenkins polls the repository and automatically builds/deploys after changes are pushed to `main`.

## Important Notes

- Work on `E:\machcare_app` for the live `machcare.me` project.
- Keep production secrets out of Git.
- Validate backend and frontend builds before pushing.
- Avoid committing generated folders such as `node_modules`, `dist`, or `target`.
- Any change to realtime behavior should be tested with two logged-in browser sessions: one admin and one engineer/operator.
