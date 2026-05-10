# Tech Context

## Stack
| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| UI Framework | JavaFX | 21.0.1 |
| Database | SQLite (via sqlite-jdbc) | 3.46.1.3 |
| Password Hashing | jBCrypt | 0.4 |
| Excel Export | Apache POI (poi-ooxml) | 5.2.5 |
| PDF Export | OpenPDF (librepdf) | 1.3.43 |
| Build | Maven | 3.x |

## Build
- `pom.xml` at `Library-managment-system-advanced-version/Library/pom.xml`
- Artifact: `library-management-system-2.0.0.jar`
- Main class: `com.library.LibraCoreApp`
- Run scripts: `run.bat`, `run.ps1`, `run-production.bat`
- JavaFX SDK bundled at `Library/javafx-sdk/` (for non-Maven runs)
- Build: `mvn clean package` | Run: `mvn javafx:run`

## pom.xml Key Dependencies
```xml
<!-- JavaFX -->
<dependency><groupId>org.openjfx</groupId><artifactId>javafx-controls</artifactId><version>21</version></dependency>
<dependency><groupId>org.openjfx</groupId><artifactId>javafx-fxml</artifactId><version>21</version></dependency>
<!-- SQLite -->
<dependency><groupId>org.xerial</groupId><artifactId>sqlite-jdbc</artifactId><version>3.46.1.3</version></dependency>
<!-- BCrypt -->
<dependency><groupId>org.mindrot</groupId><artifactId>jbcrypt</artifactId><version>0.4</version></dependency>
<!-- Excel -->
<dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.2.5</version></dependency>
<!-- PDF -->
<dependency><groupId>com.github.librepdf</groupId><artifactId>openpdf</artifactId><version>1.3.43</version></dependency>
```

## Performance Targets
- App startup: < 3 seconds
- DB query: < 500ms for 10,000 records
- Search response: < 200ms (with 300ms debounce)
- Chart rendering: < 1 second

## Database Schema (tables)
- `users` — login accounts (username, password_hash, role, status, failed_attempts)
- `books` — catalogue (isbn, book_name, author, category, quantity, available_qty, status, book_code, serial_no, is_archived)
- `members` — library members (student_id, name, department, program, status, fine_balance, member_code, serial_no, is_archived)
- `students` — student records (student_id, full_name, department, year, email, phone, is_archived)
- `transactions` / `issue_records` — issue/return (book_id, member_id, issue_date, due_date, return_date, fine_amount, status)
- `reservations` — queue (book_id, member_id, queue_position, status)
- `employees` — staff (employee_code, name, designation, salary, status, serial_no, is_archived)
- `settings` — key/value store
- `activity_log` — audit trail (user_id, action, details, timestamp)
- `archive_log` — archive reasons (record_type, record_id, reason, archived_at, archived_by)
- `id_counters` — structured ID counters (BK, ST, MB, EP)
- `admin`, `librarydetails` — legacy tables (backward compatibility)

## Indexes
- books: isbn, book_name, category, author, status
- members: student_id, name, email, status
- transactions: member_id+status, due_date, book_id
- reservations: book_id+status
- employees: employee_code, name, status

## Config File
`libra_config.properties` — next to JAR, loaded/saved by `AppConfig` singleton.
- Loan days (14), fine rate (PKR 5/day), grace period (2 days)
- Max books per member (5), items per page (10), currency (PKR)
- Theme (light/dark), overdue alerts, due-soon threshold

## Resource Paths
All FXML, CSS, images under: `src/main/resources/com/library/ui/`
Loaded via: `getClass().getResource("/com/library/ui/<file>.fxml")`

## CSS Files
- `main.css` — main theme (primary `#2c3e50`, secondary `#3498db`, accent `#e74c3c`)
- `login.css` — login page
- `dashboard.css` — cards/charts
- `forms.css` — form inputs
- `tables.css` — data tables
- `charts.css` — chart styling
