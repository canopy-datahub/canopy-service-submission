package org.canopyplatform.canopy.submissionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.canopyplatform.canopy.submissionservice.auth.AuthRole;
import org.canopyplatform.canopy.submissionservice.auth.AuthUser;
import org.canopyplatform.canopy.submissionservice.auth.UserAuthorizationException;
import org.canopyplatform.canopy.submissionservice.auth.core.KeycloakAuthenticationService;
import org.canopyplatform.canopy.submissionservice.models.AccessLevel;
import org.canopyplatform.canopy.submissionservice.models.DataFile;
import org.canopyplatform.canopy.submissionservice.models.DataSubmission;
import org.canopyplatform.canopy.submissionservice.models.Study;
import org.canopyplatform.canopy.submissionservice.repositories.DataFileRepository;
import org.canopyplatform.canopy.submissionservice.repositories.DataSubmissionRepository;
import org.canopyplatform.canopy.submissionservice.repositories.StudyRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Per-study authorization decisions. Layers on top of the capability check
 * (which decides "can the user do this kind of thing in general"); this
 * service decides "for this specific study."
 *
 * <p>Rules:
 * <ul>
 *   <li><b>canRead</b> — depends on access_level. PUBLIC: everyone (auth optional).
 *       LIMITED: any authenticated user. PRIVATE: Creator + Curator + Admin.</li>
 *   <li><b>canSubmitTo</b> — strict Creator-only. Curator and Admin do NOT
 *       bypass; they don't submit data.</li>
 *   <li><b>canEditStudy / canEditAccess / canDeleteStudy</b> — Creator OR
 *       Curator OR Admin.</li>
 * </ul>
 *
 * <p>For Phase A this class is wired into the bean graph but not yet called
 * from controllers; that happens in Phase B. The require* methods throw
 * UserAuthorizationException on failure so they slot into the existing
 * controller exception pipeline.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StudyAccessService {

    private static final String ROLE_CURATOR = "Data Curator";
    private static final String ROLE_ADMIN   = "Application Administrator";
    private static final Set<String> OVERRIDE_ROLES = Set.of(ROLE_CURATOR, ROLE_ADMIN);

    private final StudyRepository studyRepository;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final DataFileRepository dataFileRepository;
    private final KeycloakAuthenticationService authenticationService;

    /**
     * Read decision. Accepts a null JWT (anonymous caller) — anonymous can
     * still read PUBLIC studies.
     */
    public boolean canRead(Jwt jwt, Integer studyId) {
        Study study = loadOrThrow(studyId);
        AccessLevel level = study.getAccessLevel();
        if (level == AccessLevel.PUBLIC) {
            return true;
        }
        if (jwt == null) {
            return false;
        }
        AuthUser user = authenticationService.getAuthenticatedUser(jwt);
        if (level == AccessLevel.LIMITED) {
            return true;
        }
        // PRIVATE
        return isCreator(user, study) || hasOverrideRole(user);
    }

    /**
     * Strict Creator-only. Curator/Admin do not bypass — they do not submit
     * data on someone else's behalf.
     */
    public boolean canSubmitTo(Jwt jwt, Integer studyId) {
        AuthUser user = authenticationService.getAuthenticatedUser(jwt);
        Study study = loadOrThrow(studyId);
        return isCreator(user, study);
    }

    /**
     * Creator OR Curator OR Admin.
     */
    public boolean canEditStudy(Jwt jwt, Integer studyId) {
        return creatorOrOverride(jwt, studyId);
    }

    /**
     * Creator OR Curator OR Admin. Same shape as canEditStudy today; kept as
     * a distinct method so the controller call site documents intent.
     */
    public boolean canEditAccess(Jwt jwt, Integer studyId) {
        return creatorOrOverride(jwt, studyId);
    }

    /**
     * Creator OR Curator OR Admin.
     */
    public boolean canDeleteStudy(Jwt jwt, Integer studyId) {
        return creatorOrOverride(jwt, studyId);
    }

    // ---- require* variants throw on failure ------------------------------

    public void requireRead(Jwt jwt, Integer studyId) {
        if (!canRead(jwt, studyId)) {
            throw new UserAuthorizationException(
                    "User does not have read access to study " + studyId);
        }
    }

    public void requireSubmitTo(Jwt jwt, Integer studyId) {
        if (!canSubmitTo(jwt, studyId)) {
            throw new UserAuthorizationException(
                    "Only the study's Creator may submit data to it");
        }
    }

    public void requireEditStudy(Jwt jwt, Integer studyId) {
        if (!canEditStudy(jwt, studyId)) {
            throw new UserAuthorizationException(
                    "User may not edit study " + studyId);
        }
    }

    public void requireEditAccess(Jwt jwt, Integer studyId) {
        if (!canEditAccess(jwt, studyId)) {
            throw new UserAuthorizationException(
                    "User may not change access level for study " + studyId);
        }
    }

    public void requireDeleteStudy(Jwt jwt, Integer studyId) {
        if (!canDeleteStudy(jwt, studyId)) {
            throw new UserAuthorizationException(
                    "User may not delete study " + studyId);
        }
    }

    // ---- submission-id / file-id convenience variants -------------------
    //
    // Many endpoints take a submissionId or fileId rather than a studyId.
    // These helpers do the lookup and delegate to the studyId-based checks
    // so controllers stay one-liners.

    public void requireSubmitToBySubmission(Jwt jwt, Integer submissionId) {
        requireSubmitTo(jwt, lookupStudyIdForSubmission(submissionId));
    }

    public void requireStudyManagementBySubmission(Jwt jwt, Integer submissionId) {
        requireEditStudy(jwt, lookupStudyIdForSubmission(submissionId));
    }

    public void requireSubmitToByFile(Jwt jwt, Integer fileId) {
        requireSubmitTo(jwt, lookupStudyIdForFile(fileId));
    }

    public Integer lookupStudyIdForSubmission(Integer submissionId) {
        DataSubmission sub = dataSubmissionRepository.findDataSubmissionById(submissionId);
        if (sub == null) {
            throw new UserAuthorizationException(
                    "Submission " + submissionId + " not found");
        }
        return sub.getStudyId();
    }

    public Integer lookupStudyIdForFile(Integer fileId) {
        DataFile file = dataFileRepository.findById(fileId)
                .orElseThrow(() -> new UserAuthorizationException(
                        "Data file " + fileId + " not found"));
        return lookupStudyIdForSubmission(file.getSubmissionId());
    }

    // ---- internals -------------------------------------------------------

    private boolean creatorOrOverride(Jwt jwt, Integer studyId) {
        AuthUser user = authenticationService.getAuthenticatedUser(jwt);
        Study study = loadOrThrow(studyId);
        return isCreator(user, study) || hasOverrideRole(user);
    }

    private boolean isCreator(AuthUser user, Study study) {
        return study.getCreatedBy() != null
                && user.getId() != null
                && study.getCreatedBy().equals(user.getId());
    }

    private boolean hasOverrideRole(AuthUser user) {
        List<AuthRole> roles = user.getRoles();
        if (roles == null) return false;
        return roles.stream()
                .map(AuthRole::getName)
                .anyMatch(OVERRIDE_ROLES::contains);
    }

    private Study loadOrThrow(Integer studyId) {
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new UserAuthorizationException(
                        "Study " + studyId + " not found"));
    }
}
