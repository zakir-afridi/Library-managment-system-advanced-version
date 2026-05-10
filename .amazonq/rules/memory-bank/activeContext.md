# Active Context

## Current State
- Architecture: Branch-Per-Module Tree — each feature is an independent branch with Module/Service/Controller
- Core CRUD for books, members, students, employees is implemented
- Issue/Return with fine calculation working (PKR 5/day, 2-day grace period)
- Dashboard with `DashboardCache` (5-min TTL), charts auto-refresh every 30s
- Archive/Unarchive for books, members, students, employees with reason tracking (`archive_log`)
- Reservation system (`ReservationService`) exists
- Reports (PDF/Excel) via `ReportService` + `PrintService`
- Global search via `GlobalSearchService` + `SearchTrie` (300ms debounce, case-insensitive)
- Settings UI (`SettingsController`) persists to `AppConfig` → `libra_config.properties`
- Theme switching (light/dark) via `ThemeManager`
- Session management with 30-min auto-logout
- Password recovery: Login → key `03150315` → `ForgotPasswordController` → `ResetPasswordController`
- Data seeder populates sample data on first launch
- Toast notifications + activity log (audit trail)

## Known Issues / Incomplete Areas
- Forgot/Reset password FXML not yet wired to controllers
- `students/StudentController` is an empty stub — real impl in `ui/AddStudentController`
- Legacy `ui/LoginController` duplicates `controller/LoginController` (harmless, not wired)

## Next Steps (Planned)
1. Wire forgot/reset password FXML + controllers
2. Student CRUD UI (FXML + StudentService confirmed)
3. Archive students/employees from UI
4. Barcode scanner integration (P1)
5. Email overdue notifications via SMTP (P1)
