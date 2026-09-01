/**
 * LibraCore Pro v3.0.0 — Root Launcher
 *
 * Run this file directly from the project root:
 *   java Main.java
 * or
 *   javac Main.java && java Main
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String dir = System.getProperty("user.dir");

        java.io.File targetDir = new java.io.File(dir, "target");
        java.io.File jar = new java.io.File(targetDir, "LibraCore-Pro-3.0.0.jar");
        java.io.File libDir = new java.io.File(targetDir, "lib");
        java.io.File classesDir = new java.io.File(targetDir, "classes");

        if (!jar.exists() || !libDir.exists() || !classesDir.exists()) {
            run(dir, "D:\\maven\\apache-maven-3.9.6\\bin\\mvn.cmd", "clean", "package", "-DskipTests", "-q");
        }

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";

        // Build absolute classpath with application jar, classes dir, and all dependencies
        StringBuilder cp = new StringBuilder();
        if (jar.exists()) {
            cp.append(jar.getAbsolutePath()).append(java.io.File.pathSeparator);
        }
        if (classesDir.exists()) {
            cp.append(classesDir.getAbsolutePath()).append(java.io.File.pathSeparator);
        }

        StringBuilder modulePath = new StringBuilder();
        java.io.File[] libs = libDir.listFiles();
        if (libs != null) {
            for (java.io.File f : libs) {
                if (f.getName().endsWith(".jar")) {
                    cp.append(f.getAbsolutePath()).append(java.io.File.pathSeparator);
                    if (f.getName().startsWith("javafx-") && f.getName().contains("-win.jar")) {
                        modulePath.append(f.getAbsolutePath()).append(java.io.File.pathSeparator);
                    }
                }
            }
        }

        run(dir,
            javaBin,
            "--module-path", modulePath.toString(),
            "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing",
            "--enable-native-access=javafx.graphics,ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
            "-Dsun.misc.unsafe.warn=false",
            "-Dfile.encoding=UTF-8",
            "-cp", cp.toString(),
            "com.library.LibraCoreApp"
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
