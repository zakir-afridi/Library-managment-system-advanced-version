# Active Context

## Current State
- Branch-Per-Module architecture fully established; all 8 branches wired through `LibraCoreApp`
- `SharedModule.initDatabase()` → `DatabaseConnection.initialise()` → `DataSeeder.seedIfNeeded()` runs on every startup
- `SharedModule.saveConfig()` → `AppConfig.save()` runs on shutdown
- Auth: BCrypt login, 30-min session timeout (`SessionManager` + JavaFX `Timeline`)
- Book CRUD, Member CRUD, Employee CRUD — all working with paginated tables
- Issue/Return with `FineCalculator.calculate(dueDate, checkDate)` — reads grace + rate from `AppConfig`
- Dashboard: `DashboardCache` (5-min TTL), 30s auto-refresh, PieChart + BarChart + LineChart
- Archive/Unarchive for books, members, employees with `archive_log` tracking
- Reservation queue via `ReservationService`
- Reports PDF/Excel via `ReportService` + `PrintService`
- Global search: `GlobalSearchService` + `SearchTrie`, 300ms debounce
- Settings UI → `AppConfig` → `libra_config.properties`
- Theme: `ThemeManager.applyTheme(scene)`, CSS: `light-theme.css` / `dark-theme.css`
- Toast notifications: `ToastNotification`
- Audit trail: `activity_log` table
- ID generation: `BK`, `ST`, `EP` prefixes via `id_counters` table

## Known Issues / Incomplete
- `students/StudentController.java` is an **empty stub** — real impl is `ui/AddStudentController`
- Forgot/Reset password FXML not yet wired to controllers
- Student CRUD UI exists (`AddStudentForm.fxml` + `ui/AddStudentController`) but not wired from dashboard nav
- Archive students/employees not exposed from UI
- Legacy entry points (`LibraryApp`, `ModernLibraryApp`, `ProfessionalLibraryApp`) are dead code
- `ui/LoginController` duplicates `controller/LoginController` (harmless, not wired)

## Next Steps (Planned)
1. Wire `AddStudentForm.fxml` to `StudentModule` + dashboard nav
2. Wire forgot/reset password FXML to controllers
3. Archive students and employees from UI
4. Barcode scanner integration (P1)
5. Email overdue notifications via SMTP (P1)
