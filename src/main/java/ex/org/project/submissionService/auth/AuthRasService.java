package ex.org.project.submissionService.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRasService {
	@Autowired
	RestTemplate restTemplate;

	@Value("${ras.url}")
	private String rasUrl;

	@Value("${ras.client.id}")
	private String rasClientId;

	@Value("${ras.client.secret}")
	private String rasClientSecret;
	
	private final AuthRasTrackingRepository authRasTrackingRepository;
	private static final String GRANT_TYPE = "grant_type";
	private static final String SCOPE = "scope";
	private static final String CLIENT_ID = "client_id";
	private static final String CLIENT_SECRET = "client_secret";

	/**
	 * To get the RAS User info by sending the accessToken to the API
	 * @param accessToken
	 * @return RasUserDTO
	 */
	public AuthRasDTO getRasUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		HttpEntity<?> request = new HttpEntity<>(payload, headers);
		String url = rasUrl + "/openid/connect/v1.1/userinfo";
		ResponseEntity<AuthRasDTO> responseEntity = restTemplate.exchange(url, HttpMethod.POST, request, AuthRasDTO.class);
		return responseEntity.getBody();
	}

	public void processRefreshToken(String sessionId) {
		Optional<AuthRasTracking> rasUserOpt = authRasTrackingRepository.findBySessionId(sessionId);
		if(rasUserOpt.isEmpty()){
			String errorMessage = "Unable to find valid session: " + sessionId;
			log.info(errorMessage);
			throw new UserAuthenticationException(errorMessage);
		}
		AuthRasTracking authRasTracking = rasUserOpt.get();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add(GRANT_TYPE, "refresh_token");
		payload.add(SCOPE, "openid profile email department");
		payload.add("refresh_token", authRasTracking.getRefreshToken());
		payload.add(CLIENT_ID, rasClientId);
		payload.add(CLIENT_SECRET, rasClientSecret);
		HttpEntity<?> request = new HttpEntity<>(payload, headers);

		String url = rasUrl + "/auth/oauth/v2/token";
		ResponseEntity<AuthRasToken> responseEntity = restTemplate.exchange(url, HttpMethod.POST, request, AuthRasToken.class);
		AuthRasToken rasToken = responseEntity.getBody();
		authRasTrackingRepository.delete(authRasTracking);
		authRasTracking.setId(null);
		authRasTracking.setRefreshToken(rasToken.getRefresh_token());
		authRasTracking.setAccessToken(rasToken.getAccess_token());
		authRasTrackingRepository.save(authRasTracking);
	}

}


