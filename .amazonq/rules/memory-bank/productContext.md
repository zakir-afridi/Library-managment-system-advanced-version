# Product Context

## Problem Solved
Libraries need to track books, members, students, borrowing, returns, fines, and staff — fully offline, no network or external DB server required.

## How It Works
- Librarians log in and manage books/members/students through a JavaFX GUI
- Books are issued to members/students; due dates and fines calculated automatically (PKR 5/day, 2-day grace)
- Overdue books surface in a priority queue sorted by days overdue
- Reservations queue members waiting for unavailable books
- Reports exported to PDF or Excel
- Dashboard charts auto-refresh every 30 seconds

## Branch Responsibilities
| Branch | What It Does |
|--------|-------------|
| auth/ | Login, forgot password (key: 03150315), reset password |
| dashboard/ | Stats cards, PieChart (categories), BarChart (monthly issues), LineChart (trends) |
| books/ | Add/edit/delete/search books, archive/restore, ISBN search |
| members/ | Member CRUD, membership types (Regular, Premium, Student, Faculty) |
| students/ | Student CRUD, student ID, department, year |
| employees/ | Employee CRUD, role, department, salary |
| issuing/ | Issue/return books, fine calculation, issue history |
| reports/ | Borrowing reports, overdue reports, PDF/Excel export |
| shared/ | DB connection, all models, utilities (used by all branches) |

## Key User Flows
1. Login → LoginPage.fxml → LoginController → SessionManager
2. Forgot Password → ForgotPasswordController → key `03150315` → ResetPasswordController → DB update
3. Add Book → AddBookForm.fxml → AddBookController → BookService → DB
4. Issue Book → IssueReturnBooksForm.fxml → IssueReturnController → TransactionService
5. Return Book → same form → TransactionService.returnBook() → fine calculated
6. Dashboard → ModernDashboard.fxml → ModernDashboardController → DashboardCache
7. Reports → ReportsView.fxml → ReportsController → ReportService (PDF/Excel)
8. Archive → ArchiveView.fxml → ArchiveController → BookService/MemberService

## Configuration (libra_config.properties)
Managed by `AppConfig` singleton, sits next to the JAR:
- Loan days: 14 | Fine rate: PKR 5/day | Grace period: 2 days
- Max books per member: 5 | Items per page: 10 | Currency: PKR
- Theme: light/dark | Overdue alerts | Due-soon threshold

## UI/UX Design
- Color scheme: primary `#2c3e50`, secondary `#3498db`, accent `#e74c3c`, success `#27ae60`, warning `#f39c12`
- Min window: 1024×768; sidebar collapses to icons-only below 1200px
- Search: real-time, 300ms debounce, case-insensitive SQL LIKE
- Tables: sortable, paginated (20 rows/page), row hover
- Forms: two-column layout, inline validation
