package org.canopyplatform.canopy.submissionservice.models.dtos;

import org.canopyplatform.canopy.submissionservice.models.AccessLevel;

/**
 * Request body for {@code PUT /study/{studyId}/access}. The supplied
 * accessLevel replaces the study's current access_level. Caller must hold the
 * {@code study.access.update} capability AND be the study's Creator (or a
 * Curator/Admin override) — enforced by StudyAccessService.requireEditAccess.
 */
public record AccessLevelUpdateDTO(
        AccessLevel accessLevel
) {
}
