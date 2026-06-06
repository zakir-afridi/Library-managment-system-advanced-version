# Project Brief — LibraCore Pro

## What It Is
A fully offline desktop Library Management System.
- **App**: LibraCore Pro v2.0.0
- **Entry point**: `com.library.LibraCoreApp` — the ONLY class with `main()`
- **Architecture**: Branch-Per-Module Tree (each feature = independent branch)
- **Stack**: Java 17 · JavaFX 21.0.1 · SQLite 3.46.1.3 · Maven 3.x

## Core Goals
- Manage books, members, students, employees, issue/return transactions
- Reservations, archiving, fine calculation (PKR 5/day, 2-day grace)
- Fully offline — embedded SQLite (`library.db`, WAL mode, pool size 5)
- Export reports to PDF (OpenPDF 1.3.43) and Excel (Apache POI 5.2.5)
- BCrypt passwords, SHA-256 recovery key, 30-min auto-logout

## Login
- Default: `admin` / `admin` (role: ADMIN)
- Recovery key: `03150315` (SHA-256 checked in `Constants.isValidRecoveryKey()`)
- Roles: `ADMIN` (full access), `LIBRARIAN` (read/write, no admin settings)

## 8 Feature Branches
| Branch | Responsibility |
|--------|---------------|
| `auth/` | Login, forgot password, reset password |
| `dashboard/` | KPI cards, PieChart, BarChart, LineChart, 30s auto-refresh |
| `books/` | CRUD, search by title/author/ISBN, archive/restore |
| `members/` | CRUD, membership types (Regular, Premium, Student, Faculty) |
| `students/` | CRUD, student ID, department, year |
| `employees/` | CRUD, employee code (EP prefix), salary |
| `issuing/` | Issue/return, fine calculation via `FineCalculator`, history |
| `reports/` | Borrowing, overdue, PDF/Excel export |

## Shared Branch
`shared/` — `SharedModule`, `DatabaseManager`, `ValidationUtil`, `AlertUtil`, `DateUtil`, `ChartUtil`

## Build & Run
- `pom.xml`: `Library-managment-system-advanced-version/Library/pom.xml`
- JAR: `library-management-system-2.0.0.jar`
- Run: `run.bat` (first run builds; subsequent runs skip build)
- Force rebuild: `run.bat --rebuild`

## Roadmap (Priority Order)
- P1: Barcode scanner, email overdue notifications (SMTP)
- P2: Multi-language (i18n), scheduled DB backups
- P3: Cloud sync, mobile app, AI book recommendations
