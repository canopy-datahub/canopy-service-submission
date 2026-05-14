package org.canopyplatform.canopy.submissionservice.models;

/**
 * Per-study access control. Stored as a string in study.access_level; the DB
 * check constraint mirrors this enum.
 *
 * <ul>
 *   <li>{@code PUBLIC} — visible to everyone, including unauthenticated visitors.</li>
 *   <li>{@code LIMITED} — visible to any authenticated user, regardless of role.</li>
 *   <li>{@code PRIVATE} — visible only to the Creator and to users with the
 *       Curator or Admin role.</li>
 * </ul>
 */
public enum AccessLevel {
    PUBLIC,
    LIMITED,
    PRIVATE
}
