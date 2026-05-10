package com.library.database;

import com.library.model.Book;
import com.library.model.Member;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.TransactionService;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Runs exactly once on first launch.
 * Seeds the database with enough data to make the dashboard
 * and all modules immediately usable without manual setup.
 *
 * Idempotent: checks a flag in the settings table before seeding.
 */
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class.getName());
    private static final String SEED_FLAG = "seeded_v2";

    private final BookService        books   = new BookService();
    private final MemberService      members = new MemberService();
    private final TransactionService txs     = new TransactionService();

    // ── Entry point ───────────────────────────────────────────────────────────

    public void seedIfNeeded() {
        if (isAlreadySeeded()) {
            LOG.info("Database already seeded — skipping.");
            return;
        }
        LOG.info("First launch detected — seeding sample data...");
        try {
            seedBooks();
            seedMembers();
            seedTransactions();
            markSeeded();
            LOG.info("Data seeding complete.");
        } catch (Exception e) {
            LOG.warning("Seeding error (non-fatal): " + e.getMessage());
        }
    }

    // ── Books ─────────────────────────────────────────────────────────────────

    private void seedBooks() {
        Object[][] data = {
            // isbn, title, author, publisher, year, edition, category, qty, shelf
            {"978-0-262-03384-8","Introduction to Algorithms","Thomas H. Cormen","MIT Press",2022,"4th","Computer Science",3,"CS-A1"},
            {"978-0-13-468599-1","Clean Code","Robert C. Martin","Prentice Hall",2008,"1st","Computer Science",2,"CS-A2"},
            {"978-0-201-63361-0","Design Patterns","Gang of Four","Addison-Wesley",1994,"1st","Computer Science",2,"CS-A3"},
            {"978-0-13-235088-4","The Pragmatic Programmer","Andrew Hunt","Addison-Wesley",2019,"2nd","Computer Science",2,"CS-A4"},
            {"978-0-13-110362-7","The C Programming Language","Brian Kernighan","Prentice Hall",1988,"2nd","Computer Science",3,"CS-B1"},
            {"978-0-07-352332-6","Operating System Concepts","Abraham Silberschatz","Wiley",2018,"10th","Computer Science",2,"CS-B2"},
            {"978-0-13-294645-6","Computer Networks","Andrew Tanenbaum","Pearson",2021,"6th","Computer Science",2,"CS-B3"},
            {"978-0-13-468440-6","Database System Concepts","Abraham Silberschatz","McGraw-Hill",2019,"7th","Computer Science",2,"CS-B4"},
            {"978-0-13-604259-4","Artificial Intelligence","Stuart Russell","Pearson",2020,"4th","Computer Science",1,"CS-C1"},
            {"978-0-387-31073-2","Pattern Recognition","Christopher Bishop","Springer",2006,"1st","Computer Science",1,"CS-C2"},
            {"978-0-07-338332-5","Calculus","James Stewart","Cengage",2015,"8th","Mathematics",3,"MA-A1"},
            {"978-0-13-468951-7","Linear Algebra","Gilbert Strang","Wellesley",2016,"5th","Mathematics",2,"MA-A2"},
            {"978-0-07-338045-4","Discrete Mathematics","Kenneth Rosen","McGraw-Hill",2018,"8th","Mathematics",2,"MA-A3"},
            {"978-0-13-110362-8","Probability & Statistics","Sheldon Ross","Pearson",2014,"9th","Mathematics",2,"MA-B1"},
            {"978-0-201-55802-9","The Feynman Lectures on Physics","Richard Feynman","Caltech",2011,"1st","Physics",2,"PH-A1"},
            {"978-0-07-249908-6","University Physics","Hugh Young","Pearson",2019,"15th","Physics",3,"PH-A2"},
            {"978-0-19-852091-0","Organic Chemistry","Paula Bruice","Pearson",2016,"8th","Chemistry",2,"CH-A1"},
            {"978-0-07-352218-3","Molecular Biology of the Cell","Bruce Alberts","Norton",2014,"6th","Biology",2,"BI-A1"},
            {"978-0-14-028329-7","1984","George Orwell","Penguin",1949,"1st","Literature",4,"LT-A1"},
            {"978-0-06-112008-4","To Kill a Mockingbird","Harper Lee","HarperCollins",1960,"1st","Literature",3,"LT-A2"},
            {"978-0-7432-7356-5","The Alchemist","Paulo Coelho","HarperOne",1988,"1st","Literature",3,"LT-A3"},
            {"978-0-374-52935-0","Sapiens","Yuval Noah Harari","Harper",2011,"1st","History",3,"HI-A1"},
            {"978-0-553-38016-3","A Brief History of Time","Stephen Hawking","Bantam",1988,"1st","Physics",2,"PH-B1"},
            {"978-0-374-27563-1","Thinking, Fast and Slow","Daniel Kahneman","Farrar",2011,"1st","Psychology",2,"PS-A1"},
            {"978-0-06-093546-9","The Art of War","Sun Tzu","Harper",500,"1st","Philosophy",3,"PH-A1"},
            {"978-0-14-044140-6","Meditations","Marcus Aurelius","Penguin",180,"1st","Philosophy",2,"PH-A2"},
            {"978-0-19-283407-0","The Republic","Plato","Oxford",380,"1st","Philosophy",2,"PH-A3"},
            {"978-0-14-044253-3","Nicomachean Ethics","Aristotle","Penguin",350,"1st","Philosophy",2,"PH-A4"},
            {"978-0-06-196436-2","The Lean Startup","Eric Ries","Crown",2011,"1st","Business",2,"BU-A1"},
            {"978-0-06-662099-5","Good to Great","Jim Collins","HarperBusiness",2001,"1st","Business",2,"BU-A2"},
        };

        for (Object[] row : data) {
            Book b = new Book();
            b.setIsbn((String) row[0]);
            b.setBookName((String) row[1]);
            b.setAuthor((String) row[2]);
            b.setPublisher((String) row[3]);
            b.setPublicationYear((int) row[4]);
            b.setEdition((String) row[5]);
            b.setCategory((String) row[6]);
            int qty = (int) row[7];
            b.setQuantity(qty);
            b.setAvailableQty(qty);
            b.setShelfLocation((String) row[8]);
            b.setStatus(Book.STATUS_AVAILABLE);
            books.addBook(b);
        }
        LOG.info("Seeded " + data.length + " books.");
    }

    // ── Members ───────────────────────────────────────────────────────────────

    private void seedMembers() {
        Object[][] data = {
            // name, fname, gender, contact, email, dept, program, semester
            {"Ahmed Ali Khan",      "Ali Khan",       "Male",   "03001234567","ahmed.ali@uni.edu.pk",      "Computer Science","BS","5"},
            {"Fatima Malik",        "Usman Malik",    "Female", "03012345678","fatima.malik@uni.edu.pk",   "Computer Science","BS","3"},
            {"Muhammad Hassan",     "Hassan Ahmed",   "Male",   "03023456789","m.hassan@uni.edu.pk",       "Electrical Eng",  "BS","7"},
            {"Sara Qureshi",        "Tariq Qureshi",  "Female", "03034567890","sara.q@uni.edu.pk",         "Mathematics",     "BS","4"},
            {"Omar Siddiqui",       "Bilal Siddiqui", "Male",   "03045678901","omar.s@uni.edu.pk",         "Physics",         "MS","2"},
            {"Zainab Chaudhry",     "Imran Chaudhry", "Female", "03056789012","zainab.c@uni.edu.pk",       "Chemistry",       "BS","6"},
            {"Yusuf Ansari",        "Khalid Ansari",  "Male",   "03067890123","yusuf.a@uni.edu.pk",        "Business Admin",  "MBA","3"},
            {"Maryam Sheikh",       "Asif Sheikh",    "Female", "03078901234","maryam.s@uni.edu.pk",       "Literature",      "BS","2"},
            {"Ibrahim Butt",        "Nadeem Butt",    "Male",   "03089012345","ibrahim.b@uni.edu.pk",      "Computer Science","BS","8"},
            {"Noor Fatima",         "Zahid Fatima",   "Female", "03090123456","noor.f@uni.edu.pk",         "Biology",         "BS","4"},
            {"Ali Raza",            "Raza Ahmed",     "Male",   "03101234567","ali.raza@uni.edu.pk",       "Mechanical Eng",  "BS","6"},
            {"Hira Baig",           "Baig Sahib",     "Female", "03112345678","hira.b@uni.edu.pk",         "Computer Science","MS","1"},
            {"Usman Tariq",         "Tariq Usman",    "Male",   "03123456789","usman.t@uni.edu.pk",        "Economics",       "BS","5"},
            {"Amna Riaz",           "Riaz Ahmed",     "Female", "03134567890","amna.r@uni.edu.pk",         "Psychology",      "BS","3"},
            {"Bilal Hussain",       "Hussain Ali",    "Male",   "03145678901","bilal.h@uni.edu.pk",        "Law",             "LLB","4"},
        };

        for (Object[] row : data) {
            Member m = new Member();
            m.setName((String) row[0]);
            m.setFname((String) row[1]);
            m.setGender((String) row[2]);
            m.setContact((String) row[3]);
            m.setEmail((String) row[4]);
            m.setDepartment((String) row[5]);
            m.setProgram((String) row[6]);
            m.setSemester((String) row[7]);
            m.setCity("Lahore");
            m.setCountry("Pakistan");
            m.setStatus(Member.STATUS_ACTIVE);
            m.setBookLimit(5);
            members.addMember(m);
        }
        LOG.info("Seeded " + data.length + " members.");
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    private void seedTransactions() {
        // Issue books to first 10 members (book IDs 1-10, member IDs 1-10)
        int issued = 0;
        for (int i = 1; i <= 10; i++) {
            String result = txs.issueBook(i, i, "system");
            if (result.isEmpty()) issued++;
        }
        LOG.info("Seeded " + issued + " transactions.");
    }

    // ── Flag helpers ──────────────────────────────────────────────────────────

    private boolean isAlreadySeeded() {
        String sql = "SELECT value FROM settings WHERE key=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, SEED_FLAG);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "true".equals(rs.getString(1));
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void markSeeded() {
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, 'true')";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, SEED_FLAG);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Could not mark seed flag: " + e.getMessage());
        }
    }
}
