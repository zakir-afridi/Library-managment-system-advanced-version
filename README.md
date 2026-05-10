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

## Architecture

```
LibraCoreApp (Main Trunk)
|-- auth/        |-- dashboard/   |-- books/
|-- members/     |-- employees/   |-- issuing/
|-- reports/     `-- shared/
```

Each branch exposes only static methods — no direct controller coupling between branches.

---

## Quick Start

```bat
cd Library-managment-system-advanced-version
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
|-- run.bat
`-- Library/
    |-- pom.xml
    `-- src/main/java/com/library/
        |-- LibraCoreApp.java    <- ONLY main()
        |-- auth/  books/  members/  employees/
        |-- issuing/  dashboard/  reports/
        |-- shared/  controller/  service/
        `-- model/  database/  security/  config/  util/
```

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

---

## Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `loan.days` | `14` | Default loan period |
| `fine.rate` | `5.0` | Fine per day (PKR) |
| `grace.period` | `2` | Grace days before fine |
| `max.books` | `5` | Max books per member |
| `currency` | `PKR` | Display currency |
| `ui.theme` | `light` | `light` or `dark` |

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
