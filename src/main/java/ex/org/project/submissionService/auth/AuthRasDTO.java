package ex.org.project.submissionService.auth;

import lombok.Data;

import java.util.List;

@Data
public class AuthRasDTO {

    private String sub;

    private String jti;

    private String scope;

    private String txn;
    private String iss;
    private long iat;
    private long exp;
    private String name;
    private String first_name;
    private String last_name;
    private String preferred_username;
    private String userid;
    private String email;
    private String department;
    private String passport_jwt_v11;
    private String[] ga4gh_passport_v1;
    private List<AuthRasDbGapPermissionDTO> ras_dbgap_permissions;
    private AuthRasGa4ghDTO ga4gh_visa_v1;

}
