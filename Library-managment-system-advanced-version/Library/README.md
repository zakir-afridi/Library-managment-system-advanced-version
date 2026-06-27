# LibraCore Pro v3.0.0 — Library Management System

> A production-grade desktop application for managing library books, members, employees, and transactions.  
> Built with **Java 21 LTS**, **JavaFX 21.0.2**, **SQLite 3.49 + HikariCP**, and **Maven 3.9+**.  
> Runs on Windows, macOS, and Linux.

---

## What's New in v3.0.0

| Feature | Detail |
|---|---|
| **Java 21 LTS** | Upgraded from Java 17. Virtual threads used for all async tasks. |
| **HikariCP Connection Pool** | Replaced hand-rolled pool. Pool size 10, WAL mode, FK constraints. |
| **Open Library API** | Auto-fill book title/author/publisher/year/cover by ISBN on Add Book form. |
| **Open-Meteo Weather Widget** | Live weather on Dashboard. Free, no API key required. |
| **Frankfurter Exchange Rates** | View fines in USD/EUR/GBP/AED. Cached 24h. |
| **Email Notifications** | Welcome emails + overdue reminders via Gmail SMTP. |
| **ZXing Barcode/QR Scanner** | Decode ISBNs from image files; generate QR codes for member ID cards. |
| **SLF4J + Logback** | Structured logging with daily rolling files in `logs/`. |
| **Global Exception Handler** | No more silent crashes — shows error dialog with stack trace. |
| **Auto Backup Scheduler** | Daily database backup to `~/.libracore/backups/`, keeps last 30. |
| **Overdue Email Notifications** | Hourly background check sends overdue reminders. |
| **Book Metadata Cache** | 30-day SQLite cache for Open Library responses. |
| **InputValidator** | ISBN-10/13 checksum validation, Pakistan phone format, RFC email. |
| **AsyncRunner** | Virtual-thread helper eliminates boilerplate Task/Thread code. |
| **v3 Security** | 15-minute account lockout after 5 failed attempts (was 3, status-only). |
| **Settings: Email & Weather** | SMTP config and weather city configurable from Settings screen. |

---

## Current Module Status

| Module | Status |
|---|---|
| Login / Authentication (BCrypt + RBAC) | ✅ Complete |
| Dashboard (KPI cards, charts, weather widget) | ✅ Complete |
| Book Management (CRUD, ISBN auto-fill, barcode scan, archive) | ✅ Complete |
| Member Management (CRUD, welcome email, archive) | ✅ Complete |
| Employee Management (CRUD, archive, PDF profile) | ✅ Complete |
| Issue / Return System | ✅ Complete |
| Archive View | ✅ Complete |
| Global Search | ✅ Complete |
| Reports (PDF: Overdue, Circulation, Inventory, Fines) | ✅ Complete |
| Settings (theme, loan config, email, weather, backup) | ✅ Complete |
| Open Library ISBN Lookup | ✅ Complete |
| Exchange Rate Display | ✅ Complete |
| Weather Widget | ✅ Complete |
| Email Notifications (Welcome + Overdue) | ✅ Complete |
| Auto Backup Scheduler | ✅ Complete |
| ZXing Barcode from Image | ✅ Complete |
| HikariCP Pool | ✅ Complete |
| SLF4J + Logback | ✅ Complete |
| Global Exception Handler | ✅ Complete |
| Virtual Threads (AsyncRunner) | ✅ Complete |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 LTS (Virtual Threads) |
| UI Framework | JavaFX 21.0.2 |
| Database | SQLite 3.49 (embedded) |
| Connection Pool | HikariCP 5.1.0 |
| Build Tool | Maven 3.9+ |
| Password Hashing | jBCrypt 0.4 |
| PDF Reports | OpenPDF 2.0.3 |
| Excel/CSV | Apache POI 5.4.0 |
| JSON | Jackson 2.18.2 |
| Logging | SLF4J 2.0 + Logback 1.5 |
| Email | Jakarta Mail 2.0 (Gmail SMTP) |
| Barcode/QR | ZXing 3.5.3 |
| Book Metadata | Open Library API (free, no key) |
| Weather | Open-Meteo API (free, no key) |
| Exchange Rates | Frankfurter API (free, no key) |

---

## Prerequisites

- **Java 17 or higher** — Java 21 recommended: [Adoptium JDK](https://adoptium.net/)
- **Maven 3.9+** — [Download Maven](https://maven.apache.org/download.cgi)

```bash
java -version   # should show 17+ (21 recommended)
```

---

## Installation & First Run

### Windows — One Command

```bat
run.bat
```

Pass `--rebuild` to force a fresh Maven build:  
```bat
run.bat --rebuild
```

### Manual Maven

```bash
mvn clean package -DskipTests
```

Then launch with `run.bat` or the generated `target/LibraCore-Pro-3.0.0.jar`.

### First Launch

On first launch, the app:
1. Creates `~/.libracore/library.db` (user home directory)
2. Applies the full schema (12 tables, 20+ indexes)
3. Seeds the default admin user

---

## Login Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `admin` | Admin (full access) |

---

## Email Configuration (Optional)

In **Settings → Email**, enter your SMTP credentials:

| Setting | Example (Gmail) |
|---|---|
| SMTP Host | `smtp.gmail.com` |
| SMTP Port | `587` |
| Username | `you@gmail.com` |
| Password | App password (not your Google password) |

Gmail requires an [App Password](https://support.google.com/accounts/answer/185833) with 2FA enabled.

---

## Weather Widget (Optional)

In **Settings → Weather**, set your city name (e.g., `Peshawar`).  
Uses [Open-Meteo](https://open-meteo.com/) — completely free, no API key needed.

---

## Database Location

The database is stored at:
```
~/.libracore/library.db
```
Backups are stored at:
```
~/.libracore/backups/library_backup_YYYY-MM-DD_HH-mm-ss.db
```

---

## Logs

Application logs are written to:
```
logs/libracore.log
logs/libracore.YYYY-MM-DD.log  (daily rotation, 30-day retention)
```

---

## Project Structure

```
Library/
├── src/main/java/com/library/
│   ├── LibraCoreApp.java              ← Entry point (v3)
│   ├── api/                           ← NEW: OpenLibraryClient, WeatherClient, ExchangeRateClient
│   ├── auth/
│   ├── books/
│   ├── cache/
│   ├── config/
│   ├── controller/                    ← JavaFX FXML controllers
│   ├── dashboard/
│   ├── database/                      ← DatabaseConnection + HikariConnectionPool
│   ├── email/                         ← NEW: EmailService, EmailTemplates
│   ├── employees/
│   ├── issuing/
│   ├── members/
│   ├── model/                         ← Book, Member, Employee, User, BookMetadata, WeatherInfo
│   ├── reports/
│   ├── repository/
│   ├── scanner/                       ← NEW: BarcodeScanner (ZXing)
│   ├── security/
│   ├── service/                       ← All business logic
│   ├── shared/
│   ├── students/
│   ├── ui/
│   └── util/                          ← AsyncRunner, GlobalExceptionHandler, InputValidator
├── src/main/resources/
│   ├── com/library/ui/                ← FXML + CSS + images
│   └── logback.xml                    ← NEW: Logging config
├── pom.xml                            ← Java 21, all v3 deps
├── run.bat                            ← One-click Windows launcher
└── README.md
```

---

## Version History

| Version | Date | Changes |
|---|---|---|
| v3.0.0 | 2026 | Java 21, HikariCP, Open Library ISBN API, Weather widget, Exchange rates, Email notifications, ZXing barcode, SLF4J logging, Global exception handler, Auto backup scheduler, Virtual threads, 15-min lockout |
| v2.0.0 | 2025 | BCrypt auth, RBAC, themes, charts, PDF reports, Trie search, Employee module, Archive system, Structured IDs, Pagination |
| v1.0.0 | 2023 | Initial release |

---

*Built with JavaFX 21 · SQLite · HikariCP · Maven · OpenPDF · jBCrypt · Apache POI · ZXing · Jackson · SLF4J*
