package ex.org.project.submissionService.services;

import ex.org.project.submissionService.auth.UserNotFoundException;
import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.exceptions.custom.FileDeletionException;
import ex.org.project.submissionService.exceptions.custom.StudyNotFoundException;
import ex.org.project.submissionService.mappers.UserFileUploadMapper;
import ex.org.project.submissionService.mappers.ViewStudyMapper;
import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.UserFileUpload;
import ex.org.project.submissionService.models.Users;
import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.StudiesDTO;
import ex.org.project.submissionService.models.dtos.UploadPortalCuratorDashboardDTO;
import ex.org.project.submissionService.repositories.S3FileRepository;
import ex.org.project.submissionService.repositories.UserFileUploadRepository;
import ex.org.project.submissionService.repositories.UsersRepository;
import ex.org.project.submissionService.repositories.ViewStudyRepository;
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
//        emailRequestService.sendUploadPortalEmail(uploadedFile.getFileName(), study.getPhsTitle(), user);
    }

    public List<StudiesDTO> getApprovedStudies() {
        List<ViewStudy> studies = viewStudyRepository.findAllBySubmissionStatusEqualsIgnoreCaseOrderByPhs("Approved");
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
