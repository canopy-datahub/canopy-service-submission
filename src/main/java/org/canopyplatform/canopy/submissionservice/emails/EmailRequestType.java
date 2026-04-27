package org.canopyplatform.canopy.submissionservice.emails;

/**
 * Enum containing the different types of emails that can be sent by Submission Service.
 * type: the type of email request
 * subject: the subject line of the email to be sent
 */
public interface EmailRequestType {
    String getType();
    String getSubject();
}
