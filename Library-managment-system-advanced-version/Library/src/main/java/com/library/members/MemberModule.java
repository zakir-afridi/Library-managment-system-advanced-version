package com.library.members;

import com.library.model.Member;
import com.library.service.MemberService;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * MEMBERS BRANCH — public API.
 */
public class MemberModule {

    private static final MemberService service = new MemberService();
    private static com.library.controller.MemberController controller;

    // ── UI Navigation ─────────────────────────────────────────────────────────

    public static void showMemberList(Pane container) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MemberModule.class.getResource("/com/library/ui/AddMemberForm.fxml"));
            Pane view = loader.load();
            controller = loader.getController();
            container.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("MemberModule.showMemberList: " + e.getMessage());
        }
    }

    public static void showAddMember() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MemberModule.class.getResource("/com/library/ui/AddMemberForm.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Add Member");
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("MemberModule.showAddMember: " + e.getMessage());
        }
    }

    public static void refreshMemberData() {
        if (controller != null) controller.loadPage();
    }

    // ── Data API ──────────────────────────────────────────────────────────────

    public static Member getById(int memberId)             { return service.getMemberById(memberId); }
    public static List<Member> search(String query)        { return service.searchMembers(query); }
    public static int getTotalCount()                      { return service.getTotalMembers(); }
    public static int getActiveBookCount(int memberId)     { return service.getActiveBookCount(memberId); }
    public static boolean addFine(int memberId, double amt){ return service.addFine(memberId, amt); }
}
