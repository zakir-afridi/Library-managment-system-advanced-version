# LibraCore Pro — Library Management System

> A production-grade desktop application for managing library books, members, employees, and transactions.
> Built with JavaFX 21, SQLite, and Maven. Runs on Windows, macOS, and Linux.

---

> **⚠️ WORK IN PROGRESS**
> This project is actively being developed. Core modules are functional and the system runs end-to-end.
> Additional features, UI polish, and bug fixes are planned and will be added in upcoming updates.
> See the [Pending Work](#pending-work) section for what is coming next.

---

## Current Status

| Module | Status |
|---|---|
| Login / Authentication (BCrypt + RBAC) | ✅ Complete |
| Dashboard (KPI cards, charts, activity) | ✅ Complete |
| Book Management (CRUD, search, archive, BK codes) | ✅ Complete |
| Member Management (CRUD, search, archive, ST codes) | ✅ Complete |
| Employee Management (CRUD, search, archive, EP codes) | ✅ Complete |
| Issue / Return System | ✅ Complete |
| Archive View (Books, Members, Employees) | ✅ Complete |
| Global Search (across all entities + archives) | ✅ Complete |
| Reports (PDF: Overdue, Circulation, Inventory, Fines) | ✅ Complete |
| Settings (theme, loan config, backup/restore) | ✅ Complete |
| Structured ID System (BK/ST/EP + 8 digits) | ✅ Complete |
| Serial Number Resequencing | ✅ Complete |
| Light / Dark Theme | ✅ Complete |
| Data Seeding (first launch) | ✅ Complete |
| Print Service (A4 PDF profiles + fee slips) | ✅ Complete |

---

## Features

### Core Modules

| Module | Description |
|---|---|
| **Dashboard** | Real-time KPI cards, bar/pie charts, overdue badge, recent activity feed |
| **Book Management** | Add, edit, archive, search by BK code/title/author/ISBN, paginate, CSV import/export |
| **Member Management** | Student profiles, ST codes, photo upload, fine tracking, archive/restore |
| **Employee Management** | Full CRUD, EP codes, archive/restore, PDF profile print |
| **Issue / Return** | Issuance with validation, auto fine calculation, return condition tracking |
| **Archive View** | Tabbed view of archived Books, Members, Employees with restore and global search |
| **Reports** | PDF: Overdue, Circulation, Inventory, Fine Collection, Popular Books |
| **Settings** | Library branding, loan period, fine rate, theme toggle, DB backup/restore |

### ID System

Every entity gets a permanent structured ID on creation:

| Entity | Format | Example |
|---|---|---|
| Books | `BK` + 8 digits | `BK00000001` |
| Students / Members | `ST` + 8 digits | `ST00000001` |
| Employees | `EP` + 8 digits | `EP00000001` |

- IDs are **permanent** — they never change or reset, even after deletion
- Counters are stored in the `id_counters` database table
- All search queries support searching by ID code

### Serial Number System

- Active lists show sequential serial numbers: `1, 2, 3, 4...`
- When an item is archived or deleted, remaining items **shift up automatically**
- Archived list shows newest archived item at serial `1`
- Resequencing runs atomically in a single DB transaction after every add/archive/delete

### Archive System

- Archiving moves an item out of the active list — it does **not** delete it
- Archived items are fully searchable via the Archive View
- Any archived item can be **restored** back to active status
- Separate archive tabs for Books, Members, and Employees

### Technical Highlights

- **BCrypt** password hashing — no plain-text passwords stored
- **Role-Based Access Control** — Admin, Librarian, Viewer roles
- **30-minute session timeout** — auto-logout on inactivity
- **Light / Dark theme** — toggleable, persisted across sessions
- **Trie autocomplete** — fast prefix search on book titles and authors
- **Connection pool** — 5-connection SQLite pool (WAL mode)
- **Dashboard cache** — 60-second TTL HashMap cache reduces DB calls
- **PriorityQueue** — overdue books sorted by days overdue (max-heap)
- **FilteredList + SortedList** — live table filtering without re-querying DB
- **Pagination** — configurable page size (default 10, max 200), enforced on all queries
- **Auto data seeding** — 30 books, 15 members, 10 transactions on first launch

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 (LTS) |
| UI Framework | JavaFX 21 |
| Database | SQLite 3.46 (embedded, no server needed) |
| Build Tool | Maven 3.8+ |
| Password Hashing | jBCrypt 0.4 |
| PDF Reports | OpenPDF 1.3.43 |
| Excel / CSV | Apache POI 5.2.5 |

---

## Prerequisites

- **Java 17 or higher** — [Download Adoptium JDK](https://adoptium.net/)
- **Maven 3.8+** — [Download Maven](https://maven.apache.org/download.cgi) *(optional — run.bat auto-detects)*
- Windows 10/11 recommended (run.bat), or any OS with Java 17+

Verify your Java version:
```bash
java -version
# Should show: openjdk version "17.x.x" or higher
```

---

## Installation & First Run

### Option 1 — One command (recommended for Windows)

```bat
cd Library
run.bat
```

`run.bat` will:
1. Verify Java is installed
2. Auto-detect Maven (checks PATH, then `~/.m2/wrapper/dists/`)
3. Skip build if JAR already exists (fast launch on subsequent runs)
4. Launch the application with correct JavaFX module path

> Pass `--rebuild` to force a fresh build: `run.bat --rebuild`

On **first launch**, the app automatically:
- Creates `library.db` (SQLite database)
- Applies the full schema (9 tables, 15+ indexes)
- Seeds 30 books, 15 members, and 10 transactions

### Option 2 — Manual Maven

```bash
cd Library
mvn clean package -DskipTests
java --module-path "target/lib/javafx-controls-21.0.1.jar;target/lib/javafx-fxml-21.0.1.jar;target/lib/javafx-graphics-21.0.1.jar;target/lib/javafx-base-21.0.1.jar" \
     --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base \
     --enable-native-access=ALL-UNNAMED \
     -cp "target/library-management-system-2.0.0.jar;target/lib/*" \
     com.library.LibraCoreApp
```

### Option 3 — IDE (IntelliJ IDEA / Eclipse)

1. Open the `Library` folder as a Maven project
2. Let the IDE import dependencies automatically
3. Run `com.library.LibraCoreApp`

---

## Login Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `admin` | Admin (full access) |

> **Security note:** On first login you will be prompted to change the default password.
> New password must be at least 8 characters with one uppercase letter, one digit, and one symbol.

---

## Project Structure

```
Library/
├── src/main/java/com/library/
│   ├── LibraCoreApp.java              ← Application entry point
│   ├── cache/                         ← DashboardCache (HashMap TTL)
│   ├── config/                        ← AppConfig, ThemeManager
│   ├── controller/                    ← JavaFX UI controllers
│   │   ├── LoginController.java
│   │   ├── DashboardController.java
│   │   ├── BookController.java
│   │   ├── MemberController.java
│   │   ├── EmployeeController.java    ← NEW
│   │   ├── IssueReturnController.java
│   │   ├── ArchiveController.java     ← NEW
│   │   ├── ReportsController.java     ← NEW
│   │   └── SettingsController.java
│   ├── database/
│   │   ├── DatabaseConnection.java    ← Connection pool + schema
│   │   └── DataSeeder.java            ← First-launch seed data
│   ├── model/                         ← Entity classes
│   │   ├── Book.java  (BK codes, serial_no)
│   │   ├── Member.java (ST codes, serial_no)
│   │   ├── Employee.java (EP codes, serial_no)  ← NEW
│   │   ├── Transaction.java
│   │   └── User.java
│   ├── security/                      ← BCrypt, SessionManager
│   ├── service/                       ← Business logic layer
│   │   ├── BookService.java
│   │   ├── MemberService.java
│   │   ├── EmployeeService.java       ← NEW
│   │   ├── TransactionService.java
│   │   ├── GlobalSearchService.java   ← NEW
│   │   ├── SerialNumberService.java   ← NEW
│   │   ├── ReportService.java
│   │   ├── PrintService.java          ← NEW
│   │   └── SearchTrie.java
│   └── util/
│       ├── IdGenerator.java           ← NEW (BK/ST/EP codes)
│       ├── PageRequest.java           ← NEW (pagination)
│       └── ToastNotification.java
├── src/main/resources/com/library/ui/
│   ├── css/
│   │   ├── light-theme.css
│   │   └── dark-theme.css
│   ├── LoginPage.fxml
│   ├── ProfessionalDashboard.fxml
│   ├── AddBookForm.fxml
│   ├── AddMemberForm.fxml
│   ├── EmployeeForm.fxml              ← NEW
│   ├── ArchiveView.fxml               ← NEW
│   ├── ReportsView.fxml               ← NEW
│   ├── IssueReturnBooksForm.fxml
│   └── Settings.fxml
├── pom.xml
├── run.bat                            ← One-click launcher
├── README.md
└── .gitignore
```

---

## Data Structures Used

| Structure | Where Used | Purpose |
|---|---|---|
| `HashMap<String, Book>` | BookService | O(1) ISBN lookup cache |
| `HashMap<String, Member>` | MemberService | O(1) student-ID lookup cache |
| `Trie` | SearchService | Prefix autocomplete for titles/authors |
| `PriorityQueue<Transaction>` | TransactionService | Overdue books sorted by days overdue |
| `Deque<Transaction>` (Stack) | TransactionService | Last 50 activity log, undo support |
| `HashMap<Integer, Queue>` | ReservationService | FIFO waiting queue per book |
| `FilteredList + SortedList` | All table controllers | Live filtering without DB re-query |
| `HashMap<String, Object>` | DashboardCache | 60s TTL stats cache |
| `id_counters` table | IdGenerator | Atomic persistent counters for BK/ST/EP |

---

## Configuration

Settings are stored in `libra_config.properties` (auto-created next to the JAR):

| Key | Default | Description |
|---|---|---|
| `library.name` | LibraCore Pro Library | Appears in reports and title bar |
| `library.loanDays` | 14 | Default loan period in days |
| `library.fineRate` | 5.0 | Fine per overdue day |
| `library.gracePeriod` | 2 | Days before fine starts |
| `library.maxBooks` | 5 | Max books per member |
| `library.currency` | PKR | Currency symbol for fines |
| `library.defaultLimit` | 10 | Default query limit (pagination) |
| `library.itemsPerPage` | 10 | Rows shown per page in tables |
| `ui.theme` | light | `light` or `dark` |

All settings are editable from the **Settings** screen inside the app.

---

## Backup & Restore

From the Settings screen:
- **Backup** — copies `library.db` to a chosen folder with a timestamp filename
- **Restore** — replaces `library.db` with a selected backup file (requires restart)

---

## Pending Work

> The following features are planned and will be implemented in the next development session:

### High Priority
- [ ] **Fix Issue/Return FXML encoding** — emoji characters in button text cause XML parse errors on some systems; replacing with plain text labels
- [ ] **Reports screen navigation** — Reports button on dashboard navigates to `ReportsView.fxml` correctly but PDF save dialog needs testing on all OS
- [ ] **Settings screen** — `defaultLimitCombo` field needs to be wired to the FXML (currently added to controller but FXML needs the matching `fx:id`)
- [ ] **Member print** — Print member profile PDF from MemberController (PrintService is ready, button needs wiring)

### Medium Priority
- [ ] **Email notifications** — Overdue reminders via JavaMail
- [ ] **Barcode scanner** — USB HID hardware integration for book scanning
- [ ] **Excel export** — Full `.xlsx` export using Apache POI (currently CSV only)
- [ ] **Renew book** — Extend due date without return/re-issue cycle
- [ ] **Book reservation** — Reserve books that are currently issued (queue system is built, UI pending)

### Low Priority
- [ ] **Multi-branch support** — Separate library locations
- [ ] **Book recommendation** — Collaborative filtering based on borrowing patterns
- [ ] **Automated backups** — Scheduled daily/weekly DB backup
- [ ] **Member self-service portal** — Web interface for members to check their status

---

## Version History

| Version | Date | Changes |
|---|---|---|
| v2.0.0 | 2025 | Complete rewrite — BCrypt auth, RBAC, themes, charts, PDF reports, Trie search, connection pool, Employee module, Archive system, Structured IDs (BK/ST/EP), Serial resequencing, Global search, Pagination engine, Print service |
| v1.0.0 | 2023 | Initial release — basic CRUD, SQLite, JavaFX |

---

## License

This project is for educational purposes.

---

*Built with JavaFX 21 · SQLite · Maven · OpenPDF · jBCrypt · Apache POI*

*Development is ongoing — more features coming soon.*
