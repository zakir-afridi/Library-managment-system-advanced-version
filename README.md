# LibraCore Pro v3.0.0

<div align="center">

![Java](https://img.shields.io/badge/Java-24-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-3.49.1-lightblue?style=for-the-badge&logo=sqlite)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=for-the-badge&logo=apachemaven)
![HikariCP](https://img.shields.io/badge/HikariCP-5.1.0-green?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**A fully offline, professional Desktop Library Management System**
Built with Java 24 · JavaFX 21.0.2 · SQLite · HikariCP · Branch-Per-Module Architecture

</div>

---

## What's New in v3.0.0

| Upgrade | Detail |
|---------|--------|
| **Java 24** | `--enable-preview` + virtual threads via `Executors.newVirtualThreadPerTaskExecutor()` |
| **HikariCP 5.1.0** | Replaces hand-rolled connection pool — pool size 10, WAL mode |
| **SQLite 3.49.1** | Upgraded from 3.46 |
| **Structured Logging** | SLF4J + Logback replaces all `System.out.println` |
| **GlobalExceptionHandler** | No silent crashes |
| **OpenLibraryClient** | ISBN auto-fill from Open Library API (30-day DB cache) |
| **WeatherClient** | Open-Meteo live weather, no API key needed (30-min cache) |
| **BarcodeScanner** | ZXing 3.5.3 — decode EAN/QR from image, generate QR codes |
| **EmailService** | Jakarta Mail 2.0.1 — Gmail SMTP, queue with 3 retries |
| **BackupScheduler** | Daily auto-backup to `~/.libracore/backups/`, keeps last 30 |
| **ExchangeRateClient** | Frankfurter free currency rates (24h cache) |

---

## Features

| Module | What It Does |
|--------|-------------|
| **Auth** | Login, forgot password (recovery key: `03150315`), BCrypt hashing, 30-min auto-logout |
| **Dashboard** | KPI cards, PieChart, BarChart, LineChart, 30s auto-refresh, weather widget, overdue alerts |
| **Books** | Add / Edit / Delete / Search by title, author, ISBN · ISBN auto-fill · Archive & Restore |
| **Members** | Full CRUD · Student ID generation (`LIB-YYYY-NNNN`) · Fine tracking |
| **Employees** | Staff CRUD · EP code generation · Profile PDF print |
| **Issue / Return** | Issue books · Auto fine calculation (PKR 5/day, 2-day grace) · Return with condition |
| **Reports** | Overdue · Circulation · Inventory · Fine Collection · Popular Books — PDF & Excel export |
| **Settings** | Loan days · Fine rate · Theme · Library name · Email · DB backup/restore |

### Additional Capabilities
- CSV import/export for books and members
- Light / Dark theme switching
- Toast notifications + activity audit log
- Global search with Trie (300ms debounce)
- Paginated tables (sortable, 20 rows/page)
- Inline form validation with field-level error highlighting
- Structured ID generation (`BK00000001`, `ST00000001`, `EP00000001`)
- Archive / Unarchive with reason tracking
- Reservation queue for unavailable books
- Overdue priority queue (max-heap by days overdue)

---

## Architecture

```
LibraCoreApp (Main Trunk)
|-- auth/        |-- dashboard/   |-- books/
|-- members/     |-- employees/   |-- issuing/
|-- reports/     |-- students/    `-- shared/
```

Each branch exposes only static methods — no direct controller coupling between branches.

---

## Quick Start

```bat
cd Library-managment-system-advanced-version\Library
run.bat
```

> First run builds the JAR. Subsequent runs skip build and launch instantly.
> Force rebuild: `run.bat --rebuild`

---

## Login

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin` |
| Recovery Key | `03150315` |

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 24 |
| UI Framework | JavaFX | 21.0.2 |
| Database | SQLite (sqlite-jdbc) | 3.49.1.0 |
| Connection Pool | HikariCP | 5.1.0 |
| Password Hashing | jBCrypt | 0.4 |
| PDF Export | OpenPDF (librepdf) | 1.3.43 |
| Excel/CSV Export | Apache POI | 5.2.5 |
| Logging | SLF4J + Logback | 2.x |
| JSON | Jackson | 2.18.2 |
| Barcode | ZXing | 3.5.3 |
| Email | Jakarta Mail | 2.0.1 |
| Build | Maven | 3.x |

---

## Project Structure

```
Library-managment-system-advanced-version/
|-- run.bat
`-- src/main/java/com/library/
    |-- LibraCoreApp.java       <- ONLY main()
    |-- auth/    books/    members/    employees/
    |-- issuing/ dashboard/ reports/  students/
    |-- shared/  controller/ service/
    |-- api/     cache/     config/
    `-- model/   database/  security/  util/
```

---

## Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `library.loanDays` | `14` | Default loan period |
| `library.fineRate` | `5.0` | Fine per day (PKR) |
| `library.gracePeriod` | `2` | Grace days before fine |
| `library.maxBooks` | `5` | Max books per member |
| `library.currency` | `PKR` | Display currency |
| `ui.theme` | `light` | `light` or `dark` |

---

## Roadmap

- [x] Core CRUD — Books, Members, Employees
- [x] Issue / Return with fine calculation
- [x] Dashboard with charts + weather
- [x] PDF & Excel reports
- [x] Archive / Unarchive
- [x] Light / Dark theme
- [x] Password recovery
- [x] HikariCP connection pool
- [x] Structured logging (SLF4J + Logback)
- [x] ISBN auto-fill (Open Library API)
- [x] Barcode scanner (ZXing)
- [x] Daily auto-backup
- [ ] Email overdue notifications via SMTP *(P1)*
- [ ] Student CRUD UI *(in progress)*
- [ ] Multi-language / i18n *(P2)*

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
Made with Java · LibraCore Pro v3.0.0
</div>
