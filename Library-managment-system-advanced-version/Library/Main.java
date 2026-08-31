/**
 * LibraCore Pro v3.0.0 — Root Launcher
 *
 * Run this file directly from the project root:
 *   java Main.java
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String dir = System.getProperty("user.dir");

        // Check if JAR exists, if not build it
        java.io.File jar = new java.io.File(dir, "target/LibraCore-Pro-3.0.0.jar");
        java.io.File libDir = new java.io.File(dir, "target/lib");

        if (!jar.exists() || !libDir.exists()) {
            System.out.println("==> Building LibraCore Pro (first-time setup)...");
            run(dir, "D:\\maven\\apache-maven-3.9.6\\bin\\mvn.cmd", "clean", "package", "-DskipTests", "-q");
        }

        System.out.println("==================================================");
        System.out.println(" 🚀 Launching LibraCore Pro v3.0.0 (Enterprise)");
        System.out.println(" 🔑 Default login:  admin / admin");
        System.out.println("==================================================");

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";

        String fxModulePath = "target/lib/javafx-controls-21.0.2-win.jar;"
                            + "target/lib/javafx-fxml-21.0.2-win.jar;"
                            + "target/lib/javafx-graphics-21.0.2-win.jar;"
                            + "target/lib/javafx-base-21.0.2-win.jar;"
                            + "target/lib/javafx-swing-21.0.2-win.jar";

        String classPath = "target/LibraCore-Pro-3.0.0.jar;target/lib/*";

        run(dir,
            javaBin,
            "--module-path", fxModulePath,
            "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing",
            "--enable-native-access=javafx.graphics,ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
            "-Dsun.misc.unsafe.warn=false",
            "-Dfile.encoding=UTF-8",
            "-cp", classPath,
            "com.library.Main"
        );
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
