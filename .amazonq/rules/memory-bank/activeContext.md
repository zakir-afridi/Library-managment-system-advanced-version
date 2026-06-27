# Active Context

## Current State — LibraCore Pro v3.0.0 ✅ BUILT

- **JAR:** `target/LibraCore-Pro-3.0.0.jar` (845 KB) — 103 source files, 44 dependency JARs
- **JDK:** Java 24 (`C:\Program Files\Java\jdk-24`)
- **Maven:** `D:\maven\apache-maven-3.9.6\bin\mvn.cmd`
- **Build command:** `set "JAVA_HOME=C:\Program Files\Java\jdk-24" && "D:\maven\apache-maven-3.9.6\bin\mvn.cmd" clean package -DskipTests`
- **Launch:** `run.bat` (auto-detects Java + Maven, searches `D:\maven` first)

## v3 Upgrades Completed
- Java 24 + `--enable-preview` (pom.xml release updated from 21 → 24)
- HikariCP 5.1.0 connection pool replacing hand-rolled ArrayBlockingQueue
- SQLite 3.49.1.0 (upgraded from 3.46)
- Jackson 2.18.2 for API JSON parsing
- SLF4J + Logback structured logging (replaces System.out.println)
- GlobalExceptionHandler — no silent crashes
- AsyncRunner — virtual threads (Executors.newVirtualThreadPerTaskExecutor)
- OpenLibraryClient — free ISBN metadata fetch with 30-day DB cache
- ExchangeRateClient — Frankfurter free currency rates (24h cache)
- WeatherClient — Open-Meteo free weather, no API key (30min cache)
- BarcodeScanner (ZXing 3.5.3) — decode EAN/QR from image, generate QR codes
- EmailService (Jakarta Mail 2.0.1) — Gmail SMTP, email_queue table, 3 retries
- BackupScheduler — daily auto-backup to ~/.libracore/backups/, keeps last 30
- OverdueNotificationService — hourly overdue email reminders
- BookController — ISBN auto-fill from Open Library on focus-lost
- SettingsController — email/weather/backup UI panels added

## Bugs Fixed During Build
1. `pom.xml` — `--enable-preview` requires release matching JDK; changed 21 → 24
2. BOM (`\ufeff`) stripped from 9 files: BookService, EmployeeService, GlobalSearchService,
   MemberService, ReportService, ReservationService, SerialNumberService,
   TransactionService, SettingsController
3. `SettingsController.java` — duplicate class body removed (v2 appended after v3)
4. `BarcodeScanner.java` — missing `import com.google.zxing.common.BitMatrix` added

## Known Issues / Incomplete
- Weather widget FXML nodes (weatherCityLabel etc.) not present in ProfessionalDashboard.fxml
  — DashboardController checks for null before updating, so no crash
- Email + Weather sections in Settings.fxml not yet added (backend ready)
- Student CRUD UI (`AddStudentForm.fxml`) not wired from dashboard nav
- Forgot/Reset password FXML not wired (LoginController has inline dialogs as workaround)
- Legacy dead-code entry points (LibraryApp, ModernLibraryApp, etc.) still present

## Next Steps
1. Add weather widget nodes to ProfessionalDashboard.fxml
2. Add Email + Weather sections to Settings.fxml
3. Wire AddStudentForm.fxml to dashboard nav (StudentModule)
4. Remove legacy entry-point files
