# Library Management System

A clean, professional Java-based Library Management System built with JavaFX and SQLite.

## Features

- **Book Management**: Add, update, delete, and search books
- **Member Management**: Manage library members
- **Issue/Return System**: Track book borrowing and returns
- **Dashboard**: Overview with statistics and charts
- **SQLite Database**: Embedded database, no server required
- **Clean Architecture**: Well-organized code structure

## Project Structure

```
Library/
├── src/main/java/com/library/
│   ├── LibraryManagementSystem.java  ← Main class
│   ├── model/                        ← Data models
│   ├── service/                      ← Business logic
│   ├── repository/                   ← Database operations
│   ├── ui/                          ← UI controllers
│   └── util/                        ← Utilities
├── src/main/resources/com/library/ui/
│   ├── *.fxml                       ← UI layouts
│   ├── css/                         ← Stylesheets
│   └── images/                      ← Icons and images
├── lib/                             ← Dependencies
└── pom.xml                          ← Maven configuration
```

## How to Run

### Method 1: Using run.bat (Windows)
```bash
run.bat
```

### Method 2: Using IDE (Recommended)
1. Open the `Library` folder in IntelliJ IDEA or Eclipse
2. Run `com.library.LibraryManagementSystem`

### Method 3: Command Line
```bash
cd Library
java -cp "build/classes;lib/*" com.library.LibraryManagementSystem
```

## Login Credentials

- **Username**: `admin`
- **Password**: `admin`

## Database

- **Type**: SQLite (embedded)
- **File**: `library.db` (created automatically)
- **Location**: Library folder

## Requirements

- Java 17 or higher
- No additional setup required

## Architecture

This project follows clean architecture principles:

- **Model**: Data entities (Book, Member, IssueRecord)
- **Repository**: Database access layer
- **Service**: Business logic layer
- **UI**: User interface controllers
- **Util**: Helper utilities

## Technologies Used

- **Java 17**
- **JavaFX 21** - User interface
- **SQLite** - Database
- **Maven** - Build tool

---

**The project is not fully ready so we will continue to work on it and add more features in the future.**