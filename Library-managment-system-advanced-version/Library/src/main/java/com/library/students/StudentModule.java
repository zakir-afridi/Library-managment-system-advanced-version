package com.library.students;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Map;

/**
 * STUDENTS BRANCH — public API.
 */
public class StudentModule {

    private static final StudentService service = new StudentService();

    // ── UI Navigation ─────────────────────────────────────────────────────────

    public static void showStudentList(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    StudentModule.class.getResource("/com/library/ui/AddStudentForm.fxml"));
            Pane view = loader.load();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("StudentModule.showStudentList: " + e.getMessage());
        }
    }

    public static void showAddStudent() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    StudentModule.class.getResource("/com/library/ui/AddStudentForm.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Add Student");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("StudentModule.showAddStudent: " + e.getMessage());
        }
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    public static int getTotalCount()                      { return service.getTotalCount(); }
    public static List<Map<String, String>> search(String q) { return service.search(q); }
}
