/**
 * LibraCore Pro v3.0.0 — Root Launcher
 *
 * Run this file directly from the project root:
 *   java Main.java
 *
 * It will build (if needed) and launch the application using Maven.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String dir = System.getProperty("user.dir");

        // Check if JAR already built
        java.io.File jar = new java.io.File(dir, "target/LibraCore-Pro-3.0.0.jar");

        if (!jar.exists()) {
            System.out.println("==> Building project (first time)...");
            run(dir, "D:\\maven\\apache-maven-3.9.6\\bin\\mvn.cmd",
                    "clean", "package", "-DskipTests");
        }

        System.out.println("==> Launching LibraCore Pro...");
        System.out.println("    Default login:  admin / admin");
        System.out.println();

        run(dir, "D:\\maven\\apache-maven-3.9.6\\bin\\mvn.cmd", "javafx:run");
    }

    static void run(String dir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new java.io.File(dir));
        pb.inheritIO();
        int code = pb.start().waitFor();
        if (code != 0) {
            System.err.println("[ERROR] Command failed with code: " + code);
            System.exit(code);
        }
    }
}
