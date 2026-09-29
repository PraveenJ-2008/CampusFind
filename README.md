# CampusFind — Lost and Found Item Tracker for Campus

**CampusFind** is a complete, production-grade, college-level web application designed to track lost and found items across a university campus. Built with modern Java 21, Spring Boot 3, Spring Data JPA, Hibernate, MySQL, Spring Security with BCrypt & JWT, and a responsive vanilla HTML5/CSS3/JavaScript frontend served directly by Spring Boot.

---

## 🌟 Key Features

1. **Role-Based Authentication & User Management**
   - Secure registration for **Students** and **Staff**.
   - Strict server-side restriction: users **cannot** self-register as `ADMIN`.
   - Administrator provisioning with pre-seeded credentials.
   - Passwords securely hashed with **BCrypt**.
   - Stateless **JWT (JSON Web Token)** authentication stored in `localStorage` and passed in `Authorization: Bearer <token>` headers.

2. **Lost Item Reporting & Tracking**
   - Students can submit detailed lost item reports with title, category, description, last-seen location, and date.
   - Server-side validation rejecting missing fields or future dates.
   - Ownership guards: only the reporting student or an administrator can edit or close a report.

3. **Found Item Registration & State Machine**
   - Staff and security personnel can register found items with physical location and safeguard details.
   - Strict status transition engine in the service layer:
     - `AVAILABLE` ➔ `CLAIMED`
     - `CLAIMED` ➔ `RETURNED`
     - `CLAIMED` ➔ `AVAILABLE` (if claim cancelled/unverified)
     - **Direct transition from `AVAILABLE` to `RETURNED` is strictly forbidden** and rejected with HTTP 400.

4. **Intelligent Keyword & Category Matching Engine**
   - Backend matching algorithm between open `LostReport` records and available `FoundItem` records.
   - Category ID verification (awards +40 points).
   - Text normalization: lowercase conversion, non-alphanumeric punctuation stripping, tokenization.
   - Stop-words filtering (strips common English filler words like "the", "a", "lost", "found", "room", etc.).
   - Keyword overlap calculation (awards up to +45 points) and campus location token overlap (up to +15 points).
   - Transparent similarity scoring (0–100%).
   - One-click backend confirmation that simultaneously marks the lost report as `MATCHED` and the found item as `CLAIMED`.

5. **Live Admin Dashboard & Metrics**
   - Real-time calculated statistics: Total Lost, Total Found, Pending (Open), Matched, Available, Claimed, Returned, and Total Users.
   - Real-time recent reports and found items tables.
   - Category management: create, edit, activate/deactivate, and delete (with protection preventing deletion of categories referenced by existing items).

6. **Responsive Web Frontend**
   - Clean, modern campus portal design adhering to university design aesthetics.
   - Professional navy (`#0f2744`) and blue (`#1d4ed8`) palette with subtle shadows, rounded cards, and responsive tables.
   - 11 complete screens:
     1. Login (with one-click demo login buttons)
     2. Registration
     3. Student Dashboard
     4. Report Lost Item Form
     5. Browse Lost Items
     6. Report Found Item Form (Staff/Admin)
     7. Browse Found Items
     8. Possible Matches Engine
     9. Admin Dashboard
     10. Category Management
     11. User Profile

---

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.4
- **Security**: Spring Security 6, BCrypt Password Hashing, JJWT (io.jsonwebtoken 0.12.6)
- **Persistence**: Spring Data JPA, Hibernate ORM, MySQL Connector/J
- **Validation**: Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@PastOrPresent`, `@Size`, `@Email`)
- **Documentation**: Springdoc OpenAPI 3 / Swagger UI 2.6.0
- **Frontend**: HTML5, Vanilla CSS3 (Custom Design System), Vanilla JavaScript (ES6+ `fetch()` API)
- **Build & Test**: Apache Maven 3.9+, JUnit 5, MockMvc

---

## 🗄️ Database Architecture & Setup

### 1. Create MySQL Database
Run this command in MySQL Workbench, MySQL Command Line Client, or terminal:
```sql
CREATE DATABASE IF NOT EXISTS campusfind_db;
```

### 2. Configure Environment Variables (Optional)
The application has sensible defaults for localhost MySQL (`root` / `root`), but you can customize via environment variables or IntelliJ Run Configuration:

| Environment Variable | Default Value | Description |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL Host |
| `DB_PORT` | `3306` | MySQL Port |
| `DB_NAME` | `campusfind_db` | Database Name |
| `DB_USER` | `root` | MySQL Username |
| `DB_PASSWORD` | `root` | MySQL Password |
| `SERVER_PORT` | `8080` | Application Web Port |
| `JWT_SECRET` | `404E6352...` | 256-bit Secret Key |

> **Note on Schema Generation**: The application uses Hibernate `ddl-auto: update` for rapid college prototyping and automatic table/index generation upon startup.

---

## 🚀 How to Run in IntelliJ IDEA

1. **Open IntelliJ IDEA**:
   - Click **File ➔ Open...**
   - Navigate to and select `C:\Users\Praveen\Documents\campusFind` (or `C:\Users\Praveen\Documents\campusFind\CampusFind`).
2. **Import Maven Project**:
   - IntelliJ will automatically detect `pom.xml`.
   - Click the **Maven** tab on the right sidebar and click the **Reload All Maven Projects** icon (🔄).
3. **Set Project SDK**:
   - Go to **File ➔ Project Structure ➔ Project**.
   - Ensure the SDK is set to **Java 21**.
4. **Run Application**:
   - In the Project explorer, expand `CampusFind/src/main/java/com/campusfind/`.
   - Right-click `CampusFindApplication.java` and click **Run 'CampusFindApplication'**.
5. **Access Application**:
   - Web Portal: [http://localhost:8080](http://localhost:8080)
   - Swagger / OpenAPI Documentation: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - API Docs JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 💻 Run via Command Line

```bash
cd CampusFind
mvn spring-boot:run
```

To run all automated test suites:
```bash
mvn test
```

---

## 🔑 Pre-Seeded Demo Accounts

The application automatically seeds realistic demo data and accounts upon startup:

| Role | Email | Password | Allowed Operations |
|---|---|---|---|
| **ADMIN** | `admin@campusfind.edu` | `Admin@123` | View full dashboard metrics, manage categories, view all reports, re-open items |
| **STUDENT** | `student@campusfind.edu` | `Student@123` | Report lost items, view personal reports, search items, run matching engine |
| **STAFF** | `staff@campusfind.edu` | `Staff@123` | Register found items, update item state (`AVAILABLE` ➔ `CLAIMED` ➔ `RETURNED`) |

*(On the login screen, click any of the quick-login chips to automatically fill these credentials!)*

---

## 📡 REST API Reference

All endpoints return a uniform JSON structure:
- **Success**: `{ "success": true, "message": "...", "data": ... }`
- **Error**: `{ "success": false, "message": "...", "errors": [...] }`

### Authentication (`/api/auth`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register Student or Staff account (Self-admin forbidden) |
| `POST` | `/api/auth/login` | Public | Authenticate user and receive JWT token |
| `GET` | `/api/auth/me` | Authenticated | Retrieve current user profile |

### Categories (`/api/categories`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/categories` | Public | List active categories (or `?all=true` for admin) |
| `GET` | `/api/categories/{id}` | Public | Get single category |
| `POST` | `/api/categories` | ADMIN | Create new category (prevents duplicates) |
| `PUT` | `/api/categories/{id}` | ADMIN | Edit category name/description |
| `PATCH` | `/api/categories/{id}/active` | ADMIN | Activate / Deactivate category |
| `DELETE` | `/api/categories/{id}` | ADMIN | Delete category (blocked if items reference it) |

### Lost Reports (`/api/lost`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/lost` | Authenticated | Submit lost item report |
| `GET` | `/api/lost` | Public | Search lost items (`?status=OPEN&categoryId=1&query=...`) |
| `GET` | `/api/lost/{id}` | Public | Get lost report details |
| `GET` | `/api/lost/mine` | Authenticated | View current user's submitted lost reports |
| `PUT` | `/api/lost/{id}` | Owner / ADMIN | Update lost report fields |
| `PATCH` | `/api/lost/{id}/status` | Owner / ADMIN | Update report status (`OPEN`, `MATCHED`, `CLOSED`) |

### Found Items (`/api/found`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/found` | STAFF / ADMIN | Register discovered found item |
| `GET` | `/api/found` | Public | Browse/filter found items (`?status=AVAILABLE...`) |
| `GET` | `/api/found/{id}` | Public | Get found item details |
| `GET` | `/api/found/mine` | STAFF / ADMIN | Get items registered by current staff member |
| `PATCH` | `/api/found/{id}/status` | Reporter / ADMIN | Update found item status (`AVAILABLE` ➔ `CLAIMED` ➔ `RETURNED`) |

### Matching Engine (`/api/matches`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/matches/{lostId}` | Public | Generate candidate matches with similarity scores |
| `POST` | `/api/matches/{lostId}/confirm?foundId={id}` | Owner / ADMIN | Confirm match (marks Lost=MATCHED, Found=CLAIMED) |

### Administration (`/api/admin`)
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/admin/dashboard` | ADMIN | Aggregated system metrics and recent logs |
| `GET` | `/api/admin/users` | ADMIN | List all registered users |
| `GET` | `/api/admin/reports` | ADMIN | Master reports overview with filter parameters |

---

## 🧪 Postman Collection

A complete Postman collection is included in the project:
`CampusFind/CampusFind_Postman_Collection.json`

Import it into Postman to test all positive and negative scenarios, including duplicate emails, unauthorized role escalation, invalid date validations, and invalid state transitions.

---

## 🛡️ Automated Test Results

The project includes 22 comprehensive automated tests:
```
[INFO] Running com.campusfind.CampusFindApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.campusfind.controller.CategoryControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.campusfind.controller.LostReportControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.campusfind.service.AuthServiceTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.campusfind.service.ItemLifecycleAndMatchingTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Results: Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
