package org.canopyplatform.canopy.submissionservice.auth;

import lombok.Data;

@Data
public class AuthRasToken {

    private String access_token;
    private String refresh_token;
    private String id_token;
}
