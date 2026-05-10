# Project Brief — LibraCore Pro

## What It Is
A desktop Library Management System built with Java 17 + JavaFX 21 + SQLite.
App name: **LibraCore Pro v2.0.0**
Architecture: **Branch-Per-Module Tree** (AI-optimized, token-efficient)
Main entry point: `com.library.LibraCoreApp` — the ONLY class with `main()`

## Core Goals
- Manage books, members, students, employees, and transactions (issue/return)
- Support reservations, archiving, fine calculation, and reporting
- Run fully offline with embedded SQLite (`library.db`)
- Export reports to PDF (OpenPDF) and Excel (Apache POI)
- Password recovery via universal recovery key (`03150315`)
- Each feature is an independent branch — developed, tested, and fixed in isolation

## Status
Work in progress — core features implemented, more features planned.

## Login
- Default credentials: `admin` / `admin` (force password change on first login)
- Roles: `ADMIN`, `LIBRARIAN`
- Password recovery key: `03150315` (in `Constants.java`, SHA-256 hashed for comparison)

## 8 Feature Branches
| Branch | Responsibility |
|--------|---------------|
| auth/ | Login, forgot password, reset password |
| dashboard/ | Stats cards, PieChart, BarChart, LineChart, auto-refresh |
| books/ | CRUD, search by title/author/ISBN, archive/restore |
| members/ | CRUD, search, membership types (Regular, Premium, Student, Faculty) |
| students/ | CRUD, student ID, department, year |
| employees/ | CRUD, employee ID, role, department, salary |
| issuing/ | Issue/return books, fine calculation (PKR 5/day), history |
| reports/ | Borrowing reports, overdue reports, PDF/Excel export |

## Shared Branch
`shared/` contains ALL models, database code, and utilities used by every branch.

## Future Roadmap (Priority Order)
- P1: Barcode scanner integration, email overdue notifications (SMTP)
- P2: Multi-language (i18n), scheduled DB backups
- P3: Cloud sync, mobile companion app, AI book recommendations
