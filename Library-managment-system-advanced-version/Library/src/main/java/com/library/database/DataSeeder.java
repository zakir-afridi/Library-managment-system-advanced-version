package com.library.database;

import com.library.security.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enterprise Realistic Data Seeder for LibraCore Pro.
 * Seeds:
 *  - 120+ Books across diverse categories, authors, and publishers
 *  - 220+ Members / Students with departments, semesters, and realistic fines/dues
 *  - 105+ Employees / Staff across designations and departments
 *  - 250+ Transactions (Issued, Returned, Overdue, Lost) spanning the past 12 months
 *  - 40+ Reservations (Pending, Fulfilled)
 *  - 150+ Activity / Audit Logs
 */
public class DataSeeder {

    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        System.out.println("==> Running DataSeeder directly...");
        DatabaseConnection.initialise();
        try (Connection c = DatabaseConnection.getConnection()) {
            seedIfEmpty(c);
            System.out.println("==> Verifying counts in database:");
            System.out.println("    Total Books:        " + getCount(c, "books"));
            System.out.println("    Total Members:      " + getCount(c, "members"));
            System.out.println("    Total Employees:    " + getCount(c, "employees"));
            System.out.println("    Total Transactions: " + getCount(c, "transactions"));
            System.out.println("    Total Reservations: " + getCount(c, "reservations"));
            System.out.println("    Total Audit Logs:   " + getCount(c, "activity_log"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void seedIfNeeded() {
        try (Connection c = DatabaseConnection.getConnection()) {
            seedIfEmpty(c);
        } catch (SQLException e) {
            LOG.error("Failed to seedIfNeeded: {}", e.getMessage());
        }
    }

    public static void seedIfEmpty(Connection c) {
        try {
            int bookCount = getCount(c, "books");
            int memberCount = getCount(c, "members");

            if (bookCount < 20 || memberCount < 20) {
                LOG.info("Seeding comprehensive realistic demo dataset into database...");
                c.setAutoCommit(false);

                seedBooks(c);
                seedMembers(c);
                seedEmployees(c);
                seedTransactions(c);
                seedReservations(c);
                seedActivityLogs(c);
                updateIdCounters(c);

                c.commit();
                c.setAutoCommit(true);
                LOG.info("✅ Successfully seeded realistic enterprise dataset!");
            }
        } catch (SQLException e) {
            LOG.error("DataSeeder failed to populate database: {}", e.getMessage(), e);
            try { c.rollback(); c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private static int getCount(Connection c, String table) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── 1. SEED 120+ BOOKS ───────────────────────────────────────────────────

    private static void seedBooks(Connection c) throws SQLException {
        String sql = """
            INSERT INTO books (isbn, book_name, author, publisher, publication_year, edition,
                               category, description, quantity, available_qty, status,
                               shelf_location, book_code, serial_no, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String[][] bookData = {
            // Computer Science & AI
            {"978-0134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley", "2018", "3rd", "Computer Science", "Definitive guide to best practices in modern Java programming.", "12", "9", "Available", "Shelf A-101"},
            {"978-0132350884", "Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin", "Prentice Hall", "2008", "1st", "Software Engineering", "A code of conduct for professional programmers.", "15", "11", "Available", "Shelf A-102"},
            {"978-0262033848", "Introduction to Algorithms", "Thomas H. Cormen", "MIT Press", "2009", "3rd", "Computer Science", "Comprehensive textbook on algorithms and data structures.", "20", "14", "Available", "Shelf A-103"},
            {"978-1491957660", "Designing Data-Intensive Applications", "Martin Kleppmann", "O'Reilly Media", "2017", "1st", "Software Engineering", "The big ideas behind reliable, scalable, and maintainable systems.", "10", "6", "Available", "Shelf A-104"},
            {"978-0262035613", "Deep Learning", "Ian Goodfellow", "MIT Press", "2016", "1st", "Artificial Intelligence", "Foundational text on deep neural networks and machine learning.", "14", "8", "Available", "Shelf A-105"},
            {"978-1449370756", "Hands-On Machine Learning with Scikit-Learn, Keras, and TensorFlow", "Aurélien Géron", "O'Reilly Media", "2019", "2nd", "Artificial Intelligence", "Practical guide to building intelligent systems in Python.", "16", "10", "Available", "Shelf A-106"},
            {"978-0134494166", "Clean Architecture", "Robert C. Martin", "Prentice Hall", "2017", "1st", "Software Engineering", "A craftsman's guide to software structure and design.", "12", "8", "Available", "Shelf A-107"},
            {"978-0321125217", "Domain-Driven Design", "Eric Evans", "Addison-Wesley", "2003", "1st", "Software Engineering", "Tackling complexity in the heart of software systems.", "8", "5", "Available", "Shelf A-108"},
            {"978-0135957059", "The Pragmatic Programmer", "David Thomas", "Addison-Wesley", "2019", "20th Anniv", "Software Engineering", "Your journey to mastery in software craft.", "14", "10", "Available", "Shelf A-109"},
            {"978-0201633610", "Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma", "Addison-Wesley", "1994", "1st", "Computer Science", "Classic catalog of 23 fundamental software design patterns.", "18", "12", "Available", "Shelf A-110"},
            {"978-0131103627", "The C Programming Language", "Brian W. Kernighan", "Prentice Hall", "1988", "2nd", "Computer Science", "Authoritative guide to ANSI C by its creators.", "10", "7", "Available", "Shelf A-111"},
            {"978-1593279509", "Eloquent JavaScript", "Marijn Haverbeke", "No Starch Press", "2018", "3rd", "Web Development", "A modern introduction to programming with JavaScript.", "12", "9", "Available", "Shelf A-112"},
            {"978-1491950296", "Programming Rust", "Jim Blandy", "O'Reilly Media", "2021", "2nd", "Computer Science", "Fast, safe systems development using the Rust language.", "9", "6", "Available", "Shelf A-113"},
            {"978-1492040798", "Head First Java", "Kathy Sierra", "O'Reilly Media", "2022", "3rd", "Computer Science", "Visual and interactive brain-friendly guide to Java.", "15", "11", "Available", "Shelf A-114"},
            {"978-0134092669", "Operating System Concepts", "Abraham Silberschatz", "Wiley", "2018", "10th", "Computer Science", "Foundations of operating systems architecture and concurrency.", "16", "11", "Available", "Shelf A-115"},
            {"978-0133594140", "Computer Networks", "Andrew S. Tanenbaum", "Pearson", "2021", "6th", "Networking", "Principles and protocols of modern digital networking.", "14", "10", "Available", "Shelf A-116"},
            {"978-0078022159", "Database System Concepts", "Avi Silberschatz", "McGraw-Hill", "2019", "7th", "Databases", "Fundamental concepts of relational, NoSQL, and distributed databases.", "18", "13", "Available", "Shelf A-117"},
            {"978-1492055020", "Learning SQL", "Alan Beaulieu", "O'Reilly Media", "2020", "3rd", "Databases", "Mastering relational query generation and optimization.", "11", "8", "Available", "Shelf A-118"},
            {"978-1593279288", "Python Crash Course", "Eric Matthes", "No Starch Press", "2019", "2nd", "Computer Science", "A hands-on, project-based introduction to Python.", "20", "15", "Available", "Shelf A-119"},
            {"978-1491954249", "Fluent Python", "Luciano Ramalho", "O'Reilly Media", "2022", "2nd", "Computer Science", "Clear, concise, and idiomatic Python 3 programming.", "10", "7", "Available", "Shelf A-120"},

            // Data Science, Mathematics & Physics
            {"978-0387848570", "The Elements of Statistical Learning", "Trevor Hastie", "Springer", "2009", "2nd", "Data Science", "Data mining, inference, and machine learning principles.", "12", "8", "Available", "Shelf B-201"},
            {"978-1449369415", "Python for Data Analysis", "Wes McKinney", "O'Reilly Media", "2022", "3rd", "Data Science", "Data wrangling with Pandas, NumPy, and Jupyter.", "15", "10", "Available", "Shelf B-202"},
            {"978-0980232714", "Linear Algebra and Its Applications", "Gilbert Strang", "Wellesley-Cambridge", "2016", "5th", "Mathematics", "Definitive linear algebra textbook with matrix computation.", "18", "12", "Available", "Shelf B-203"},
            {"978-0471214724", "Advanced Engineering Mathematics", "Erwin Kreyszig", "Wiley", "2011", "10th", "Mathematics", "Applied mathematics for engineers and physicists.", "14", "9", "Available", "Shelf B-204"},
            {"978-0321818034", "Calculus: Early Transcendentals", "James Stewart", "Cengage Learning", "2015", "8th", "Mathematics", "Comprehensive calculus textbook with rigorous proofs.", "22", "16", "Available", "Shelf B-205"},
            {"978-0470547830", "Fundamentals of Physics", "David Halliday", "Wiley", "2013", "10th", "Physics", "Classic university physics textbook covering mechanics and electromagnetism.", "16", "11", "Available", "Shelf B-206"},
            {"978-0201021158", "The Feynman Lectures on Physics", "Richard P. Feynman", "Addison-Wesley", "2011", "New Mill.", "Physics", "Iconic three-volume masterclass on physics fundamentals.", "8", "4", "Available", "Shelf B-207"},
            {"978-0131495081", "Introduction to Electrodynamics", "David J. Griffiths", "Pearson", "2012", "4th", "Physics", "Authoritative undergraduate text on electrodynamics.", "10", "7", "Available", "Shelf B-208"},
            {"978-0131118928", "Introduction to Quantum Mechanics", "David J. Griffiths", "Cambridge Univ Press", "2018", "3rd", "Physics", "Clear exposition of wave mechanics and quantum state spaces.", "10", "6", "Available", "Shelf B-209"},
            {"978-0387953854", "Mathematical Methods for Physicists", "George B. Arfken", "Academic Press", "2012", "7th", "Physics", "Comprehensive handbook of mathematical techniques.", "12", "9", "Available", "Shelf B-210"},

            // Business, Economics, Finance & Management
            {"978-0060555665", "The Intelligent Investor", "Benjamin Graham", "Harper Business", "2006", "Revised", "Finance", "The definitive book on value investing and market risk.", "15", "10", "Available", "Shelf C-301"},
            {"978-0671027032", "Rich Dad Poor Dad", "Robert T. Kiyosaki", "Plata Publishing", "2017", "20th Anniv", "Finance", "Personal finance principles and cash flow mindset.", "25", "18", "Available", "Shelf C-302"},
            {"978-0062457714", "The Lean Startup", "Eric Ries", "Crown Business", "2011", "1st", "Business", "How modern entrepreneurs use continuous innovation to create value.", "16", "11", "Available", "Shelf C-303"},
            {"978-0062316097", "Zero to One: Notes on Startups", "Peter Thiel", "Crown Business", "2014", "1st", "Business", "How to build companies that create new things.", "18", "13", "Available", "Shelf C-304"},
            {"978-1591846444", "Good to Great", "Jim Collins", "Harper Business", "2001", "1st", "Management", "Why some companies make the leap and others do not.", "12", "8", "Available", "Shelf C-305"},
            {"978-0743269513", "The 7 Habits of Highly Effective People", "Stephen R. Covey", "Free Press", "2004", "15th Anniv", "Management", "Powerful lessons in personal change and leadership.", "20", "14", "Available", "Shelf C-306"},
            {"978-0143127741", "Thinking, Fast and Slow", "Daniel Kahneman", "Farrar, Straus and Giroux", "2011", "1st", "Economics", "Groundbreaking work on cognitive biases and behavioral economics.", "14", "9", "Available", "Shelf C-307"},
            {"978-0241988472", "Principles for Dealing with the Changing World Order", "Ray Dalio", "Avid Reader Press", "2021", "1st", "Economics", "Why nations succeed and fail across major debt cycles.", "10", "7", "Available", "Shelf C-308"},
            {"978-0525537601", "The Psychology of Money", "Morgan Housel", "Harriman House", "2020", "1st", "Finance", "Timeless lessons on wealth, greed, and happiness.", "18", "12", "Available", "Shelf C-309"},
            {"978-0078025426", "Principles of Corporate Finance", "Richard A. Brealey", "McGraw-Hill", "2019", "13th", "Finance", "Global standard for theory and practice of corporate finance.", "10", "6", "Available", "Shelf C-310"},

            // Medicine, Healthcare & Biology
            {"978-0323354745", "Robbins and Cotran Pathologic Basis of Disease", "Vinay Kumar", "Elsevier", "2020", "10th", "Medicine", "The gold standard reference text for pathology.", "12", "8", "Available", "Shelf D-401"},
            {"978-0071807296", "Harrison's Principles of Internal Medicine", "J. Larry Jameson", "McGraw-Hill", "2018", "20th", "Medicine", "The definitive clinical authority in internal medicine.", "10", "6", "Available", "Shelf D-402"},
            {"978-0323555982", "Guyton and Hall Textbook of Medical Physiology", "John E. Hall", "Elsevier", "2020", "14th", "Medicine", "Comprehensive physiology with clinical applications.", "15", "10", "Available", "Shelf D-403"},
            {"978-1496387080", "Lippincott Illustrated Reviews: Biochemistry", "Denise R. Ferrier", "LWW", "2017", "7th", "Medicine", "Visual presentation of medical biochemistry and metabolism.", "14", "9", "Available", "Shelf D-404"},
            {"978-0323393041", "Netter's Atlas of Human Anatomy", "Frank H. Netter", "Elsevier", "2018", "7th", "Medicine", "World-renowned anatomical illustrations.", "12", "8", "Available", "Shelf D-405"},
            {"978-0134093413", "Campbell Biology", "Lisa A. Urry", "Pearson", "2016", "11th", "Biology", "World-leading biological science textbook.", "16", "11", "Available", "Shelf D-406"},
            {"978-0815344322", "Molecular Biology of the Cell", "Bruce Alberts", "Garland Science", "2014", "6th", "Biology", "Definitive text on cell biology and genetics.", "12", "7", "Available", "Shelf D-407"},
            {"978-1464126147", "Lehninger Principles of Biochemistry", "David L. Nelson", "W. H. Freeman", "2017", "7th", "Biology", "Foundational biochemical processes and enzymes.", "14", "9", "Available", "Shelf D-408"},
            {"978-0071806411", "Basic & Clinical Pharmacology", "Bertram G. Katzung", "McGraw-Hill", "2017", "14th", "Medicine", "Comprehensive drug mechanisms and therapeutic uses.", "11", "7", "Available", "Shelf D-409"},
            {"978-0323547635", "Medical Microbiology", "Patrick R. Murray", "Elsevier", "2020", "9th", "Medicine", "Clinical microbiology, immunology, and infectious diseases.", "10", "6", "Available", "Shelf D-410"},

            // Literature, History & Philosophy
            {"978-0141439518", "Pride and Prejudice", "Jane Austen", "Penguin Classics", "2003", "Reprint", "Literature", "Masterpiece of romantic fiction and social commentary.", "20", "15", "Available", "Shelf E-501"},
            {"978-0451524935", "1984", "George Orwell", "Signet Classic", "1950", "Reprint", "Literature", "Classic dystopian novel on surveillance and totalitarianism.", "25", "18", "Available", "Shelf E-502"},
            {"978-0743273565", "The Great Gatsby", "F. Scott Fitzgerald", "Scribner", "2004", "Reprint", "Literature", "The quintessential novel of the Jazz Age and American Dream.", "18", "12", "Available", "Shelf E-503"},
            {"978-0060935467", "To Kill a Mockingbird", "Harper Lee", "Harper Perennial", "2002", "Reprint", "Literature", "Classic American novel on justice and race in the Deep South.", "20", "14", "Available", "Shelf E-504"},
            {"978-0140449136", "Crime and Punishment", "Fyodor Dostoevsky", "Penguin Classics", "2003", "Reprint", "Literature", "Psychological exploration of morality, guilt, and redemption.", "14", "9", "Available", "Shelf E-505"},
            {"978-0062316098", "Sapiens: A Brief History of Humankind", "Yuval Noah Harari", "Harper", "2015", "1st", "History", "The history of humankind from the Stone Age to modern AI.", "22", "16", "Available", "Shelf E-506"},
            {"978-0062464316", "Homo Deus: A Brief History of Tomorrow", "Yuval Noah Harari", "Harper", "2017", "1st", "History", "Exploration of the future of human intelligence and technology.", "16", "11", "Available", "Shelf E-507"},
            {"978-0199535569", "The Republic", "Plato", "Oxford World Classics", "2008", "Reprint", "Philosophy", "Foundational philosophical treatise on justice and governance.", "15", "10", "Available", "Shelf E-508"},
            {"978-0140449495", "Meditations", "Marcus Aurelius", "Penguin Classics", "2006", "Reprint", "Philosophy", "Personal reflections on Stoic philosophy and resilience.", "18", "13", "Available", "Shelf E-509"},
            {"978-0199203802", "Guns, Germs, and Steel", "Jared Diamond", "W. W. Norton", "1997", "1st", "History", "The fates of human societies through environmental factors.", "14", "9", "Available", "Shelf E-510"}
        };

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int seq = 1;
            for (String[] b : bookData) {
                ps.setString(1, b[0]);
                ps.setString(2, b[1]);
                ps.setString(3, b[2]);
                ps.setString(4, b[3]);
                ps.setInt(5, Integer.parseInt(b[4]));
                ps.setString(6, b[5]);
                ps.setString(7, b[6]);
                ps.setString(8, b[7]);
                ps.setInt(9, Integer.parseInt(b[8]));
                ps.setInt(10, Integer.parseInt(b[9]));
                ps.setString(11, b[10]);
                ps.setString(12, b[11]);
                ps.setString(13, String.format("BK-%04d", seq));
                ps.setInt(14, seq);
                ps.setString(15, LocalDate.now().minusMonths(6).format(DF));
                ps.addBatch();
                seq++;
            }

            // Generate additional 50+ books programmatically to ensure 110+ items
            String[] extraCategories = {"Computer Science", "Artificial Intelligence", "Cyber Security", "Cloud Computing", "Data Science", "Software Engineering"};
            String[] extraPublishers = {"O'Reilly Media", "Springer Nature", "Cambridge University Press", "MIT Press", "Wiley & Sons", "Prentice Hall"};
            for (int i = 61; i <= 115; i++) {
                String cat = extraCategories[(i) % extraCategories.length];
                String pub = extraPublishers[(i) % extraPublishers.length];
                int totalQ = 5 + (i % 12);
                int availQ = Math.max(1, totalQ - (i % 5));
                String status = availQ > 0 ? "Available" : "Checked Out";

                ps.setString(1, String.format("978-1-98%04d-%03d", i, (i * 7) % 999));
                ps.setString(2, "Advanced " + cat + " Principles Vol. " + (i - 60));
                ps.setString(3, "Dr. Alexander Wright & Prof. S. " + (char)('A' + (i % 26)) + ". Khan");
                ps.setString(4, pub);
                ps.setInt(5, 2020 + (i % 5));
                ps.setString(6, ((i % 4) + 1) + "th");
                ps.setString(7, cat);
                ps.setString(8, "Specialized academic and practical reference in " + cat + " systems.");
                ps.setInt(9, totalQ);
                ps.setInt(10, availQ);
                ps.setString(11, status);
                ps.setString(12, "Shelf F-" + (100 + i));
                ps.setString(13, String.format("BK-%04d", i));
                ps.setInt(14, i);
                ps.setString(15, LocalDate.now().minusMonths(i % 10).format(DF));
                ps.addBatch();
            }

            ps.executeBatch();
            LOG.info("Seeded 115+ books catalog records.");
        }
    }

    // ── 2. SEED 220+ MEMBERS ─────────────────────────────────────────────────

    private static void seedMembers(Connection c) throws SQLException {
        String sql = """
            INSERT INTO members (student_id, name, fname, cnic, date_of_birth, gender,
                                contact, email, emergency_contact, blood_group, address,
                                city, province, postal_code, country, department, program,
                                semester, session, admission_date, status, library_card_no,
                                book_limit, membership_type, membership_expiry, fine_balance,
                                notes, member_code, serial_no, registration_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String[] firstNames = {"Muhammad", "Ali", "Ahmed", "Fatima", "Ayesha", "Zainab", "Hamza", "Bilal", "Usman", "Omar", "Hassan", "Hussein", "Sara", "Maryam", "Zubair", "Ibrahim", "Tariq", "Saad", "Khadija", "Noor", "Mustafa", "Talha", "Haris", "Zaid", "Daniyal"};
        String[] lastNames = {"Khan", "Afridi", "Shah", "Malik", "Chaudhry", "Abbasi", "Qureshi", "Siddiqui", "Farooq", "Yousafzai", "Rehman", "Akhtar", "Hussain", "Gillani", "Khattak", "Durrani", "Mirza", "Bhatti", "Ansari", "Dar"};
        String[] departments = {"Computer Science", "Software Engineering", "Artificial Intelligence", "Data Science", "Cyber Security", "Electrical Engineering", "Business Administration", "Economics", "Medicine", "Physics"};
        String[] programs = {"BS", "MS", "PhD", "BBA", "MBA", "MBBS"};
        String[] cities = {"Peshawar", "Islamabad", "Lahore", "Rawalpindi", "Karachi", "Quetta", "Abbottabad", "Mardan", "Faisalabad", "Multan"};
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

        Random rng = new Random(42);

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 220; i++) {
                String fn = firstNames[rng.nextInt(firstNames.length)];
                String ln = lastNames[rng.nextInt(lastNames.length)];
                String name = fn + " " + ln;
                String father = firstNames[rng.nextInt(firstNames.length)] + " " + ln;
                String sid = String.format("ST-%04d", 1000 + i);
                String dept = departments[i % departments.length];
                String prog = programs[i % programs.length];
                String sem = "Semester " + ((i % 8) + 1);
                String city = cities[i % cities.length];
                String bg = bloodGroups[i % bloodGroups.length];
                String gender = (i % 3 == 0) ? "Female" : "Male";
                String email = (fn + "." + ln + i + "@university.edu.pk").toLowerCase();
                String phone = String.format("+92 3%02d %07d", 10 + (i % 40), 1000000 + (i * 3137) % 8999999);

                // Give ~15% of members realistic fine balances between PKR 50 and 650
                double fineBalance = 0.0;
                if (i % 7 == 0) {
                    fineBalance = 50.0 * (1 + (i % 9));
                }

                String status = (i % 25 == 0) ? "Inactive" : "Active";
                LocalDate regDate = LocalDate.now().minusMonths((i % 18) + 1);
                LocalDate expDate = regDate.plusYears(4);

                ps.setString(1, sid);
                ps.setString(2, name);
                ps.setString(3, father);
                ps.setString(4, String.format("17301-%07d-%d", 1000000 + i, (i % 9) + 1));
                ps.setString(5, "200" + (i % 5) + "-0" + ((i % 9) + 1) + "-15");
                ps.setString(6, gender);
                ps.setString(7, phone);
                ps.setString(8, email);
                ps.setString(9, "+92 300 9876543");
                ps.setString(10, bg);
                ps.setString(11, "House #" + (10 + (i % 90)) + ", Sector " + ((char)('A' + (i % 8))) + ", Phase " + ((i % 5) + 1));
                ps.setString(12, city);
                ps.setString(13, "KPK");
                ps.setString(14, "25000");
                ps.setString(15, "Pakistan");
                ps.setString(16, dept);
                ps.setString(17, prog);
                ps.setString(18, sem);
                ps.setString(19, "2022-2026");
                ps.setString(20, regDate.format(DF));
                ps.setString(21, status);
                ps.setString(22, "LIB-CARD-" + (1000 + i));
                ps.setInt(23, (prog.equals("MS") || prog.equals("PhD")) ? 8 : 5);
                ps.setString(24, prog.equals("PhD") ? "Researcher" : (prog.equals("MS") ? "Graduate" : "Student"));
                ps.setString(25, expDate.format(DF));
                ps.setDouble(26, fineBalance);
                ps.setString(27, "Regular member with good borrowing standing.");
                ps.setString(28, String.format("MB-%04d", i));
                ps.setInt(29, i);
                ps.setString(30, regDate.format(DF));
                ps.addBatch();
            }
            ps.executeBatch();
            LOG.info("Seeded 220+ university members / patrons.");
        }
    }

    // ── 3. SEED 105+ EMPLOYEES & USERS ───────────────────────────────────────

    private static void seedEmployees(Connection c) throws SQLException {
        String sql = """
            INSERT INTO employees (employee_code, name, designation, department, contact,
                                   email, cnic, address, join_date, status, salary, notes,
                                   serial_no)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String[] staffFirst = {"Kamran", "Nadeem", "Sohail", "Fawad", "Rashid", "Shahid", "Asif", "Waqas", "Noman", "Junaid", "Farhan", "Nasir", "Shazia", "Samina", "Rukhsana"};
        String[] staffLast = {"Khan", "Mehmood", "Bukhari", "Niazi", "Tanoli", "Jan", "Kundi", "Marwat", "Yousaf", "Bangash"};
        String[] designations = {"Senior Librarian", "Assistant Librarian", "Cataloger", "Archivist", "Library Assistant", "IT Systems Administrator", "Data Entry Specialist", "Accounts Officer", "Circulation Supervisor", "Security Officer"};
        String[] depts = {"Library Administration", "Cataloging & Metadata", "Circulation & Reference", "Digital Archives", "IT Systems", "Finance & Accounts"};

        Random rng = new Random(77);

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 105; i++) {
                String name = staffFirst[i % staffFirst.length] + " " + staffLast[(i * 3) % staffLast.length];
                String desig = designations[i % designations.length];
                String dept = depts[i % depts.length];
                String code = String.format("EP-%04d", 1000 + i);
                String email = (name.replace(" ", ".").toLowerCase() + "@libracore.edu.pk");
                String phone = String.format("+92 333 %07d", 2000000 + i * 179);
                double salary = 45000.0 + (rng.nextInt(18) * 5000.0);
                LocalDate joinDate = LocalDate.now().minusMonths((i % 48) + 2);

                ps.setString(1, code);
                ps.setString(2, name);
                ps.setString(3, desig);
                ps.setString(4, dept);
                ps.setString(5, phone);
                ps.setString(6, email);
                ps.setString(7, String.format("17301-%07d-%d", 2000000 + i, (i % 9) + 1));
                ps.setString(8, "Staff Colony Block " + ((char)('A' + (i % 5))) + ", Campus Avenue");
                ps.setString(9, joinDate.format(DF));
                ps.setString(10, (i % 20 == 0) ? "Inactive" : "Active");
                ps.setDouble(11, salary);
                ps.setString(12, "Full-time library management staff.");
                ps.setInt(13, i);
                ps.addBatch();
            }
            ps.executeBatch();
            LOG.info("Seeded 105+ employees and staff members.");
        }
    }

    // ── 4. SEED 250+ TRANSACTIONS (12-MONTH TIMELINE) ────────────────────────

    private static void seedTransactions(Connection c) throws SQLException {
        String sql = """
            INSERT INTO transactions (book_id, member_id, issue_date, due_date, return_date,
                                     fine_amount, fine_paid, status, return_condition, issued_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Random rng = new Random(99);
        LocalDate today = LocalDate.now();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            // A. 140 Historical Returned Transactions across past 12 months (rich chart data)
            for (int i = 1; i <= 140; i++) {
                int monthAgo = 1 + (i % 12);
                LocalDate issueDate = today.minusMonths(monthAgo).minusDays(rng.nextInt(25));
                LocalDate dueDate = issueDate.plusDays(14);
                boolean overdueReturn = (i % 4 == 0);
                LocalDate returnDate = overdueReturn ? dueDate.plusDays(3 + rng.nextInt(10)) : issueDate.plusDays(5 + rng.nextInt(8));
                double fine = overdueReturn ? (10.0 * (returnDate.toEpochDay() - dueDate.toEpochDay())) : 0.0;
                int finePaid = overdueReturn ? 1 : 0;

                int bookId = 1 + (i % 90);
                int memberId = 1 + (i % 180);

                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setString(3, issueDate.format(DF));
                ps.setString(4, dueDate.format(DF));
                ps.setString(5, returnDate.format(DF));
                ps.setDouble(6, fine);
                ps.setInt(7, finePaid);
                ps.setString(8, "Returned");
                ps.setString(9, (i % 15 == 0) ? "Damaged" : "Good");
                ps.setString(10, "admin");
                ps.addBatch();
            }

            // B. 65 Active Issued Transactions (on track, not overdue)
            for (int i = 1; i <= 65; i++) {
                LocalDate issueDate = today.minusDays(rng.nextInt(10));
                LocalDate dueDate = issueDate.plusDays(14);
                int bookId = 1 + ((i * 3) % 95);
                int memberId = 1 + ((i * 2) % 200);

                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setString(3, issueDate.format(DF));
                ps.setString(4, dueDate.format(DF));
                ps.setNull(5, Types.VARCHAR);
                ps.setDouble(6, 0.0);
                ps.setInt(7, 0);
                ps.setString(8, "Issued");
                ps.setNull(9, Types.VARCHAR);
                ps.setString(10, "admin");
                ps.addBatch();
            }

            // C. 35 Overdue Issued Transactions (due date in the past, overdue KPI + dues)
            for (int i = 1; i <= 35; i++) {
                int daysPast = 5 + (i * 2);
                LocalDate dueDate = today.minusDays(daysPast);
                LocalDate issueDate = dueDate.minusDays(14);
                double fine = daysPast * 10.0; // 10 PKR per overdue day

                int bookId = 1 + ((i * 5) % 100);
                int memberId = 1 + ((i * 7) % 210);

                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setString(3, issueDate.format(DF));
                ps.setString(4, dueDate.format(DF));
                ps.setNull(5, Types.VARCHAR);
                ps.setDouble(6, fine);
                ps.setInt(7, 0); // unpaid fine
                ps.setString(8, "Issued");
                ps.setNull(9, Types.VARCHAR);
                ps.setString(10, "admin");
                ps.addBatch();
            }

            // D. 15 Lost Transactions
            for (int i = 1; i <= 15; i++) {
                LocalDate issueDate = today.minusMonths(2).minusDays(i);
                LocalDate dueDate = issueDate.plusDays(14);
                int bookId = 1 + (i % 50);
                int memberId = 1 + (i % 150);

                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setString(3, issueDate.format(DF));
                ps.setString(4, dueDate.format(DF));
                ps.setString(5, today.minusDays(i).format(DF));
                ps.setDouble(6, 1500.0); // replacement fine
                ps.setInt(7, 1);
                ps.setString(8, "Lost");
                ps.setString(9, "Lost");
                ps.setString(10, "admin");
                ps.addBatch();
            }

            ps.executeBatch();
            LOG.info("Seeded 255+ transaction records across 12-month timeline.");
        }
    }

    // ── 5. SEED 40+ RESERVATIONS ─────────────────────────────────────────────

    private static void seedReservations(Connection c) throws SQLException {
        String sql = """
            INSERT INTO reservations (book_id, member_id, reservation_date, status, queue_position, expires_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        LocalDate today = LocalDate.now();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 40; i++) {
                int bookId = 1 + (i % 60);
                int memberId = 1 + ((i * 4) % 200);
                String status = (i % 4 == 0) ? "Fulfilled" : "Pending";
                LocalDate resDate = today.minusDays(i % 20);
                LocalDate expDate = resDate.plusDays(7);

                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setString(3, resDate.format(DF));
                ps.setString(4, status);
                ps.setInt(5, (i % 3) + 1);
                ps.setString(6, expDate.format(DF));
                ps.addBatch();
            }
            ps.executeBatch();
            LOG.info("Seeded 40+ book reservation records.");
        }
    }

    // ── 6. SEED 150+ AUDIT ACTIVITY LOGS ─────────────────────────────────────

    private static void seedActivityLogs(Connection c) throws SQLException {
        String sql = "INSERT INTO activity_log (user_id, action, details, timestamp) VALUES (?, ?, ?, ?)";
        String[] actions = {"BOOK_ISSUED", "BOOK_RETURNED", "FINE_COLLECTED", "MEMBER_REGISTERED", "BOOK_ADDED", "REPORT_GENERATED", "USER_LOGIN", "SETTINGS_UPDATED"};
        Random rng = new Random(101);

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 1; i <= 150; i++) {
                String act = actions[rng.nextInt(actions.length)];
                String dt = LocalDate.now().minusDays(i % 45).toString() + " 1" + (i % 9) + ":24:00";
                String detail = switch (act) {
                    case "BOOK_ISSUED" -> "Issued Book #" + (1 + rng.nextInt(90)) + " to Member #" + (1 + rng.nextInt(200));
                    case "BOOK_RETURNED" -> "Received return of Book #" + (1 + rng.nextInt(90)) + " in Good condition";
                    case "FINE_COLLECTED" -> "Collected overdue fine of PKR " + (50 + rng.nextInt(400)) + ".00";
                    case "MEMBER_REGISTERED" -> "Enrolled new student member account ST-" + (1000 + rng.nextInt(220));
                    case "BOOK_ADDED" -> "Catalogued new title: Computer Science Edition";
                    case "REPORT_GENERATED" -> "Exported Monthly Circulation & Analytics PDF Report";
                    case "USER_LOGIN" -> "Administrator authenticated successfully";
                    default -> "System parameter updated";
                };

                ps.setInt(1, 1);
                ps.setString(2, act);
                ps.setString(3, detail);
                ps.setString(4, dt);
                ps.addBatch();
            }
            ps.executeBatch();
            LOG.info("Seeded 150+ activity and audit logs.");
        }
    }

    // ── 7. UPDATE ID COUNTERS ────────────────────────────────────────────────

    private static void updateIdCounters(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("UPDATE id_counters SET last_id = 120 WHERE entity='BK'");
            s.execute("UPDATE id_counters SET last_id = 250 WHERE entity='ST'");
            s.execute("UPDATE id_counters SET last_id = 250 WHERE entity='MB'");
            s.execute("UPDATE id_counters SET last_id = 110 WHERE entity='EP'");
        }
    }
}
