package ex.org.project.submissionService.emails;

import java.util.List;
import java.util.Map;

public record EmailRequest(
        String type,
        List<String> to,
        List<String> cc,
        String from,
        String subject,
        Map<String, String> props
) {
    public EmailRequest(EmailRequestType emailRequestType, List<String> to, List<String> cc, String from, Map<String, String> props){
        this(
                emailRequestType.getType(),
                to,
                cc,
                from,
                emailRequestType.getSubject(),
                props
            );
    }

}
