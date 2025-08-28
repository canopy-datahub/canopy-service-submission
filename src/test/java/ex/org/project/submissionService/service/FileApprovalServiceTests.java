package ex.org.project.submissionService.service;

import ex.org.project.submissionService.exceptions.custom.BadDataException;
import ex.org.project.submissionService.exceptions.custom.DataFileNotFoundException;
import ex.org.project.submissionService.mappers.DataFileMapper;
import ex.org.project.submissionService.mappers.DataFileMapperImpl;
import ex.org.project.submissionService.mappers.SubmissionByCuratorMapper;
import ex.org.project.submissionService.mappers.SubmissionByCuratorMapperImpl;
import ex.org.project.submissionService.models.*;
import ex.org.project.submissionService.models.dtos.BundlesDTO;
import ex.org.project.submissionService.models.dtos.DetailsDTO;
import ex.org.project.submissionService.models.dtos.SubmissionApprovalDTO;
import ex.org.project.submissionService.repositories.*;
import ex.org.project.submissionService.services.DataFileService;
import ex.org.project.submissionService.emails.EmailRequestService;
import ex.org.project.submissionService.services.FileApprovalService;
import ex.org.project.submissionService.services.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileApprovalServiceTests {

    @Mock
    private LkupStatusRepository lkupStatusRepository;
    @Mock
    private DataSubmissionRepository dataSubmissionRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private DataFileRepository dataFileRepository;
    @Mock
    private DataFileService dataFileService;
    @Mock
    private StorageService storageService;
    @Mock
    private EmailRequestService emailRequestService;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private ViewStudyRepository viewStudyRepository;
    @Mock
    private StudyPropertyValueRepository studyPropertyValueRepository;
    @Spy
    private SubmissionByCuratorMapper submissionByCuratorMapper = new SubmissionByCuratorMapperImpl();
    @Spy
    private DataFileMapper dataFileMapper = new DataFileMapperImpl();

    @InjectMocks
    private FileApprovalService fileApprovalService;

    @Captor
    ArgumentCaptor<List<S3File>> approvedCaptor;
    @Captor
    ArgumentCaptor<Set<Integer>> rejectedCaptor;
    @Captor
    ArgumentCaptor<Integer> deleteCaptor;

    private SubmissionApprovalDTO getMockSubmissionApprovalDto(){

        DetailsDTO details = new DetailsDTO();
        details.setSubmissionId(100);
        List<BundlesDTO> bundles = new ArrayList<>();

        BundlesDTO bundle1 = new BundlesDTO();
        bundle1.setId(1);
        bundle1.setReviewDecision("approved");
        bundles.add(bundle1);

        BundlesDTO bundle2 = new BundlesDTO();
        bundle2.setReviewDecision("approved");
        bundle2.setId(2);
        bundles.add(bundle2);

        BundlesDTO bundle3 = new BundlesDTO();
        bundle3.setReviewDecision("approved");
        bundle3.setId(3);
        bundles.add(bundle3);

        details.setBundles(bundles);

        SubmissionApprovalDTO dto = new SubmissionApprovalDTO();
        dto.setSubmissionDetails(details);

        return dto;
    }

    private List<DataFile> getMockDataFiles(){
        List<DataFile> dfs= new ArrayList<>(3);

        DataFile df1 = new DataFile();
        S3File s3f1 = new S3File();
        s3f1.setId(1);
        s3f1.setFilePath("bucket/path/file1");
        DataFileCategory cat1 = new DataFileCategory();
        cat1.setCategoryGroup("data");
        cat1.setName("Tabular Data - Non-harmonized");
        df1.setFileCategory(cat1);
        df1.setId(1);
        df1.setS3File(s3f1);
        df1.setSubmissionId(100);
        df1.setDictionaryFileId(3);
        df1.setMetadataFileId(2);

        DataFile df2 = new DataFile();
        S3File s3f2 = new S3File();
        s3f2.setId(2);
        s3f2.setFilePath("bucket/path/file2");
        DataFileCategory cat2 = new DataFileCategory();
        cat2.setCategoryGroup("meta");
        cat2.setName("File Metadata - Non-harmonized");

        df2.setFileCategory(cat2);
        df2.setId(2);
        df2.setS3File(s3f2);
        df2.setSubmissionId(100);
        df2.setMetaValidationFailed(false);

        DataFile df3 = new DataFile();
        S3File s3f3 = new S3File();
        s3f3.setId(3);
        s3f3.setFilePath("bucket/path/file3");
        DataFileCategory cat3 = new DataFileCategory();
        cat3.setCategoryGroup("dict");
        cat3.setName("File Data Dictionary - Non-harmonized");

        df3.setFileCategory(cat3);
        df3.setId(3);
        df3.setS3File(s3f3);
        df3.setSubmissionId(100);
        df3.setDictValidationFailed(false);

        dfs.add(df1);
        dfs.add(df2);
        dfs.add(df3);

        return dfs;
    }

    @Test
    void testProcessSubmission_ApprovedHappyPath(){
        SubmissionApprovalDTO mockDto = getMockSubmissionApprovalDto();
        mockDto.setFileRejectionReason("");
        List<DataFile> dataFiles = getMockDataFiles();

        DataSubmission submission = new DataSubmission();
        submission.setId(100);
        submission.setStudyId(10);
        Study study = new Study();
        study.setUuid("study123");
        LkupStatus fileStatus = new LkupStatus();
        fileStatus.setUsage("file");
        fileStatus.setName("approved");
        LkupStatus submissionStatus = new LkupStatus();
        submissionStatus.setId(87);
        submissionStatus.setUsage("data_submission");
        submissionStatus.setName("completed");

        StudyPropertyValue spvHasDataFiles = new StudyPropertyValue();
        spvHasDataFiles.setPropertyValue("Yes");
        spvHasDataFiles.setStudyId(10);

        //lookup
        when(dataSubmissionRepository.findById(100))
                .thenReturn(Optional.of(submission));
        when(studyRepository.findStudyById(10))
                .thenReturn(study);
        when(lkupStatusRepository.findByUsageAndName("file", "approved"))
                .thenReturn(Optional.of(fileStatus));
        when(lkupStatusRepository.findByUsageAndName("data_submission", "completed"))
                .thenReturn(Optional.of(submissionStatus));
        //approved
        when(dataFileRepository.findById(anyInt()))
                .thenAnswer(invocation -> {
                    int fileId = invocation.getArgument(0);
                    return Optional.ofNullable(dataFiles.stream().filter(file -> file.getId() == fileId).findFirst().orElse(null));
                });
        when(dataFileRepository.findById(2))
                .thenReturn(Optional.of(dataFiles.get(1)));
        when(dataFileRepository.findById(3))
                .thenReturn(Optional.of(dataFiles.get(2)));
        when(studyPropertyValueRepository.findByEntityProperty_NameAndStudyId("has_data_files", 10))
                .thenReturn(spvHasDataFiles);
        //move
        when(storageService.moveToApproved(any()))
                .thenReturn(true);

        boolean response = fileApprovalService.processSubmission(mockDto);

        assertTrue(response);
        verify(dataFileRepository, times(3)).findById(anyInt());
        verify(storageService).moveToApproved(approvedCaptor.capture());
        List<S3File> approvedCaptorValue = approvedCaptor.getValue();
        assertEquals(3, approvedCaptorValue.size());
        assertTrue(approvedCaptorValue.stream().map(S3File::getId).toList().containsAll(List.of(1,2,3)));
        assertTrue(approvedCaptor.getValue().get(0).getNewFileKey().contains("v1"));
        assertTrue(approvedCaptor.getValue().get(1).getNewFileKey().contains("v1"));
        assertTrue(approvedCaptor.getValue().get(2).getNewFileKey().contains("v1"));

    }

    @Test
    void testProcessSubmission_RejectedHappyPath(){
        SubmissionApprovalDTO mockDto = getMockSubmissionApprovalDto();
        mockDto.getSubmissionDetails().getBundles().forEach(bundle -> bundle.setReviewDecision("rejected"));
        mockDto.setFileRejectionReason("Because");
        List<DataFile> dataFiles = getMockDataFiles();

        DataSubmission submission = new DataSubmission();
        submission.setId(100);
        submission.setStudyId(10);
        Study study = new Study();
        study.setUuid("study123");
        LkupStatus fileStatus = new LkupStatus();
        fileStatus.setUsage("file");
        fileStatus.setName("approved");
        LkupStatus submissionStatus = new LkupStatus();
        submissionStatus.setId(87);
        submissionStatus.setUsage("data_submission");
        submissionStatus.setName("completed");

        //lookup
        when(dataSubmissionRepository.findById(100))
                .thenReturn(Optional.of(submission));
        when(studyRepository.findStudyById(10))
                .thenReturn(study);
        when(lkupStatusRepository.findByUsageAndName("file", "approved"))
                .thenReturn(Optional.of(fileStatus));
        when(lkupStatusRepository.findByUsageAndName("data_submission", "completed"))
                .thenReturn(Optional.of(submissionStatus));
        //rejected
        when(dataFileRepository.findById(1))
                .thenReturn(Optional.of(dataFiles.get(0)));
        when(dataFileRepository.findById(2))
                .thenReturn(Optional.of(dataFiles.get(1)));
        when(dataFileRepository.findById(3))
                .thenReturn(Optional.of(dataFiles.get(2)));
        //delete
        when(dataFileService.deleteS3FileAndEntity(any(S3File.class)))
                .thenReturn(true);

        boolean response = fileApprovalService.processSubmission(mockDto);

        assertTrue(response);
        verify(dataFileRepository, times(5)).findById(anyInt());
        verify(dataFileRepository, times(1)).updateForeignKeysToNull(rejectedCaptor.capture());
        verify(dataFileRepository, times(3)).deleteById(deleteCaptor.capture());
        List<Integer> deleteCaptorValues = deleteCaptor.getAllValues();
        Set<Integer> rejectedCaptorValues = rejectedCaptor.getValue();
        assertEquals(3, rejectedCaptorValues.size());
        assertEquals(3, deleteCaptorValues.size());
        assertTrue(deleteCaptorValues.containsAll(rejectedCaptorValues));
    }

    @Test
    void testProcessSubmission_NullDecision(){
        SubmissionApprovalDTO mockDto = getMockSubmissionApprovalDto();
        mockDto.getSubmissionDetails().getBundles().forEach(bundle -> bundle.setReviewDecision(null));

        DataSubmission submission = new DataSubmission();
        submission.setId(100);
        submission.setStudyId(10);
        Study study = new Study();
        study.setUuid("study123");
        LkupStatus status = new LkupStatus();
        status.setUsage("file");
        status.setName("approved");

        //lookup
        when(dataSubmissionRepository.findById(100))
                .thenReturn(Optional.of(submission));

        assertThrows(BadDataException.class, () -> fileApprovalService.processSubmission(mockDto));
    }

    @Test
    void testProcessSubmission_InvalidDecision(){
        SubmissionApprovalDTO mockDto = getMockSubmissionApprovalDto();
        mockDto.getSubmissionDetails().getBundles().forEach(bundle -> bundle.setReviewDecision("idk"));

        DataSubmission submission = new DataSubmission();
        submission.setId(100);
        submission.setStudyId(10);
        Study study = new Study();
        study.setUuid("study123");
        LkupStatus status = new LkupStatus();
        status.setUsage("file");
        status.setName("approved");

        //lookup
        when(dataSubmissionRepository.findById(100))
                .thenReturn(Optional.of(submission));

        assertThrows(BadDataException.class, () -> fileApprovalService.processSubmission(mockDto));
    }

    @Test
    void testProcessSubmission_EmptyApprovedAndRejected(){
        SubmissionApprovalDTO mockDto = getMockSubmissionApprovalDto();
        mockDto.getSubmissionDetails().getBundles().get(0).setReviewDecision("rejected");

        DataSubmission submission = new DataSubmission();
        submission.setId(100);
        submission.setStudyId(10);
        Study study = new Study();
        study.setUuid("study123");
        LkupStatus status = new LkupStatus();
        status.setUsage("file");
        status.setName("approved");

        //lookup
        when(dataSubmissionRepository.findById(100))
                .thenReturn(Optional.of(submission));
        when(dataFileRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(DataFileNotFoundException.class, () -> fileApprovalService.processSubmission(mockDto));
    }

    @Test
    void testGetSubmissionBundleInfo_HappyPath() {
        Integer submissionId = 100;
        DataSubmission submission = new DataSubmission();
        submission.setId(submissionId);
        submission.setStudyId(10);
        submission.setSubmitterUserId(8);
        List<DataFile> dataFiles = getMockDataFiles();
        Users user = new Users();
        user.setId(8);
        user.setFirstName("Test");
        user.setLastName("McTestington");
        ViewStudy study = new ViewStudy();
        study.setPhs("phs123456");
        study.setStudyName("Study Name");
        study.setStudyId(10);
        study.setCenter("BAH");

        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));
        when(dataFileRepository.findBySubmissionId(submissionId))
                .thenReturn(dataFiles);
        when(usersRepository.findById(8))
                .thenReturn(Optional.of(user));
        when(viewStudyRepository.findByStudyId(10))
                .thenReturn(Optional.of(study));

        DetailsDTO response = fileApprovalService.getSubmissionBundleInfo(submissionId);

        assertEquals(100, response.getSubmissionId());
        assertEquals("BAH", response.getCenter());
        assertEquals("Test McTestington", response.getCenterRep());
        assertEquals("Study Name", response.getStudyName());
        assertEquals("phs123456", response.getPhs());
        assertEquals(1, response.getBundles().size());
        assertFalse(response.getBundles().get(0).getDictFailed());
        assertFalse(response.getBundles().get(0).getMetaFailed());
    }

    @Test
    void testGetSubmissionBundleInfo_UnassignedFile() {
        Integer submissionId = 100;
        DataSubmission submission = new DataSubmission();
        submission.setId(submissionId);
        submission.setStudyId(10);
        submission.setSubmitterUserId(8);
        List<DataFile> dataFiles = getMockDataFiles();
        dataFiles.get(0).setDictionaryFileId(null);
        Users user = new Users();
        user.setId(8);
        user.setFirstName("Test");
        user.setLastName("McTestington");
        ViewStudy study = new ViewStudy();
        study.setPhs("phs123456");
        study.setStudyName("Study Name");
        study.setStudyId(10);
        study.setCenter("BAH");

        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));
        when(dataFileRepository.findBySubmissionId(submissionId))
                .thenReturn(dataFiles);
        when(usersRepository.findById(8))
                .thenReturn(Optional.of(user));
        when(viewStudyRepository.findByStudyId(10))
                .thenReturn(Optional.of(study));

        DetailsDTO response = fileApprovalService.getSubmissionBundleInfo(submissionId);

        assertEquals(100, response.getSubmissionId());
        assertEquals("BAH", response.getCenter());
        assertEquals("Test McTestington", response.getCenterRep());
        assertEquals("Study Name", response.getStudyName());
        assertEquals("phs123456", response.getPhs());
        assertEquals(2, response.getBundles().size());
        assertTrue(response.getBundles().get(0).getDictFailed());
        assertFalse(response.getBundles().get(0).getMetaFailed());
    }

    @Test
    void testGetSubmissionBundleInfo_DocumentFile() {
        Integer submissionId = 100;
        DataSubmission submission = new DataSubmission();
        submission.setId(submissionId);
        submission.setStudyId(10);
        submission.setSubmitterUserId(8);
        List<DataFile> dataFiles = getMockDataFiles();
        dataFiles.get(0).setDictionaryFileId(null);
        dataFiles.get(2).getFileCategory().setCategoryGroup("document");
        Users user = new Users();
        user.setId(8);
        user.setFirstName("Test");
        user.setLastName("McTestington");
        ViewStudy study = new ViewStudy();
        study.setPhs("phs123456");
        study.setStudyName("Study Name");
        study.setStudyId(10);
        study.setCenter("BAH");

        when(dataSubmissionRepository.findById(submissionId))
                .thenReturn(Optional.of(submission));
        when(dataFileRepository.findBySubmissionId(submissionId))
                .thenReturn(dataFiles);
        when(usersRepository.findById(8))
                .thenReturn(Optional.of(user));
        when(viewStudyRepository.findByStudyId(10))
                .thenReturn(Optional.of(study));

        DetailsDTO response = fileApprovalService.getSubmissionBundleInfo(submissionId);

        assertEquals(100, response.getSubmissionId());
        assertEquals("BAH", response.getCenter());
        assertEquals("Test McTestington", response.getCenterRep());
        assertEquals("Study Name", response.getStudyName());
        assertEquals("phs123456", response.getPhs());
        assertEquals(2, response.getBundles().size());
        assertTrue(response.getBundles().get(0).getDictFailed());
        assertFalse(response.getBundles().get(0).getMetaFailed());
    }

}
