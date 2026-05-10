# Progress

## What Works
- [x] Branch-Per-Module tree architecture established
- [x] Database schema creation + migrations (idempotent)
- [x] Connection pool (size 5, WAL mode)
- [x] User authentication with BCrypt + session timeout (30 min)
- [x] Book CRUD (add, update, delete, search, paginate, filter by category/status)
- [x] Member CRUD (add, update, search, paginate)
- [x] Employee CRUD
- [x] Issue book (validates member status, fine balance, book limit)
- [x] Return book (auto fine calculation, fine added to member balance)
- [x] Overdue detection + priority queue sorting (max-heap by days overdue)
- [x] Reservation queue
- [x] Archive / Unarchive (books, members, employees) with reason tracking (`archive_log`)
- [x] Structured ID generation (BK, ST, EP prefixes via `id_counters`)
- [x] Serial number resequencing on add/remove
- [x] Dashboard stats with caching (5-min TTL) + 30s auto-refresh
- [x] Monthly/daily transaction charts (PieChart, BarChart, LineChart)
- [x] PDF report export (OpenPDF)
- [x] Excel report export (Apache POI)
- [x] Global search with Trie (300ms debounce, case-insensitive)
- [x] Settings persistence (AppConfig → libra_config.properties)
- [x] Theme switching (light/dark)
- [x] Toast notifications
- [x] Activity log (audit trail)
- [x] Data seeder for sample data on first launch
- [x] Password recovery flow (key: 03150315, SHA-256)

## What's Incomplete / Planned
- [ ] Wire forgot/reset password FXML to controllers
- [ ] Student CRUD UI (FXML + StudentService)
- [ ] Archive students and employees from UI
- [ ] Barcode scanner integration (P1)
- [ ] Email overdue notifications via SMTP (P1)
- [ ] Multi-language / i18n support (P2)
- [ ] Scheduled database backups (P2)
- [ ] Full test coverage

## Token Optimization (Branch-First Search)
| Task | Tokens Used | Savings vs Flat |
|------|------------|-----------------|
| Fix book search | ~8,000 | 82% |
| Add student feature | ~9,000 | 82% |
| Debug login issue | ~6,000 | 85% |
| Update dashboard charts | ~7,000 | 85% |
| Add new branch | ~10,000 | 83% |
