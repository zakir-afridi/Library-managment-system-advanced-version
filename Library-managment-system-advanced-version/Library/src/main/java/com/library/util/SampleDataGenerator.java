package com.library.util;

import com.library.model.Book;
import com.library.model.Member;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.TransactionService;

import java.util.Random;

/**
 * Generates realistic sample data for development and demo purposes.
 * Creates books with ISBN/category, members with student IDs, and transactions.
 */
public class SampleDataGenerator {

    private static final String[] CATEGORIES = {
        "Computer Science", "Mathematics", "Physics", "Chemistry", "Biology",
        "History", "Literature", "Philosophy", "Economics", "Engineering",
        "Medicine", "Law", "Psychology", "Sociology", "Arts"
    };

    private static final String[] TITLES = {
        "Introduction to Algorithms", "Clean Code", "Design Patterns",
        "The Pragmatic Programmer", "Structure and Interpretation",
        "Operating System Concepts", "Computer Networks", "Database Systems",
        "Artificial Intelligence", "Machine Learning", "Data Structures",
        "Calculus Made Easy", "Linear Algebra", "Discrete Mathematics",
        "Quantum Physics", "Organic Chemistry", "Molecular Biology",
        "World History", "Pride and Prejudice", "1984", "The Great Gatsby",
        "To Kill a Mockingbird", "Brave New World", "The Alchemist",
        "Thinking Fast and Slow", "Sapiens", "Brief History of Time",
        "The Art of War", "Meditations", "The Republic", "Nicomachean Ethics"
    };

    private static final String[] AUTHORS = {
        "Thomas Cormen", "Robert Martin", "Gang of Four", "Andrew Hunt",
        "Harold Abelson", "Abraham Silberschatz", "Andrew Tanenbaum",
        "Ramez Elmasri", "Stuart Russell", "Christopher Bishop",
        "Donald Knuth", "Silvanus Thompson", "Gilbert Strang",
        "Kenneth Rosen", "Richard Feynman", "Paula Bruice",
        "Bruce Alberts", "John Merriman", "Jane Austen", "George Orwell",
        "F. Scott Fitzgerald", "Harper Lee", "Aldous Huxley", "Paulo Coelho",
        "Daniel Kahneman", "Yuval Noah Harari", "Stephen Hawking",
        "Sun Tzu", "Marcus Aurelius", "Plato", "Aristotle"
    };

    private static final String[] PUBLISHERS = {
        "MIT Press", "O'Reilly Media", "Pearson", "McGraw-Hill",
        "Springer", "Wiley", "Cambridge University Press",
        "Oxford University Press", "Penguin Books", "HarperCollins"
    };

    private static final String[] DEPARTMENTS = {
        "Computer Science", "Electrical Engineering", "Mechanical Engineering",
        "Civil Engineering", "Mathematics", "Physics", "Chemistry",
        "Biology", "Business Administration", "Economics",
        "English Literature", "History", "Psychology", "Law", "Medicine"
    };

    private static final String[] FIRST_NAMES = {
        "Ahmed", "Ali", "Muhammad", "Hassan", "Omar", "Ibrahim", "Yusuf",
        "Fatima", "Aisha", "Zainab", "Maryam", "Sara", "Noor", "Hana",
        "James", "John", "Michael", "David", "Sarah", "Emily", "Jessica",
        "Usman", "Bilal", "Tariq", "Sana", "Rabia", "Amna", "Hira"
    };

    private static final String[] LAST_NAMES = {
        "Khan", "Ahmed", "Ali", "Hassan", "Malik", "Sheikh", "Qureshi",
        "Chaudhry", "Siddiqui", "Ansari", "Smith", "Johnson", "Williams",
        "Brown", "Jones", "Garcia", "Miller", "Davis", "Wilson", "Taylor"
    };

    private final BookService        bookService   = new BookService();
    private final MemberService      memberService = new MemberService();
    private final TransactionService txService     = new TransactionService();
    private final Random             random        = new Random(42);

    public void generateAll() {
        System.out.println("=== LibraCore Pro — Sample Data Generator ===");
        generateBooks(100);
        generateMembers(50);
        generateTransactions(30);
        System.out.println("=== Sample data generation complete! ===");
    }

    public void generateBooks(int count) {
        System.out.println("Generating " + count + " books...");
        int created = 0;
        for (int i = 0; i < count; i++) {
            Book b = new Book();
            b.setIsbn(generateISBN());
            b.setBookName(TITLES[random.nextInt(TITLES.length)] + " " + (i + 1));
            b.setAuthor(AUTHORS[random.nextInt(AUTHORS.length)]);
            b.setPublisher(PUBLISHERS[random.nextInt(PUBLISHERS.length)]);
            b.setPublicationYear(2000 + random.nextInt(24));
            b.setEdition((random.nextInt(5) + 1) + "th Edition");
            b.setCategory(CATEGORIES[random.nextInt(CATEGORIES.length)]);
            int qty = random.nextInt(5) + 1;
            b.setQuantity(qty);
            b.setAvailableQty(qty);
            b.setShelfLocation(String.valueOf((char)('A' + random.nextInt(10)))
                    + "-" + (random.nextInt(20) + 1));
            if (bookService.addBook(b)) created++;
        }
        System.out.println("  Created " + created + " books.");
    }

    public void generateMembers(int count) {
        System.out.println("Generating " + count + " members...");
        int created = 0;
        for (int i = 0; i < count; i++) {
            Member m = new Member();
            String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String last  = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            m.setName(first + " " + last);
            m.setFname(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + last);
            m.setGender(random.nextBoolean() ? "Male" : "Female");
            m.setContact("03" + String.format("%09d", random.nextInt(1_000_000_000)));
            m.setEmail(first.toLowerCase() + "." + last.toLowerCase()
                    + (i + 1) + "@university.edu.pk");
            m.setDepartment(DEPARTMENTS[random.nextInt(DEPARTMENTS.length)]);
            m.setProgram(random.nextBoolean() ? "BS" : "MS");
            m.setSemester(String.valueOf(random.nextInt(8) + 1));
            m.setSession("2022-2026");
            m.setCity("Lahore");
            m.setCountry("Pakistan");
            m.setBookLimit(5);
            m.setStatus(Member.STATUS_ACTIVE);
            if (memberService.addMember(m)) created++;
        }
        System.out.println("  Created " + created + " members.");
    }

    public void generateTransactions(int count) {
        System.out.println("Generating " + count + " transactions...");
        int totalBooks   = bookService.getTotalBooks();
        int totalMembers = memberService.getTotalMembers();
        if (totalBooks == 0 || totalMembers == 0) {
            System.out.println("  No books or members found — skipping transactions.");
            return;
        }
        int created = 0;
        for (int i = 0; i < count; i++) {
            int bookId   = random.nextInt(totalBooks) + 1;
            int memberId = random.nextInt(totalMembers) + 1;
            String result = txService.issueBook(bookId, memberId, "system");
            if (result.isEmpty()) created++;
        }
        System.out.println("  Created " + created + " transactions.");
    }

    private String generateISBN() {
        return "978-" + (random.nextInt(9) + 1)
                + "-" + String.format("%04d", random.nextInt(10000))
                + "-" + String.format("%04d", random.nextInt(10000))
                + "-" + random.nextInt(10);
    }
}
