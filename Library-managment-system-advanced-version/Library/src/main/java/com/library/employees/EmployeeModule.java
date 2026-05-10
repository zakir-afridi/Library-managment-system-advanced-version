package com.library.employees;

import com.library.model.Employee;
import com.library.service.EmployeeService;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * EMPLOYEES BRANCH — public API.
 */
public class EmployeeModule {

    private static final EmployeeService service = new EmployeeService();

    // ── UI Navigation ─────────────────────────────────────────────────────────

    public static void showEmployeeList(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmployeeModule.class.getResource("/com/library/ui/EmployeeForm.fxml"));
            Pane view = loader.load();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("EmployeeModule.showEmployeeList: " + e.getMessage());
        }
    }

    public static void showAddEmployee() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmployeeModule.class.getResource("/com/library/ui/EmployeeForm.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Add Employee");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("EmployeeModule.showAddEmployee: " + e.getMessage());
        }
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    public static List<Employee> getPage(int page, int size) { return service.getAllEmployees(page, size); }
    public static int getTotalCount()                        { return service.getTotalEmployees(); }
    public static List<Employee> search(String query)        { return service.searchEmployees(query); }
    public static Employee getById(int empId)                { return service.getEmployeeById(empId); }
}
