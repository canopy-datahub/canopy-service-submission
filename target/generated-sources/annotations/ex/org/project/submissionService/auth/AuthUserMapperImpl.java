package ex.org.project.submissionService.auth;

import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-09T16:48:25-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
class AuthUserMapperImpl implements AuthUserMapper {

    @Override
    public AuthUserDTO toAuthUserDto(AuthUser authUser, String session) {
        if ( authUser == null && session == null ) {
            return null;
        }

        List<String> roles = null;
        String status = null;
        Integer id = null;
        String email = null;
        Boolean internalUser = null;
        if ( authUser != null ) {
            roles = AuthUserMapper.extractRoles( authUser.getRoles() );
            status = AuthUserMapper.extractStatus( authUser.getStatus() );
            id = authUser.getId();
            email = authUser.getEmail();
            internalUser = authUser.getInternalUser();
        }
        String sessionId = null;
        sessionId = session;

        String dbGapPermissions = null;

        AuthUserDTO authUserDTO = new AuthUserDTO( id, email, roles, sessionId, dbGapPermissions, status, internalUser );

        return authUserDTO;
    }
}
