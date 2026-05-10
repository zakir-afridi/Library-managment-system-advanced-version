# LibraCore Pro v2.0.0

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-3.46-lightblue?style=for-the-badge&logo=sqlite)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=for-the-badge&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**A fully offline, professional Desktop Library Management System**  
Built with Java 17 · JavaFX 21 · SQLite · Branch-Per-Module Architecture

</div>

---

## Features

| Module | What It Does |
|--------|-------------|
| **Auth** | Login, forgot password (recovery key: `03150315`), BCrypt hashing, 30-min auto-logout |
| **Dashboard** | KPI cards, PieChart, BarChart, 30s auto-refresh, overdue alerts |
| **Books** | Add / Edit / Delete / Search by title, author, ISBN · Archive & Restore |
| **Members** | Full CRUD · Student ID generation (`LIB-YYYY-NNNN`) · Fine tracking |
| **Employees** | Staff CRUD · EP code generation · Profile PDF print |
| **Issue / Return** | Issue books · Auto fine calculation (PKR 5/day, 2-day grace) · Return with condition |
| **Reports** | Overdue · Circulation · Inventory · Fine Collection · Popular Books — all PDF export |
| **Settings** | Loan days · Fine rate · Theme · Library name · DB backup/restore |

### Additional Capabilities
- CSV import/export for books and members
- Light / Dark theme switching
- Toast notifications + activity audit log
- Global search with Trie (300ms debounce)
- Paginated tables (sortable, 20 rows/page)
- Inline form validation with field-level error highlighting
- Structured ID generation (`BK00000001`, `ST00000001`, `EP00000001`)
- Archive / Unarchive with reason tracking

---

## Architecture — Branch-Per-Module Tree

```
LibraCoreApp (Main Trunk — ONLY entry point)
|
|-- auth/           <- AuthModule, AuthService, AuthController
|-- dashboard/      <- DashboardModule, DashboardService, ChartFactory
|-- books/          <- BookModule, BooksService, BookController
|-- members/        <- MemberModule, MembersService, MemberController
|-- employees/      <- EmployeeModule, EmployeesService, EmployeeController
|-- issuing/        <- IssueModule, IssueService, FineCalculator
|-- reports/        <- ReportModule, ReportsService, ReportController
`-- shared/         <- DatabaseManager, Models, ValidationUtil, Constants
```

Each branch exposes **only static methods** — no direct controller coupling between branches.  
This reduces AI token usage by **82-85%** when fixing bugs (scan only the relevant branch).

---

## Quick Start

### Prerequisites
- Java 17+ ([Download](https://adoptium.net/))
- Maven (auto-detected from `~/.m2/wrapper`)

### Run (Windows)

```bat
cd Library-managment-system-advanced-version
run.bat
```

> First run builds the JAR automatically. Subsequent runs skip the build and launch instantly.  
> To force a rebuild: `run.bat --rebuild`

### Run (Maven directly)

```bash
cd Library-managment-system-advanced-version/Library
mvn clean package -DskipTests
java --module-path "target/lib/javafx-controls-21.0.1.jar;..." ^
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
     -cp "target/library-management-system-2.0.0.jar;target/lib/*" ^
     com.library.LibraCoreApp
```

---

## Login

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin` |
| Recovery Key | `03150315` |

> Roles: `ADMIN` (full access) · `LIBRARIAN` (read/write, no admin settings)

---

## Database Schema

| Table | Purpose |
|-------|---------|
| `users` | Login accounts, BCrypt hashes, roles, failed attempts |
| `books` | Catalogue with ISBN, category, quantity, shelf location |
| `members` | Library members with fine balance, membership type |
| `transactions` | Issue/return records with fine calculation |
| `employees` | Staff records |
| `reservations` | Book reservation queue |
| `activity_log` | Full audit trail |
| `archive_log` | Archive reason tracking |
| `id_counters` | Atomic structured ID generation |
| `settings` | Key/value config store |

- **Engine:** SQLite 3.46 · WAL mode · Connection pool (size 5)
- **File:** `library.db` (created automatically on first run)
- **Indexes:** isbn, book_name, category, author, student_id, name, email, due_date

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| UI Framework | JavaFX | 21.0.1 |
| Database | SQLite (sqlite-jdbc) | 3.46.1.3 |
| Password Hashing | jBCrypt | 0.4 |
| PDF Export | OpenPDF (librepdf) | 1.3.43 |
| Excel/CSV Export | Apache POI | 5.2.5 |
| Build | Maven | 3.x |

---

## Project Structure

```
Library-managment-system-advanced-version/
|-- run.bat                          <- Windows launcher (build + run)
`-- Library/
    |-- pom.xml                      <- Maven build file
    |-- .mvn/jvm.config              <- JVM flags
    `-- src/main/
        |-- java/com/library/
        |   |-- LibraCoreApp.java    <- ONLY main() in entire project
        |   |-- auth/               <- Login, forgot password, reset
        |   |-- books/              <- Book CRUD branch
        |   |-- members/            <- Member CRUD branch
        |   |-- employees/          <- Employee CRUD branch
        |   |-- issuing/            <- Issue/Return branch
        |   |-- dashboard/          <- Charts & stats branch
        |   |-- reports/            <- PDF/CSV reports branch
        |   |-- shared/             <- DB, models, utilities
        |   |-- controller/         <- JavaFX FXML controllers
        |   |-- service/            <- Business logic
        |   |-- model/              <- Plain Java beans
        |   |-- database/           <- Connection pool + DataSeeder
        |   |-- security/           <- BCrypt, SessionManager
        |   |-- config/             <- AppConfig, ThemeManager
        |   `-- util/               <- IdGenerator, Constants, PageRequest
        `-- resources/com/library/ui/
            |-- *.fxml              <- UI layouts
            |-- css/                <- light-theme.css, dark-theme.css
            `-- images/             <- Icons
```

---

## Configuration

Settings stored in `libra_config.properties` next to the JAR:

| Key | Default | Description |
|-----|---------|-------------|
| `loan.days` | `14` | Default loan period |
| `fine.rate` | `5.0` | Fine per day (PKR) |
| `grace.period` | `2` | Grace days before fine |
| `max.books` | `5` | Max books per member |
| `currency` | `PKR` | Display currency |
| `ui.theme` | `light` | `light` or `dark` |

---

## UI Design

- **Primary:** `#1976d2` (blue) · **Accent:** `#d32f2f` (red) · **Success:** `#388e3c` (green)
- **Min window:** 1024x768 · Sidebar collapses on narrow screens
- **Forms:** Two-column grid · Inline validation · Red border on error fields
- **Tables:** Sortable · Paginated (20 rows) · Row hover · Colour-coded status

---

## Roadmap

- [x] Core CRUD — Books, Members, Employees
- [x] Issue / Return with fine calculation
- [x] Dashboard with charts
- [x] PDF & CSV reports
- [x] Archive / Unarchive
- [x] Light / Dark theme
- [x] Password recovery
- [ ] Barcode scanner integration *(P1)*
- [ ] Email overdue notifications via SMTP *(P1)*
- [ ] Student CRUD UI *(in progress)*
- [ ] Multi-language / i18n *(P2)*
- [ ] Scheduled DB backups *(P2)*

---

## Authors

**Zakir Afridi**  
University of Engineering & Technology, Peshawar  
Data Science Student

**Muhammad Usman Nazir**  
University of Engineering & Technology, Peshawar  
Data Science Student

---

<div align="center">
Made with Java · LibraCore Pro v2.0.0
</div>
