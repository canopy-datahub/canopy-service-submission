package org.canopyplatform.canopy.submissionservice.auth;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
interface AuthUserMapper {

    @Mapping(target = "roles", source = "authUser.roles", qualifiedByName = "extractRoles")
    @Mapping(target = "status", source = "authUser.status", qualifiedByName = "extractStatus")
    @Mapping(target = "sessionId", source = "session")
    AuthUserDTO toAuthUserDto(AuthUser authUser, String session);

    @Named("extractRoles")
    static List<String> extractRoles(List<AuthRole> authRoles){
        return authRoles.stream().map(AuthRole::getName).toList();
    }

    @Named("extractStatus")
    static String extractStatus(AuthLookupStatus status){
        return status.getName();
    }

}
