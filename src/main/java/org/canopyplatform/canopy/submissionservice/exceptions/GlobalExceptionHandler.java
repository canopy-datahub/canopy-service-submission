package org.canopyplatform.canopy.submissionservice.exceptions;

import org.canopyplatform.canopy.submissionservice.auth.UserAuthenticationException;
import org.canopyplatform.canopy.submissionservice.auth.UserAuthorizationException;
import org.canopyplatform.canopy.submissionservice.auth.UserNotFoundException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.*;
import lombok.extern.slf4j.Slf4j;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handleMultipartException(MultipartException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Empty Multipart File",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handleEmptyParameterException(EmptyParameterException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Empty Parameter",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handleBadDataException(BadDataException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Bad Data",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handleStudyNotFoundException(StudyNotFoundException e) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Study Not Found",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handleDataFileNotFoundException(DataFileNotFoundException e){
        log.warn(e.getMessage());
        HttpStatus status = HttpStatus.NOT_FOUND;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Data File Not Found",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handleFileDeletionException(FileDeletionException e){
        log.error(e.getMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "File Deletion Exception",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler
    public final ResponseEntity<ExceptionResponseDTO> handlePdfParsingException(PdfParsingException e){
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "PDF Parsing Error",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(UserAuthorizationException.class)
    ResponseEntity<ExceptionResponseDTO> authorizationException(UserAuthorizationException e){
        HttpStatus status = HttpStatus.FORBIDDEN;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Unauthorized",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(UserAuthenticationException.class)
    ResponseEntity<ExceptionResponseDTO> authenticationException(UserAuthenticationException e){
        //why in the world does the "unauthorized" exception actually mean unauthenticated
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Unauthenticated",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ExceptionResponseDTO> userNotFoundException(UserNotFoundException e){
        HttpStatus status = HttpStatus.NOT_FOUND;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "User Not Found",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ResponseEntity<ExceptionResponseDTO> CategoryNotFoundException(CategoryNotFoundException e){
        HttpStatus status = HttpStatus.NOT_FOUND;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Category Not Found",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(SubmitterCenterException.class)
    ResponseEntity<ExceptionResponseDTO> handleSubmitterDccException(SubmitterCenterException e) {
        log.warn(e.getMessage(), e);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Submitter DCC Exception",
                status.value(),
                String.format("A user with the role of Data Submitter must be aligned to a valid DCC. %s", e.getMessage())
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(StudyRegRequestException.class)
    ResponseEntity<ExceptionResponseDTO> handleStudyRegRequestException(StudyRegRequestException e) {
        log.warn(e.getMessage(), e);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Bad Request: Study Registration",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(SubmissionIdInvalidException.class)
    ResponseEntity<ExceptionResponseDTO> handleSubmissionIdException(SubmissionIdInvalidException e) {
        log.warn(e.getMessage(), e);
        HttpStatus status = HttpStatus.NOT_FOUND;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Submission ID Not Found",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }
    @ExceptionHandler(StatusNotFoundException.class)
    ResponseEntity<ExceptionResponseDTO> statusNotFoundException(StatusNotFoundException e){
        log.warn("Invalid Status Value", e);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Invalid Status Value",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponseDTO> resourceNotFoundException(ResourceNotFoundException e) {
        log.warn("Resource Not Found", e);
        HttpStatus status = HttpStatus.NOT_FOUND;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Resource Not Found",
                status.value(),
                e.getMessage()
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(MalformedRequestException.class)
    public final ResponseEntity<ExceptionResponseDTO> malformedRequestException(MalformedRequestException e){
        log.warn(e.getMessage(), e);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Invalid parameter",
                status.value(),
                "Malformed parameter found in the request."
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(ValidationErrorException.class)
    ResponseEntity<ExceptionResponseDTO> validationErrorException(ValidationErrorException e){
        log.error(e.getMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Internal Server Error",
                status.value(),
                "An unknown error has occurred during validation."
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ExceptionResponseDTO> runtimeException(RuntimeException e){
        log.error(e.getMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Internal Server Error",
                status.value(),
                "An unknown error has occurred. Please contact support if the issue persists."
        );
        return new ResponseEntity<>(responseDTO, status);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ExceptionResponseDTO> exception(Exception e){
        log.error(e.getMessage(), e);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ExceptionResponseDTO responseDTO = new ExceptionResponseDTO(
                "Internal Server Error",
                status.value(),
                "An unknown error has occurred. Please contact support if the issue persists."
        );
        return new ResponseEntity<>(responseDTO, status);
    }

}
