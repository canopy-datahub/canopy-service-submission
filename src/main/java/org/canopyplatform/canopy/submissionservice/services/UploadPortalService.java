package org.canopyplatform.canopy.submissionservice.services;

import org.canopyplatform.canopy.submissionservice.auth.UserNotFoundException;
import org.canopyplatform.canopy.submissionservice.emails.EmailRequestService;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.BadDataException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.FileDeletionException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.StudyNotFoundException;
import org.canopyplatform.canopy.submissionservice.mappers.UserFileUploadMapper;
import org.canopyplatform.canopy.submissionservice.mappers.ViewStudyMapper;
import org.canopyplatform.canopy.submissionservice.models.S3File;
import org.canopyplatform.canopy.submissionservice.models.UserFileUpload;
import org.canopyplatform.canopy.submissionservice.models.Users;
import org.canopyplatform.canopy.submissionservice.models.ViewStudy;
import org.canopyplatform.canopy.submissionservice.models.dtos.StudiesDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.UploadPortalCuratorDashboardDTO;
import org.canopyplatform.canopy.submissionservice.repositories.S3FileRepository;
import org.canopyplatform.canopy.submissionservice.repositories.UserFileUploadRepository;
import org.canopyplatform.canopy.submissionservice.repositories.UsersRepository;
import org.canopyplatform.canopy.submissionservice.repositories.ViewStudyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UploadPortalService {

    private final AwsStorageService storageService;
    private final EmailRequestService emailRequestService;
    private final ViewStudyRepository viewStudyRepository;
    private final UsersRepository usersRepository;
    private final UserFileUploadRepository uploadRepository;
    private final ViewStudyMapper viewStudyMapper;
    private final UserFileUploadRepository userFileUploadRepository;
    private final UserFileUploadMapper userFileUploadMapper;
    private final S3FileRepository s3FileRepository;

    public void uploadFile(MultipartFile file, Integer studyId, Integer userId) {
        if(file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".zip")){
            throw new BadDataException("Provided upload portal file must be a zip file");
        }
        ViewStudy study = viewStudyRepository.findByStudyId(studyId)
                .orElseThrow(() -> new StudyNotFoundException("Could not upload file: Study ID not found"));
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Could not upload file: User ID not found"));
        S3File uploadedFile = storageService.uploadPortalFile(file, userId);
        UserFileUpload userFileUpload = new UserFileUpload(uploadedFile, study.getStudyId());
        uploadRepository.save(userFileUpload);
        emailRequestService.sendUploadPortalEmail(uploadedFile.getFileName(), study.getStudyIdTitle(), user);
    }

    public List<StudiesDTO> getApprovedStudies() {
        List<ViewStudy> studies = viewStudyRepository.findAllBySubmissionStatusEqualsIgnoreCaseOrderByStudyId("Approved");
        return viewStudyMapper.toDTOs(studies);
    }

    @Transactional
    public List<UploadPortalCuratorDashboardDTO> getCuratorDashboardView() {
        List<UserFileUpload> uploads = userFileUploadRepository.findAllOrdered();
        return userFileUploadMapper.mapToDtoList(uploads);
    }

    @Transactional
    public void deleteUpload(Integer uploadId, Integer userId){
        UserFileUpload upload = userFileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new FileDeletionException("Upload not found"));
        S3File s3File = upload.getS3File();
        if(s3File == null) {
            throw new FileDeletionException("No S3 file found for upload");
        }
        Boolean isDeletedFromS3 = storageService.deleteFileFromS3(s3File);
        if(isDeletedFromS3){
            upload.removeS3FileLink(userId);
            uploadRepository.save(upload);
            s3FileRepository.delete(s3File);
        } else {
            log.error("File deleted from S3 but not the database. Please check upload ID {} and S3 file ID {}", uploadId, s3File.getId());
            throw new FileDeletionException("Upload could not be deleted. Please try again.");
        }
    }

}
