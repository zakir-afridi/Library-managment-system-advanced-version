# Product Context

## Problem Solved
Libraries need to track books, members, students, borrowing, returns, fines, and staff — fully offline, no network or external DB server required.

## How It Works
- Librarians log in via `LoginPage.fxml`; session auto-expires after 30 minutes
- Books are issued to members; due dates and fines calculated automatically (PKR 5/day, 2-day grace via `FineCalculator`)
- Overdue books sorted by priority queue (max-heap, days overdue)
- Reservations queue members waiting for unavailable books (`ReservationService`)
- Reports exported to PDF or Excel on demand
- Dashboard auto-refreshes every 30 seconds (`DashboardCache`, 5-min TTL)

## Key User Flows
1. **Login**: `LoginPage.fxml` → `controller/LoginController` → `SessionManager.login()`
2. **Forgot Password**: `ForgotPasswordController` → `Constants.isValidRecoveryKey("03150315")` → `ResetPasswordController` → DB update
3. **Add Book**: `AddBookForm.fxml` → `ui/AddBookController` → `BookService` → DB
4. **Issue Book**: `IssueReturnBooksForm.fxml` → `controller/IssueReturnController` → `TransactionService`
5. **Return Book**: same form → `TransactionService.returnBook()` → `FineCalculator.calculate()` → fine added to member balance
6. **Dashboard**: `ModernDashboard.fxml` → `controller/ModernDashboardController` → `DashboardCache`
7. **Reports**: `ReportsView.fxml` → `controller/ReportsController` → `ReportService` (PDF/Excel)
8. **Archive**: `ArchiveView.fxml` → `controller/ArchiveController` → `BookService`/`MemberService`

## Configuration (`libra_config.properties` — next to JAR)
Managed by `AppConfig` singleton:
| Key | Default |
|-----|---------|
| `library.loanDays` | `14` |
| `library.fineRate` | `5.0` (PKR/day) |
| `library.gracePeriod` | `2` |
| `library.maxBooks` | `5` |
| `library.itemsPerPage` | `10` |
| `library.currency` | `PKR` |
| `ui.theme` | `light` |
| `notify.overdueAlert` | `true` |
| `notify.dueSoonDays` | `2` |

## UI/UX
- Color scheme: primary `#2c3e50`, secondary `#3498db`, accent `#e74c3c`, success `#27ae60`, warning `#f39c12`
- Min window: 1024×768; CSS files: `light-theme.css`, `dark-theme.css`, `Dashboard.css`, `ModernDashboard.css`, `Login.css`, `ComprehensiveForms.css`, `AddMember.css`
- Search: real-time, 300ms debounce, case-insensitive SQL LIKE + `SearchTrie`
- Tables: sortable, paginated (default 10 rows/page per config), row hover
- Forms: two-column layout, inline validation via `ValidationUtil`
- Toast notifications: `ToastNotification` util
- Audit trail: `activity_log` table
