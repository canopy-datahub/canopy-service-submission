package org.canopyplatform.canopy.submissionservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.canopyplatform.canopy.submissionservice.models.DataFile;
import org.canopyplatform.canopy.submissionservice.models.SQSMessage;
import org.canopyplatform.canopy.submissionservice.models.ValidationResult;
import org.canopyplatform.canopy.submissionservice.emails.EmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
public class SqsMessageService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper mapper;
    private final String bucket;
    private final String sqsQueuePii;
    private final String emailQueue;

    @Autowired
    public SqsMessageService(SqsAsyncClient sqsAsyncClient,
                             @Value("${s3-bucket.in-review}") String bucket,
                             @Value("${canopy.pii-queue}") String sqsQueuePii,
                             @Value("${canopy.emailQueue}") String emailQueue){
        this.sqsAsyncClient = sqsAsyncClient;
        this.bucket = bucket;
        this.sqsQueuePii = sqsQueuePii;
        this.emailQueue = emailQueue;
        mapper = new ObjectMapper();
    }

    public boolean sendPIIMessage(String uuid, Integer submissionId, DataFile dataFile ) {
        //need to update to check other conditions before sending file message, Ex: will check if file pii status is started ongoing or completed
        ValidationResult validationResult = new ValidationResult(dataFile);
        SQSMessage piisqsMessage = new SQSMessage(bucket,uuid,submissionId,validationResult);
        CompletableFuture<SendMessageResponse> response = sqsAsyncClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(sqsQueuePii)
                .messageBody(piiRequestToJsonString(piisqsMessage))
                .build());
        log.info("PII Validation request sent to SQS: " + sqsQueuePii);
        try {
            response.join();
            return true;
        } catch (CompletionException e) {
            log.error("Error sending message to sqs: ", e.getMessage());
            return false;
        }
    }

    /**
     * Converts an piiSqsMessage request object to json
     *
     * @param piisqsMessage object to transform into json
     * @return json form of the provided piiSqsMessage as a String
     * @throws RuntimeException if the object cannot be processed
     */
    private String piiRequestToJsonString(SQSMessage piisqsMessage) {
        try {
            return mapper.writeValueAsString(piisqsMessage);
        } catch (JsonProcessingException e) {
            String errorMessage = "Object to json to string failure";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

    /** Sends EmailRequest object to SQS so that a corresponding email will be sent
     * @param emailRequest object containing details needed for an email
     * @return boolean mapping to the success of the sqs upload
     * @throws CompletionException if the message failed to be received by SQS
     */
    public boolean sendEmail(EmailRequest emailRequest){
        SendMessageRequest request = SendMessageRequest
                .builder()
                .queueUrl(emailQueue)
                .messageBody(emailRequestToJsonString(emailRequest))
                .build();
        CompletableFuture<SendMessageResponse> response = sqsAsyncClient.sendMessage(request);
        try {
            response.join();
            log.info("Email request sent to SQS{" + emailQueue + "}: " + emailRequest.toString());
            return true;
        } catch (CompletionException e){
            log.error("Error sending message to sqs", e);
            return false;
        }
    }

    /**
     * Converts an EmailRequest object to json
     * @param emailRequest object to transform into json
     * @return json form of the provided EmailRequest as a String
     * @throws RuntimeException if the object cannot be processed
     */
    private String emailRequestToJsonString(EmailRequest emailRequest){
        try {
            return mapper.writeValueAsString(emailRequest);
        } catch (JsonProcessingException e){
            String errorMessage = "Object to json to string failure";
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

}
