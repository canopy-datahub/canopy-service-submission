# Auth

- Explanation of the `auth` package, along with details and examples of implementation

# Package Contents
- ## Models
  - ### Entities
    - #### AuthUser
      - Representation of the 'users' table
    - #### AuthRole
      - Representation of the 'lkup_role' table
    - #### AuthRasTracking
      - Representation of the 'ras_tracking' table
    - #### AuthLookupStatus
      - Representation of the 'lkup_status' table
  - ### DTOs
    - #### AuthUserDTO
      - A mapped version of a User object that contains a formatted version of the data needed for auth
    - #### ExceptionResponseDTO
      - A normalized response object that all auth exceptions should return
  - ### Enums
    - #### AccessRole
      - Enumerated version of user access roles
      - Default account has no roles
- ## Repositories
  - #### AuthRasTrackingRepository
  - #### AuthUserRepository
- ## Exceptions
  - #### UserAuthenticationException
    - Authentication error that should return a 401
    - Error in the process or action of verifying the identity of a user or process
  - #### UserAuthorizationException
    - Authorization error that should return a 403
    - Error in the access rights of a user or process to a resource or set of resources
  - #### UserNotFoundException
    - Actual error in our data meaning a valid session ID was provided but a corresponding user can't be found
- ## Business Logic
  - #### UserAuthService
    - API for all auth logic
  - #### AuthExceptionHandler
    - Controller advice for auth related errors

# Usage
- ## API
  - ### Publicly exposed objects
    - #### `UserAuthService::checkAuth`
      - This is the endpoint to validate a session ID as well as check if the user has the required roles
      - Place this method call as the first line in any controller and pass in the session ID provided in the cookie, 
        as well as the list of roles that are authorized to call the endpoint
        - If the endpoint is for a default user, just pass in an empty list
      - Params:
        - sessionId
          - `String`
          - User provided session ID that is being validated via the ras_tracking table
        - authorizedRoles
          - `List<AccessRole>`
          - List of access roles that have authorization to use an endpoint
      - Response:
        - `Integer`
        - ID of the user who is attached to the validated session
    - #### `UserAuthService::checkFileAuthorization`
      - This is the endpoint to validate a session ID as well as check if the user has the required roles
      - Place this method call as the first line in any controller and pass in the session ID provided in the cookie,
        as well as the list of roles that are authorized to call the endpoint
        - If the endpoint is for a default user, just pass in an empty list
      - Params:
        - userId
          - `Integer`
          - User ID that is being validated via the user_ras table
        - fileIds
          - `List<Integer>`
          - List of file IDs to which access is being validated against the permissions provided in the RAS passport
      - Response:
        - `Void`
          - An error response will be returned if the user does not have the required file authorizations
    - #### `AccessRole`
      - Possible user roles in the DataHub application
  - ### Notes
    - All external auth related calls should be made to the UserAuthService class
      - Any additional entities should have the default package-private access modifier
      - Methods should only be made public if they have an explicit reasons to be

- ## Exceptions
  - ### `UserAuthenticationException`
    - To be used when the provided credentials are invalid or nonexistant
      - Examples:
        - The provided session ID does not exist
        - The provided session ID is expired
    - Should return a `401`
  - ### `UserAuthorizationException`
    - To be used when the provided credentials are valid but the user does not have the required role(s)
      - Examples:
        - The user is a researcher but tries to access the metrics tables that are only for curators and admins
        - The user tries to download files that they have not requested access to via dbGaP
    - Should return a `403`

# Examples
- ## Controller
  - This ↓ will authenticate a user, as well as check that they have the Support Team role
  ``` 
  @GetMapping("/testAuth")
  public ResponseEntity<String> testSessionCookie(@CookieValue("chocolateChip") String sessionId){
      Integer userId = authService.checkAuthorization(sessionId, List.of(AccessRole.SUPPORT_TEAM));
          return ResponseEntity.ok("Authorization of session { " + sessionId + " } for user ID { " + userId + " }: Success");
  } 
  ```

  - This ↓ will authenticate a user
  ``` 
  @GetMapping("/testAuth")
  public ResponseEntity<String> testSessionCookie(@CookieValue("chocolateChip") String sessionId){
      Integer userId = authService.checkAuthorization(sessionId);
          return ResponseEntity.ok("Authorization of session { " + sessionId + " } for user ID { " + userId + " }: Success");
  } 
  ```
  
  - This ↓ will authenticate a user, and check that they have at least one of the Support Team or Application Admin role
    - They do not need to have both
  ``` 
  @GetMapping("/testAuth")
  public ResponseEntity<String> testSessionCookie(@CookieValue("chocolateChip") String sessionId){
      Integer userId = authService.checkAuthorization(sessionId, List.of(AccessRole.SUPPORT_TEAM, AccessRole.ADMIN));
          return ResponseEntity.ok("Authorization of session { " + sessionId + " } for user ID { " + userId + " }: Success");
  } 
  ```

  - The cookie is read for the field "chocolateChip" which contains the sessionId
    - Immediately a call is made to `checkAuth` with the session ID and the support team access role
      - If the session ID is invalid an error will be thrown
        - A response will be returned of type `ResponseEntity<ExceptionResponseDTO>` with the status code `401`
      - If the session ID is valid but the user does not have the support request role, an error will be thrown
        - A response will be returned of type `ResponseEntity<ExceptionResponseDTO>` with the status code `403`
      - If the session ID is valid and the user has the role, the user ID will be returned and the API will continue as normal
  - Table showing expected behavior: 
  
      |  Session ID  | Has Role |             Error             |    Response     |
      |:------------:|:--------:|:-----------------------------:|:---------------:|
      |    valid     |   Yes    |              n/a              | returns user ID |
      |    valid     |    No    | `UserAuthorizationException`  |      `403`      |
      |   invalid    |   n/a    | `UserAuthenticationException` |      `401`      |
      |   expired    |   n/a    | `UserAuthenticationException` |      `401`      |
    

  - This ↓ will authenticate a user, and then verify that they have authorized access to the file IDs they are requesting
  ```
  @GetMapping("/testStudyAccess")
  public ResponseEntity<String> testStudyAccess(@CookieValue("chocolateChip") String sessionId, @RequestParam("dataFileIds") List<Integer> dataFileIds){
      Integer userId = authService.checkAuth(sessionId);
      authService.checkFileAuthorization(userId, dataFileIds);
      return ResponseEntity.ok("Successful access to data files: " + dataFileIds);
  }
  ```
  - The authentication and role authorization check happens the same as above
  - A call is then made to `checkFileAuthorization`
    - If the user has access to the studies necessary, and the files are approved, then the method will continue as normal after the verification
    - If the user has access to the studies necessary, but the files are unapproved, and error will be thrown
      - A response will be returned of type `ResponseEntity<ExceptionResponseDTO>` with the status code `403`
    - If the user does not have authorized access, an error will be thrown and caught
      - A response will be returned of type `ResponseEntity<ExceptionResponseDTO>` with the status code `403`