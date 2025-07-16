package ex.org.project.submissionService.auth;

import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-15T14:01:27-0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.14 (Oracle Corporation)"
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
