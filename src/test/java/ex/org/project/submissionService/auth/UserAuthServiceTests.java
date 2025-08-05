// package ex.org.project.submissionService.auth;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.anyList;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// class UserAuthServiceTests {

//     @Mock
//     private AuthRasTrackingRepository authRasTrackingRepository;

//     @Mock
//     private AuthUserRepository authUserRepository;

//     @Mock
//     private AuthUserRasRepository authUserRasRepository;

//     @Mock
//     private AuthUtilRepository authUtilRepository;

//     private final AuthUserMapper authUserMapper = new AuthUserMapperImpl();

//     private UserAuthService userAuthService;

//     @Mock
//     private AuthRasService authRasService;

//     @BeforeEach
//     public void setup() {
//         userAuthService = new UserAuthService(authUserMapper, authRasTrackingRepository, authUserRepository,
//                 authUserRasRepository, authUtilRepository, authRasService);
//     }

//     @Test
//     void testCheckAuth_roles_happyPath(){
//         AuthRasTracking rasTracking = new AuthRasTracking();
//         rasTracking.setEmail("test@bah.com");
//         AuthUser authUser = new AuthUser();
//         AuthRole authRole = new AuthRole(1, "Application Administrator");
//         AuthLookupStatus status = new AuthLookupStatus(1, "active");
//         authUser.setRoles(List.of(authRole));
//         authUser.setStatus(status);
//         authUser.setId(8);

//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.of(rasTracking));
//         when(authUserRepository.findByEmail("test@bah.com"))
//                 .thenReturn(Optional.of(authUser));

//         Integer response = userAuthService.checkAuth("123", List.of(AccessRole.ADMIN));

//         assertEquals(8, response);

//     }

//     @Test
//     void testCheckAuth_roles_happyPathNoRoleRequired(){
//         AuthRasTracking rasTracking = new AuthRasTracking();
//         rasTracking.setEmail("test@bah.com");
//         AuthUser authUser = new AuthUser();
//         AuthLookupStatus status = new AuthLookupStatus(1, "active");
//         authUser.setStatus(status);
//         authUser.setRoles(new ArrayList<>());
//         authUser.setId(8);

//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.of(rasTracking));
//         when(authUserRepository.findByEmail("test@bah.com"))
//                 .thenReturn(Optional.of(authUser));

//         Integer response = userAuthService.checkAuth("123", List.of());

//         assertEquals(8, response);
//     }

//     @Test
//     void testCheckAuth_roles_invalidSessionId(){
//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.empty());

//         assertThrows(UserAuthenticationException.class,
//                 () -> userAuthService.checkAuth("123", List.of(AccessRole.ADMIN)));
//     }

//     @Test
//     void testCheckAuth_roles_emailNotFound(){
//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.of(new AuthRasTracking()));

//         assertThrows(UserNotFoundException.class,
//                 () -> userAuthService.checkAuth("123", List.of(AccessRole.ADMIN)));
//     }

//     @Test
//     void testCheckAuth_roles_userNotFound(){
//         AuthRasTracking rasTracking = new AuthRasTracking();
//         rasTracking.setEmail("test@bah.com");

//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.of(rasTracking));
//         when(authUserRepository.findByEmail("test@bah.com"))
//                 .thenReturn(Optional.empty());

//         assertThrows(UserNotFoundException.class,
//                 () -> userAuthService.checkAuth("123", List.of(AccessRole.ADMIN)));
//     }

//     @Test
//     void testCheckAuth_roles_invalidRoles(){
//         AuthRasTracking rasTracking = new AuthRasTracking();
//         rasTracking.setEmail("test@bah.com");
//         AuthUser authUser = new AuthUser();
//         AuthRole authRole = new AuthRole(1, "Data Submitter");
//         AuthLookupStatus status = new AuthLookupStatus(1, "active");
//         authUser.setRoles(List.of(authRole));
//         authUser.setStatus(status);
//         authUser.setId(8);

//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.of(rasTracking));
//         when(authUserRepository.findByEmail("test@bah.com"))
//                 .thenReturn(Optional.of(authUser));

//         assertThrows(UserAuthorizationException.class,
//                 () -> userAuthService.checkAuth("123", List.of(AccessRole.ADMIN)));
//     }

//     @Test
//     void testCheckAuth_happyPath(){
//         AuthRasTracking rasTracking = new AuthRasTracking();
//         rasTracking.setEmail("test@bah.com");
//         AuthUser authUser = new AuthUser();
//         AuthRole authRole = new AuthRole(1, "Application Administrator");
//         AuthLookupStatus status = new AuthLookupStatus(1, "active");
//         authUser.setRoles(List.of(authRole));
//         authUser.setStatus(status);
//         authUser.setId(8);

//         when(authRasTrackingRepository.findBySessionId(anyString()))
//                 .thenReturn(Optional.of(rasTracking));
//         when(authUserRepository.findByEmail("test@bah.com"))
//                 .thenReturn(Optional.of(authUser));

//         Integer response = userAuthService.checkAuth("123");

//         assertEquals(8, response);

//     }

//     private List<AuthUserRas> getAuthUserRasList(){
//         AuthUserRas userRas1 = new AuthUserRas(10, "phs001111", 1);
//         AuthUserRas userRas2 = new AuthUserRas(10, "phs001112", 1);
//         AuthUserRas userRas3 = new AuthUserRas(10, "phs001113", 1);
//         return List.of(userRas1, userRas2, userRas3);
//     }

//     @Test
//     void testCheckFileAuthorization_happyPath(){
//         List<AuthUserRas> userRasMock = getAuthUserRasList();
//         List<String> phsNumbersMock = List.of("phs001111", "phs001112", "phs001113");
//         Integer userId = 1;
//         List<Integer> dataFileIds = List.of(1, 2, 3, 4, 5);

//         when(authUtilRepository.findAllApprovedFileIdsIn(dataFileIds))
//                 .thenReturn(dataFileIds);
//         when(authUserRasRepository.findAllByUserId(userId))
//                 .thenReturn(userRasMock);
//         when(authUtilRepository.findPhsNumbersOfFilesIn(anyList()))
//                 .thenReturn(phsNumbersMock);

//         boolean response = userAuthService.checkFileAuthorization(userId, dataFileIds);

//         assertTrue(response);
//     }

//     @Test
//     void testCheckFileAuthorization_partialStudyAuthorization(){
//         List<AuthUserRas> userRasMock = List.of(new AuthUserRas(10, "phs001111", 1));
//         List<String> phsNumbersMock = List.of("phs001111", "phs001112", "phs001113");
//         Integer userId = 1;
//         List<Integer> dataFileIds = List.of(1, 2, 3, 4, 5);

//         when(authUtilRepository.findAllApprovedFileIdsIn(dataFileIds))
//                 .thenReturn(dataFileIds);
//         when(authUserRasRepository.findAllByUserId(userId))
//                 .thenReturn(userRasMock);
//         when(authUtilRepository.findPhsNumbersOfFilesIn(anyList()))
//                 .thenReturn(phsNumbersMock);

//         assertThrows(UserAuthorizationException.class,
//                 () -> userAuthService.checkFileAuthorization(userId, dataFileIds)
//         );
//     }

//     @Test
//     void testCheckFileAuthorization_noStudyAuthorization(){
//         List<AuthUserRas> userRasMock = new ArrayList<>();
//         List<String> phsNumbersMock = List.of("phs001111", "phs001112", "phs001113");
//         Integer userId = 1;
//         List<Integer> dataFileIds = List.of(1, 2, 3, 4, 5);

//         when(authUtilRepository.findAllApprovedFileIdsIn(dataFileIds))
//                 .thenReturn(dataFileIds);
//         when(authUserRasRepository.findAllByUserId(userId))
//                 .thenReturn(userRasMock);
//         when(authUtilRepository.findPhsNumbersOfFilesIn(anyList()))
//                 .thenReturn(phsNumbersMock);

//         assertThrows(UserAuthorizationException.class,
//                 () -> userAuthService.checkFileAuthorization(userId, dataFileIds)
//         );
//     }

//     @Test
//     void testCheckFileAuthorization_allUnapprovedFiles(){
//         Integer userId = 1;
//         List<Integer> dataFileIds = List.of(1, 2, 3, 4, 5);

//         when(authUtilRepository.findAllApprovedFileIdsIn(dataFileIds))
//                 .thenReturn(new ArrayList<>());

//         assertThrows(UserAuthorizationException.class,
//                 () -> userAuthService.checkFileAuthorization(userId, dataFileIds)
//         );
//     }

//     @Test
//     void testCheckFileAuthorization_someUnapprovedFiles(){
//         Integer userId = 1;
//         List<Integer> dataFileIds = List.of(1, 2, 3, 4, 5);

//         when(authUtilRepository.findAllApprovedFileIdsIn(dataFileIds))
//                 .thenReturn(List.of(1, 2, 3, 4));

//         assertThrows(UserAuthorizationException.class,
//                 () -> userAuthService.checkFileAuthorization(userId, dataFileIds)
//         );
//     }
// }
