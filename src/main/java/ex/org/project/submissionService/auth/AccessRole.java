package ex.org.project.submissionService.auth;

import java.util.HashMap;
import java.util.Map;

public enum AccessRole {
    DATA_SUBMITTER("Data Submitter"),
    OFFICER("Officer"),
    DATA_CURATOR("Data Curator"),
    SUPPORT_TEAM("Support Team"),
    ADMIN("Application Administrator"),
    UPLOADER("Uploader");

    public final String label;

    private static final Map<String, AccessRole> BY_LABEL = new HashMap<>();
    static {
        for(AccessRole role : values()){
            BY_LABEL.put(role.label, role);
        }
    }

    AccessRole(String label){
        this.label = label;
    }

    public static AccessRole valueOfLabel(String label){
        return BY_LABEL.get(label);
    }

}
