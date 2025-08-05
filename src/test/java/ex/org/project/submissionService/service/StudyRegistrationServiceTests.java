// package ex.org.project.submissionService.service;

// import ex.org.project.submissionService.auth.UserAuthorizationException;
// import ex.org.project.submissionService.emails.EmailRequestService;
// import ex.org.project.submissionService.exceptions.custom.*;
// import ex.org.project.submissionService.mappers.*;
// import ex.org.project.submissionService.models.*;
// import ex.org.project.submissionService.models.dtos.EntityPropertyDTO;
// import ex.org.project.submissionService.models.dtos.StudyPropertyValueDTO;
// import ex.org.project.submissionService.models.dtos.StudyRegistrationDTO;
// import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
// import ex.org.project.submissionService.repositories.*;
// import ex.org.project.submissionService.services.*;
// import org.junit.jupiter.api.Assertions;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.*;
// import org.springframework.mock.web.MockMultipartFile;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;

// import org.mockito.MockitoAnnotations;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;


// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// import java.util.*;


// public class StudyRegistrationServiceTests {

//     @Mock UsersRepository usersRepository;
//     @Mock LkupDCCRepository dccRepository;
//     @Mock StudyRepository studyRepository;
//     @Mock ViewStudyRepository viewStudyRepository;
//     @Mock StudyPropertyValueRepository studyPropertyValueRepository;
//     @Mock LkupPropertySourceRepository propertySourceRepository;
//     @Mock EntityPropertyRepository entityPropertyRepository;
//     @Mock LkupPropertyCodelistValueRepository codelistValueRepository;
//     @Mock LkupStatusRepository statusRepository;
//     @Mock EntityPropertyMtaMappingRepository mtaMappingRepository;
//     @Mock AwsStorageService storageService;
//     @Mock PdfService pdfService;
//     @Mock EmailRequestService emailRequestService;

//     @Spy StudyPropertyValueMapper studyPropertyValueMapper = new StudyPropertyValueMapperImpl();
//     @Spy ViewStudyDccMapper viewStudyDccMapper = new ViewStudyDccMapperImpl();

//     @InjectMocks StudyRegistrationService studyRegistrationService;
//     @Mock UserFileUploadRepository UserFileUploadRepository;

//     @Captor ArgumentCaptor<List<StudyPropertyValue>> spvCaptor;

//     private static final String PDF_FILE_NAME = "TECH_phs9999_Study Title Goes Here.pdf";
//     private static final String BAD_PDF_FILE_NAME = "test_dcc12345.pdf";

//     @BeforeEach
//     void setUp() {
//         MockitoAnnotations.openMocks(this);
//     }

//     @Test
//     void testRegisterNewStudy_NormalHappyPath(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         Study study = new Study();
//         study.setId(1);
//         study.setDcc(dcc);
//         LkupPropertySource propertySource = new LkupPropertySource();
//         propertySource.setId(1);
//         propertySource.setName("dbGaP/MTA");
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(dcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));
//         when(studyRepository.save(any(Study.class)))
//                 .thenReturn(study);
//         when(storageService.uploadMtaForm(any(MockMultipartFile.class), anyString()))
//                 .thenReturn("aws/file/path");
//         when(entityPropertyRepository.findByName("phs"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("dcc"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("has_data_files"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(pdfService.parseStudyRegistrationForm(pdfMultiPart))
//                 .thenReturn(getParsedFields());
//         when(propertySourceRepository.findByName("dbGaP/MTA"))
//                 .thenReturn(Optional.of(propertySource));
//         when(entityPropertyRepository.findAllByPropertySourceId(propertySource.getId()))
//                 .thenReturn(getEntityProperties());
//         when(mtaMappingRepository.findAll())
//                 .thenReturn(getMtaMappings());
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getCodelistValues());
//         when(viewStudyRepository.findByStudyId(1))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         Map<String, Integer> response = studyRegistrationService.registerNewStudy(pdfMultiPart, 1);

//         assertEquals(1, response.get("studyId"));
//         verify(studyPropertyValueRepository).saveAll(spvCaptor.capture());
//         List<StudyPropertyValue> spvList = spvCaptor.getValue();
//         for(StudyPropertyValue spv : spvList) {
//             assertEquals(response.get("studyId"), spv.getStudyId());
//         }
//         assertEquals("Value1", spvList.get(0).getPropertyValue());
//         assertEquals("EP1", spvList.get(0).getEntityProperty().getName());
//         assertEquals("Value2", spvList.get(1).getPropertyValue());
//         assertEquals("EP2", spvList.get(1).getEntityProperty().getName());
//         assertEquals("test1", spvList.get(2).getPropertyValue());
//         assertEquals("EP3", spvList.get(2).getEntityProperty().getName());
//         assertEquals("validate1", spvList.get(3).getPropertyValue());
//         assertEquals("EP4", spvList.get(3).getEntityProperty().getName());
//     }

//     @Test
//     void testRegisterNewStudy_NonPdfUpload(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "text/example", "test".getBytes());
//         assertThrows(PdfParsingException.class, () -> studyRegistrationService.registerNewStudy(pdfMultiPart, 1));
//     }

//     @Test
//     void testRegisterNewStudy_StatusNotFound(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");

//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input Status"))
//                 .thenThrow(new StatusNotFoundException("StatusNotFoundException"));

//         assertThrows(StatusNotFoundException.class, () -> studyRegistrationService.registerNewStudy(pdfMultiPart, 1));
//     }

//     @Test
//     void testRegisterNewStudy_DccNotFound(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupStatus status = new LkupStatus();
//         status.setId(25);

//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.empty());
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));

//         assertThrows(PdfParsingException.class, () -> studyRegistrationService.registerNewStudy(pdfMultiPart, 1));
//     }

//     @Test
//     void testRegisterNewStudy_SubmittingToWrongDcc(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupDCC userDcc = new LkupDCC();
//         dcc.setId(1);
//         dcc.setName("UP");
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(userDcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));

//         assertThrows(UserAuthorizationException.class, () -> studyRegistrationService.registerNewStudy(pdfMultiPart, 1));
//     }

//     @Test
//     void testRegisterNewStudy_BadFileName(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(BAD_PDF_FILE_NAME, BAD_PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         assertThrows(PdfParsingException.class, () -> studyRegistrationService.registerNewStudy(pdfMultiPart, 1));
//     }

//     @Test
//     void testRegisterNewStudy_ValueIndexHappyPath(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         Study study = new Study();
//         study.setId(1);
//         study.setDcc(dcc);
//         LkupPropertySource propertySource = new LkupPropertySource();
//         propertySource.setId(1);
//         propertySource.setName("dbGaP/MTA");
//         List<EntityProperty> valueIndexEPs= getEntityProperties().stream().filter(ep -> ep.getCodeListId() == null).toList();
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(dcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));
//         when(studyRepository.save(any(Study.class)))
//                 .thenReturn(study);
//         when(storageService.uploadMtaForm(any(MockMultipartFile.class), anyString()))
//                 .thenReturn("aws/file/path");
//         when(entityPropertyRepository.findByName("phs"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("dcc"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("has_data_files"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(pdfService.parseStudyRegistrationForm(pdfMultiPart))
//                 .thenReturn(getValueIndexFields());
//         when(propertySourceRepository.findByName("dbGaP/MTA"))
//                 .thenReturn(Optional.of(propertySource));
//         when(entityPropertyRepository.findAllByPropertySourceId(propertySource.getId()))
//                 .thenReturn(valueIndexEPs);
//         when(mtaMappingRepository.findAll())
//                 .thenReturn(getValueIndexMapping());
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getCodelistValues());
//         when(viewStudyRepository.findByStudyId(1))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         Map<String, Integer> response = studyRegistrationService.registerNewStudy(pdfMultiPart, 1);

//         assertEquals(1, response.get("studyId"));
//         verify(studyPropertyValueRepository).saveAll(spvCaptor.capture());
//         List<StudyPropertyValue> spvList = spvCaptor.getValue();
//         for(StudyPropertyValue spv : spvList) {
//             assertEquals(response.get("studyId"), spv.getStudyId());
//         }
//         assertEquals("Value0_0", spvList.get(0).getPropertyValue());
//         assertEquals("EP1", spvList.get(0).getEntityProperty().getName());
//         assertEquals(0, spvList.get(0).getValueIndex());
//         assertEquals("Value1_0", spvList.get(1).getPropertyValue());
//         assertEquals("EP1", spvList.get(1).getEntityProperty().getName());
//         assertEquals(1, spvList.get(1).getValueIndex());
//         assertEquals("Value0_1", spvList.get(2).getPropertyValue());
//         assertEquals("EP2", spvList.get(2).getEntityProperty().getName());
//         assertEquals(0, spvList.get(2).getValueIndex());
//         assertEquals("Value1_1", spvList.get(3).getPropertyValue());
//         assertEquals("EP2", spvList.get(3).getEntityProperty().getName());
//         assertEquals(1, spvList.get(3).getValueIndex());
//     }

//     @Test
//     void testRegisterNewStudy_CommaSeparatedHappyPath(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         Study study = new Study();
//         study.setId(1);
//         study.setDcc(dcc);
//         LkupPropertySource propertySource = new LkupPropertySource();
//         propertySource.setId(1);
//         propertySource.setName("dbGaP/MTA");
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(dcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));
//         when(studyRepository.save(any(Study.class)))
//                 .thenReturn(study);
//         when(storageService.uploadMtaForm(any(MockMultipartFile.class), anyString()))
//                 .thenReturn("aws/file/path");
//         when(entityPropertyRepository.findByName("phs"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("dcc"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("has_data_files"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(pdfService.parseStudyRegistrationForm(pdfMultiPart))
//                 .thenReturn(Map.of("Comma Separated", "test1, test2, test3"));
//         when(propertySourceRepository.findByName("dbGaP/MTA"))
//                 .thenReturn(Optional.of(propertySource));
//         when(entityPropertyRepository.findAllByPropertySourceId(propertySource.getId()))
//                 .thenReturn(List.of(getEntityProperties().get(2)));
//         when(mtaMappingRepository.findAll())
//                 .thenReturn(getCommaSeparatedMapping());
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getCodelistValues());
//         when(viewStudyRepository.findByStudyId(1))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         Map<String, Integer> response = studyRegistrationService.registerNewStudy(pdfMultiPart, 1);

//         assertEquals(1, response.get("studyId"));
//         verify(studyPropertyValueRepository).saveAll(spvCaptor.capture());
//         List<StudyPropertyValue> spvList = spvCaptor.getValue();
//         System.out.println(spvList);
//         for(StudyPropertyValue spv : spvList) {
//             assertEquals(response.get("studyId"), spv.getStudyId());
//         }
//         assertEquals("test1", spvList.get(0).getPropertyValue());
//         assertEquals("EP3", spvList.get(0).getEntityProperty().getName());
//         assertEquals("test2", spvList.get(1).getPropertyValue());
//         assertEquals("EP3", spvList.get(1).getEntityProperty().getName());
//         assertEquals("test3", spvList.get(2).getPropertyValue());
//         assertEquals("EP3", spvList.get(2).getEntityProperty().getName());
//     }

//     @Test
//     void testRegisterNewStudy_SpecialMappings(){
//         //one-to-one: "Institutional Certifications", "NHGRI Genomic Data Sharing  Submission Information"
//         //codelisted: radios -> "On", "Off", "null",
//         //comma separated: types, types_other_specify, supporting institutes
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         Study study = new Study();
//         study.setId(1);
//         study.setDcc(dcc);
//         LkupPropertySource propertySource = new LkupPropertySource();
//         propertySource.setId(1);
//         propertySource.setName("dbGaP/MTA");
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(dcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));
//         when(studyRepository.save(any(Study.class)))
//                 .thenReturn(study);
//         when(storageService.uploadMtaForm(any(MockMultipartFile.class), anyString()))
//                 .thenReturn("aws/file/path");
//         when(entityPropertyRepository.findByName("phs"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("dcc"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("has_data_files"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(pdfService.parseStudyRegistrationForm(pdfMultiPart))
//                 .thenReturn(getSpecialParsedFields());
//         when(propertySourceRepository.findByName("dbGaP/MTA"))
//                 .thenReturn(Optional.of(propertySource));
//         when(entityPropertyRepository.findAllByPropertySourceId(propertySource.getId()))
//                 .thenReturn(getSpecialEntityProperties());
//         when(mtaMappingRepository.findAll())
//                 .thenReturn(getSpecialMtaMappings());
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getSpecialCodelistValues());
//         when(viewStudyRepository.findByStudyId(1))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         Map<String, Integer> response = studyRegistrationService.registerNewStudy(pdfMultiPart, 1);

//         assertEquals(1, response.get("studyId"));
//         verify(studyPropertyValueRepository).saveAll(spvCaptor.capture());
//         List<StudyPropertyValue> spvList = spvCaptor.getValue();
//         for(StudyPropertyValue spv : spvList) {
//             assertEquals(response.get("studyId"), spv.getStudyId());
//         }
//         assertEquals("Yes", spvList.get(0).getPropertyValue());
//         assertEquals("Institutional Certifications", spvList.get(0).getEntityProperty().getName());
//         assertEquals("No", spvList.get(1).getPropertyValue());
//         assertEquals("NHGRI Genomic Data", spvList.get(1).getEntityProperty().getName());
//         assertEquals("tis on", spvList.get(2).getPropertyValue());
//         assertEquals("Radios", spvList.get(2).getEntityProperty().getName());
//         assertEquals("type1", spvList.get(3).getPropertyValue());
//         assertEquals("Types", spvList.get(3).getEntityProperty().getName());
//         assertEquals("other", spvList.get(4).getPropertyValue());
//         assertEquals("types_other_specify", spvList.get(4).getEntityProperty().getName());
//     }

//     @Test
//     void testRegisterNewStudy_DataSubmissionRadio_Generation(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         Study study = new Study();
//         study.setId(1);
//         study.setDcc(dcc);
//         LkupPropertySource propertySource = new LkupPropertySource();
//         propertySource.setId(1);
//         propertySource.setName("dbGaP/MTA");
//         EntityProperty ep3 = new EntityProperty();
//         ep3.setId(3);
//         ep3.setName("EP3");
//         ep3.setCodeListId(33);
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(dcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));
//         when(studyRepository.save(any(Study.class)))
//                 .thenReturn(study);
//         when(storageService.uploadMtaForm(any(MockMultipartFile.class), anyString()))
//                 .thenReturn("aws/file/path");
//         when(entityPropertyRepository.findByName("phs"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("dcc"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("has_data_files"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(pdfService.parseStudyRegistrationForm(pdfMultiPart))
//                 .thenReturn(Map.of("Data submission radio", "generation"));
//         when(propertySourceRepository.findByName("dbGaP/MTA"))
//                 .thenReturn(Optional.of(propertySource));
//         when(entityPropertyRepository.findAllByPropertySourceId(propertySource.getId()))
//                 .thenReturn(List.of(ep3));
//         when(mtaMappingRepository.findAll())
//                 .thenReturn(getDataSubmissionRadioMapping());
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getDataSubmissionRadioCodelistValues());
//         when(viewStudyRepository.findByStudyId(1))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         Map<String, Integer> response = studyRegistrationService.registerNewStudy(pdfMultiPart, 1);

//         assertEquals(1, response.get("studyId"));
//         verify(studyPropertyValueRepository).saveAll(spvCaptor.capture());
//         List<StudyPropertyValue> spvList = spvCaptor.getValue();
//         for(StudyPropertyValue spv : spvList) {
//             assertEquals(response.get("studyId"), spv.getStudyId());
//         }
//         assertEquals("date", spvList.get(0).getPropertyValue());
//         assertEquals("EP3", spvList.get(0).getEntityProperty().getName());
//     }

//     @Test
//     void testRegisterNewStudy_DataSubmissionRadio_Publication(){
//         MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, PDF_FILE_NAME, "application/pdf", "test".getBytes());
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(2);
//         dcc.setName("Tech");
//         LkupStatus status = new LkupStatus();
//         status.setId(25);
//         Study study = new Study();
//         study.setId(1);
//         study.setDcc(dcc);
//         LkupPropertySource propertySource = new LkupPropertySource();
//         propertySource.setId(1);
//         propertySource.setName("dbGaP/MTA");
//         EntityProperty ep3 = new EntityProperty();
//         ep3.setId(3);
//         ep3.setName("EP3");
//         ep3.setCodeListId(33);
//         Users submitter = new Users();
//         submitter.setId(1);
//         submitter.setDcc(dcc);

//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(submitter));
//         when(dccRepository.findByNameContainingIgnoreCase("TECH"))
//                 .thenReturn(Optional.of(dcc));
//         when(statusRepository.findByUsageAndName("study", "Pending DCC Input"))
//                 .thenReturn(Optional.of(status));
//         when(studyRepository.save(any(Study.class)))
//                 .thenReturn(study);
//         when(storageService.uploadMtaForm(any(MockMultipartFile.class), anyString()))
//                 .thenReturn("aws/file/path");
//         when(entityPropertyRepository.findByName("phs"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("dcc"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(entityPropertyRepository.findByName("has_data_files"))
//                 .thenReturn(Optional.of(new EntityProperty()));
//         when(pdfService.parseStudyRegistrationForm(pdfMultiPart))
//                 .thenReturn(Map.of("Data submission radio", "publication"));
//         when(propertySourceRepository.findByName("dbGaP/MTA"))
//                 .thenReturn(Optional.of(propertySource));
//         when(entityPropertyRepository.findAllByPropertySourceId(propertySource.getId()))
//                 .thenReturn(List.of(ep3));
//         when(mtaMappingRepository.findAll())
//                 .thenReturn(getDataSubmissionRadioMapping());
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getDataSubmissionRadioCodelistValues());
//         when(viewStudyRepository.findByStudyId(1))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         Map<String, Integer> response = studyRegistrationService.registerNewStudy(pdfMultiPart, 1);

//         assertEquals(1, response.get("studyId"));
//         verify(studyPropertyValueRepository).saveAll(spvCaptor.capture());
//         List<StudyPropertyValue> spvList = spvCaptor.getValue();
//         for(StudyPropertyValue spv : spvList) {
//             assertEquals(response.get("studyId"), spv.getStudyId());
//         }
//         assertEquals("timeline", spvList.get(0).getPropertyValue());
//         assertEquals("EP3", spvList.get(0).getEntityProperty().getName());
//     }

//     private Map<String, String> getSpecialParsedFields(){
//         Map<String, String> fields = new HashMap<>();
//         fields.put("Institutional Certifications", "On");
//         fields.put("NHGRI Genomic Data Sharing  Submission Information", "Off");
//         fields.put("types", "type1, other");
//         fields.put("Field4", "On");
//         fields.put("Field5", "Off");
//         fields.put("Null Button", "null");
//         fields.put("Supporting Institutes", "institute1, institute2");
//         return fields;
//     }

//     private Map<String, String> getParsedFields(){
//         Map<String, String> fields = new HashMap<>();
//         fields.put("Field1", "Value1");
//         fields.put("Field2", "Value2");
//         fields.put("Field3", "test1");
//         fields.put("Field4", "validate1");
//         return fields;
//     }

//     private Map<String, String> getValueIndexFields(){
//         Map<String, String> fields = new HashMap<>();
//         fields.put("URL 1", "Value0_0");
//         fields.put("Description 1", "Value0_1");
//         fields.put("URL 3", "Value1_0");
//         fields.put("Description 3", "Value1_1");
//         return fields;
//     }

//     private List<EntityPropertyMtaMapping> getValueIndexMapping(){
//         EntityPropertyMtaMapping mapping1 = new EntityPropertyMtaMapping();
//         mapping1.setId(10);
//         mapping1.setPdfFieldName("URL 1");
//         mapping1.setEntityPropertyId(1);
//         mapping1.setDescription("value_index 0");
//         EntityPropertyMtaMapping mapping2 = new EntityPropertyMtaMapping();
//         mapping2.setId(11);
//         mapping2.setPdfFieldName("Description 1");
//         mapping2.setEntityPropertyId(2);
//         mapping2.setDescription("value_index 0");
//         EntityPropertyMtaMapping mapping3 = new EntityPropertyMtaMapping();
//         mapping3.setId(20);
//         mapping3.setPdfFieldName("URL 3");
//         mapping3.setEntityPropertyId(1);
//         mapping3.setDescription("value_index 1");
//         EntityPropertyMtaMapping mapping4 = new EntityPropertyMtaMapping();
//         mapping4.setId(21);
//         mapping4.setPdfFieldName("Description 3");
//         mapping4.setEntityPropertyId(2);
//         mapping4.setDescription("value_index 1 ");
//         return List.of(mapping1, mapping2, mapping3, mapping4);
//     }

//     private List<EntityPropertyMtaMapping> getDataSubmissionRadioMapping(){
//         EntityPropertyMtaMapping generation = new EntityPropertyMtaMapping();
//         generation.setId(10);
//         generation.setPdfFieldName("Data submission radio");
//         generation.setEntityPropertyId(3);
//         generation.setCodelistId(13);
//         generation.setCodelistValueId(80);
//         generation.setDescription("generation");
//         EntityPropertyMtaMapping publication = new EntityPropertyMtaMapping();
//         publication.setId(20);
//         publication.setPdfFieldName("Data submission radio");
//         publication.setEntityPropertyId(3);
//         publication.setCodelistId(13);
//         publication.setCodelistValueId(81);
//         publication.setDescription("publication");
//         return List.of(generation, publication);
//     }

//     private List<LkupPropertyCodelistValue> getDataSubmissionRadioCodelistValues(){
//         LkupPropertyCodelistValue generation = new LkupPropertyCodelistValue();
//         generation.setId(80);
//         generation.setPropertyCodelistId(33);
//         generation.setValue("date");
//         LkupPropertyCodelistValue publication = new LkupPropertyCodelistValue();
//         publication.setId(81);
//         publication.setPropertyCodelistId(33);
//         publication.setValue("timeline");
//         return List.of(generation, publication);
//     }

//     private List<EntityPropertyMtaMapping> getCommaSeparatedMapping(){
//         EntityPropertyMtaMapping commas = new EntityPropertyMtaMapping();
//         commas.setId(1);
//         commas.setPdfFieldName("Comma Separated");
//         commas.setEntityPropertyId(3);
//         commas.setCodelistId(33);
//         commas.setCodelistValueId(null);
//         commas.setDescription("comma-separated");
//         return List.of(commas);
//     }

//     private List<EntityProperty> getEntityProperties(){
//         EntityProperty ep1 = new EntityProperty();
//         ep1.setId(1);
//         ep1.setName("EP1");
//         EntityProperty ep2 = new EntityProperty();
//         ep2.setId(2);
//         ep2.setName("EP2");
//         EntityProperty ep3 = new EntityProperty();
//         ep3.setId(3);
//         ep3.setName("EP3");
//         ep3.setCodeListId(33);
//         EntityProperty ep4 = new EntityProperty();
//         ep4.setId(4);
//         ep4.setName("EP4");
//         ep4.setCodeListId(44);
//         return List.of(ep1, ep2, ep3, ep4);
//     }

//     private List<EntityProperty> getSpecialEntityProperties(){
//         EntityProperty ep1 = new EntityProperty();
//         ep1.setId(1);
//         ep1.setName("Institutional Certifications");
//         EntityProperty ep2 = new EntityProperty();
//         ep2.setId(2);
//         ep2.setName("NHGRI Genomic Data");
//         EntityProperty ep3 = new EntityProperty();
//         ep3.setId(3);
//         ep3.setName("Radios");
//         ep3.setCodeListId(33);
//         EntityProperty ep4 = new EntityProperty();
//         ep4.setId(4);
//         ep4.setName("Types");
//         ep4.setCodeListId(44);
//         EntityProperty ep5 = new EntityProperty();
//         ep5.setId(5);
//         ep5.setName("Supporting Institutes");
//         ep5.setCodeListId(55);
//         EntityProperty ep7 = new EntityProperty();
//         ep7.setId(7);
//         ep7.setName("types_other_specify");
//         return List.of(ep1, ep2, ep3, ep4, ep5, ep7);
//     }

//     private List<EntityPropertyMtaMapping> getMtaMappings(){
//         EntityPropertyMtaMapping mapping1 = new EntityPropertyMtaMapping();
//         mapping1.setId(10);
//         mapping1.setPdfFieldName("Field1");
//         mapping1.setEntityPropertyId(1);
//         mapping1.setCodelistValueId(null);
//         EntityPropertyMtaMapping mapping2 = new EntityPropertyMtaMapping();
//         mapping2.setId(20);
//         mapping2.setPdfFieldName("Field2");
//         mapping2.setEntityPropertyId(2);
//         mapping2.setCodelistValueId(null);
//         EntityPropertyMtaMapping mapping3 = new EntityPropertyMtaMapping();
//         mapping3.setId(30);
//         mapping3.setPdfFieldName("Field3");
//         mapping3.setEntityPropertyId(3);
//         mapping3.setCodelistValueId(1);
//         EntityPropertyMtaMapping mapping4 = new EntityPropertyMtaMapping();
//         mapping4.setId(40);
//         mapping4.setPdfFieldName("Field4");
//         mapping4.setEntityPropertyId(4);
//         mapping4.setCodelistValueId(2);
//         return List.of(mapping1, mapping2, mapping3, mapping4);
//     }

//     private List<EntityPropertyMtaMapping> getSpecialMtaMappings(){
//         EntityPropertyMtaMapping mapping1 = new EntityPropertyMtaMapping();
//         mapping1.setId(10);
//         mapping1.setPdfFieldName("Institutional Certifications");
//         mapping1.setEntityPropertyId(1);
//         EntityPropertyMtaMapping mapping2 = new EntityPropertyMtaMapping();
//         mapping2.setId(20);
//         mapping2.setPdfFieldName("NHGRI Genomic Data Sharing  Submission Information");
//         mapping2.setEntityPropertyId(2);
//         EntityPropertyMtaMapping mapping3 = new EntityPropertyMtaMapping();
//         mapping3.setId(30);
//         mapping3.setPdfFieldName("types");
//         mapping3.setEntityPropertyId(4);
//         mapping3.setCodelistId(44);
//         mapping3.setDescription("comma-separated");
//         EntityPropertyMtaMapping mapping4 = new EntityPropertyMtaMapping();
//         mapping4.setId(40);
//         mapping4.setPdfFieldName("Field4");
//         mapping4.setEntityPropertyId(3);
//         mapping4.setCodelistId(33);
//         mapping4.setCodelistValueId(1);
//         EntityPropertyMtaMapping mapping5 = new EntityPropertyMtaMapping();
//         mapping5.setId(50);
//         mapping5.setPdfFieldName("Null Button");
//         mapping5.setEntityPropertyId(3);
//         mapping5.setCodelistId(33);
//         mapping5.setCodelistValueId(2);
//         EntityPropertyMtaMapping mapping6 = new EntityPropertyMtaMapping();
//         mapping6.setId(60);
//         mapping6.setPdfFieldName("Supporting Institutes");
//         mapping6.setEntityPropertyId(5);
//         mapping6.setCodelistId(55);
//         mapping6.setDescription("comma-separated");
//         EntityPropertyMtaMapping mapping7 = new EntityPropertyMtaMapping();
//         mapping7.setId(70);
//         mapping7.setPdfFieldName("types_other_specify");
//         mapping7.setEntityPropertyId(7);
//         mapping7.setDescription("comma-separated");
//         EntityPropertyMtaMapping mapping8= new EntityPropertyMtaMapping();
//         mapping8.setId(80);
//         mapping8.setPdfFieldName("Field5");
//         mapping8.setEntityPropertyId(3);
//         mapping8.setCodelistId(33);
//         mapping8.setCodelistValueId(2);
//         return List.of(mapping1, mapping2, mapping3, mapping4, mapping5, mapping6, mapping7, mapping8);
//     }

//     private List<StudyPropertyValueDTO> getSpvDtoList(){
//         EntityPropertyDTO epDto1 = new EntityPropertyDTO(1, "Test");
//         EntityPropertyDTO epDto2 = new EntityPropertyDTO(2, "Validate");
//         StudyPropertyValueDTO spvDto1 = new StudyPropertyValueDTO(1, "test1", epDto1, null, false);
//         StudyPropertyValueDTO spvDto2 = new StudyPropertyValueDTO(null, "test3", epDto1, null, false);
//         StudyPropertyValueDTO spvDto3 = new StudyPropertyValueDTO(3, "validate3", epDto2, null, false);
//         return List.of(spvDto1, spvDto2, spvDto3);
//     }

//     private List<StudyPropertyValueDTO> getSpvDtoListToDelete(){
//         EntityPropertyDTO epDto1 = new EntityPropertyDTO(1, "Test");
//         StudyPropertyValueDTO spvDto1 = new StudyPropertyValueDTO(1, "test1", epDto1, null, true);
//         //ep not required to delete
//         StudyPropertyValueDTO spvDto3 = new StudyPropertyValueDTO(3, "validate3", null, null, true);
//         return List.of(spvDto1, spvDto3);
//     }

//     private List<LkupPropertyCodelistValue> getCodelistValues() {
//         LkupPropertyCodelistValue codelistValue1 = new LkupPropertyCodelistValue();
//         codelistValue1.setId(1);
//         codelistValue1.setPropertyCodelistId(33);
//         codelistValue1.setValue("test1");
//         LkupPropertyCodelistValue codelistValue2 = new LkupPropertyCodelistValue();
//         codelistValue2.setId(2);
//         codelistValue2.setPropertyCodelistId(33);
//         codelistValue2.setValue("test2");
//         LkupPropertyCodelistValue codelistValue3 = new LkupPropertyCodelistValue();
//         codelistValue3.setId(3);
//         codelistValue3.setPropertyCodelistId(33);
//         codelistValue3.setValue("test3");
//         LkupPropertyCodelistValue codelistValue4 = new LkupPropertyCodelistValue();
//         codelistValue4.setId(4);
//         codelistValue4.setPropertyCodelistId(44);
//         codelistValue4.setValue("validate1");
//         LkupPropertyCodelistValue codelistValue5 = new LkupPropertyCodelistValue();
//         codelistValue5.setId(5);
//         codelistValue5.setPropertyCodelistId(44);
//         codelistValue5.setValue("validate2");
//         LkupPropertyCodelistValue codelistValue6 = new LkupPropertyCodelistValue();
//         codelistValue6.setId(6);
//         codelistValue6.setPropertyCodelistId(44);
//         codelistValue6.setValue("validate3");
//         return List.of(codelistValue1, codelistValue2, codelistValue3, codelistValue4, codelistValue5, codelistValue6);
//     }

//     private List<LkupPropertyCodelistValue> getSpecialCodelistValues(){
//         LkupPropertyCodelistValue codelistValue1 = new LkupPropertyCodelistValue();
//         codelistValue1.setId(1);
//         codelistValue1.setPropertyCodelistId(33);
//         codelistValue1.setValue("tis on");
//         LkupPropertyCodelistValue codelistValue2 = new LkupPropertyCodelistValue();
//         codelistValue2.setId(2);
//         codelistValue1.setPropertyCodelistId(33);
//         codelistValue2.setValue("tis off");
//         LkupPropertyCodelistValue codelistValue3 = new LkupPropertyCodelistValue();
//         codelistValue3.setId(3);
//         codelistValue3.setPropertyCodelistId(44);
//         codelistValue3.setValue("type1");
//         LkupPropertyCodelistValue codelistValue4 = new LkupPropertyCodelistValue();
//         codelistValue4.setId(4);
//         codelistValue4.setPropertyCodelistId(55);
//         codelistValue4.setValue("institute1");
//         LkupPropertyCodelistValue codelistValue5 = new LkupPropertyCodelistValue();
//         codelistValue5.setId(5);
//         codelistValue5.setPropertyCodelistId(55);
//         codelistValue5.setValue("institute2");
//         return List.of(codelistValue1, codelistValue3, codelistValue4, codelistValue5);
//     }

//     @Test
//     void testEditStudyPropertyValues_CuratorEditHappyPath(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));

//         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId);

//         assertEquals("Successfully updated property values", response);
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
//     }

//     @Test
//     void testEditStudyPropertyValues_CuratorEditWithNullEntityProperty(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         //dto set up
//         StudyPropertyValueDTO spvDto = new StudyPropertyValueDTO(1, "test", null, null, false);
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, List.of(spvDto));

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));

//         assertThrows(StudyRegRequestException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId)
//         );

//     }

//     @Test
//     void testEditStudyPropertyValues_CuratorEditAndSubmitHappyPath(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         EntityProperty entityProperty3 = new EntityProperty();
//         entityProperty3.setId(3);
//         entityProperty3.setName("release_date");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         LkupStatus status = new LkupStatus();
//         status.setId(1);
//         status.setName("Approved");
//         status.setUsage("study");

//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(entityPropertyRepository.findByName("release_date"))
//                 .thenReturn(Optional.of(entityProperty3));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));
//         when(statusRepository.findByUsageAndName("study", "Approved"))
//                 .thenReturn(Optional.of(status));
//         when(viewStudyRepository.findByStudyId(8))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", true, userId);

//         assertEquals("Successfully updated property values", response);
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(4)).save(any(StudyPropertyValue.class));
//         assertEquals(status, study.getStatus());
//     }

//     @Test
//     void testEditStudyPropertyValues_DccEditHappyPath(){
//         //set up db mocks
//         Integer userId = 2;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));

//         String response = studyRegistrationService.editStudyPropertyValues(dto, "DCC", false, userId);

//         assertEquals("Successfully updated property values", response);
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
//     }

//     @Test
//     void testEditStudyPropertyValues_DccEditAndSubmitHappyPath(){
//         //set up db mocks
//         Integer userId = 2;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         LkupStatus status = new LkupStatus();
//         status.setId(2);
//         status.setName("In Review");
//         status.setUsage("study");
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));
//         when(statusRepository.findByUsageAndName("study", "In Review"))
//                 .thenReturn(Optional.of(status));
//         when(viewStudyRepository.findByStudyId(8))
//                 .thenReturn(Optional.of(new ViewStudy()));

//         String response = studyRegistrationService.editStudyPropertyValues(dto, "DCC", true, userId);

//         assertEquals("Successfully updated property values", response);
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
//         assertEquals(status, study.getStatus());
//     }

//     @Test
//     void testEditStudyPropertyValues_CodelistedHappyPath(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setCodeListId(33);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setCodeListId(44);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("test2");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("validate2");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));
//         when(codelistValueRepository.findAll())
//                 .thenReturn(getCodelistValues());

//         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId);

//         assertEquals("Successfully updated property values", response);
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
//         verify(codelistValueRepository, times(1)).findAll();
//     }

//     @Test
//     void testEditStudyPropertyValues_CodelistedInvalidValue(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setCodeListId(33);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setCodeListId(44);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("test2");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("validate2");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);

//         List<LkupPropertyCodelistValue> codelistValues = getCodelistValues();
//         codelistValues.get(0).setValue("wrong");
//         codelistValues.get(1).setValue("fail");
//         codelistValues.get(2).setValue("bad");

//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));
//         when(codelistValueRepository.findAll())
//                 .thenReturn(codelistValues);

//         assertThrows(BadDataException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId));
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(codelistValueRepository, times(1)).findAll();
//     }

//     @Test
//     void testEditStudyPropertyValues_DeleteHappyPath(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoListToDelete());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));


//         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId);

//         assertEquals("Successfully updated property values", response);
//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(1)).deleteAllById(List.of(1, 3));
//     }

//     @Test
//     void testEditStudyPropertyValues_StudyNotFound(){
//         Integer userId = 2;
//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.empty());
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         assertThrows(StudyNotFoundException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "DCC", false, userId));
//     }

//     @Test
//     void testEditStudyPropertyValues_InvalidRole(){
//         Integer userId = 3;
//         Study study = new Study();
//         study.setId(8);
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));

//         assertThrows(BadDataException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Fail", false, userId));
//     }

//     @Test
//     void testEditStudyPropertyValues_DccTryingToEditCuratorFields(){
//         //set up db mocks
//         Integer userId = 2;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty2));

//         assertThrows(UserAuthorizationException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "DCC", false, userId));

//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//     }

//     @Test
//     void testEditStudyPropertyValues_CuratorStudyStatusNotFound(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         LkupStatus status = new LkupStatus();
//         status.setId(1);
//         status.setName("Approved");
//         status.setUsage("study");

//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));
//         when(statusRepository.findByUsageAndName("study", "approved_study"))
//                 .thenThrow(new StatusNotFoundException("Invalid Data File Status: approved_study"));

//         assertThrows(StatusNotFoundException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", true, userId));

//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
//     }

//     @Test
//     void testEditStudyPropertyValues_DccStudyStatusNotFound(){
//         //set up db mocks
//         Integer userId = 2;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         LkupStatus status = new LkupStatus();
//         status.setId(2);
//         status.setName("In Review");
//         status.setUsage("study");
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.of(spv1));
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.of(spv3));
//         when(statusRepository.findByUsageAndName("study", "In Review Status"))
//                 .thenThrow(new StatusNotFoundException("status invalid"));

//         assertThrows(StatusNotFoundException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "DCC", true, userId));

//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
//         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
//     }

//     @Test
//     void testEditStudyPropertyValues_InvalidStudyPropertyValueId(){
//         //set up db mocks
//         Integer userId = 1;
//         Integer studyId = 8;
//         Study study = new Study();
//         study.setId(studyId);
//         LkupPropertySource propertySource1 = new LkupPropertySource();
//         propertySource1.setId(1);
//         propertySource1.setName("dbGaP/MTA");
//         LkupPropertySource propertySource2 = new LkupPropertySource();
//         propertySource2.setId(2);
//         propertySource2.setName("Hub Online Submission");
//         EntityProperty entityProperty1 = new EntityProperty();
//         entityProperty1.setId(1);
//         entityProperty1.setName("Test");
//         EntityProperty entityProperty2 = new EntityProperty();
//         entityProperty2.setId(2);
//         entityProperty2.setName("Validate");

//         StudyPropertyValue spv1 = new StudyPropertyValue();
//         spv1.setId(1);
//         spv1.setPropertyValue("temp1");
//         spv1.setStudyId(8);
//         spv1.setEntityProperty(entityProperty1);
//         StudyPropertyValue spv3 = new StudyPropertyValue();
//         spv3.setId(3);
//         spv3.setPropertyValue("temp3");
//         spv3.setStudyId(8);
//         spv3.setEntityProperty(entityProperty2);
//         //dto set up
//         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(study));
//         when(propertySourceRepository.findAllByNameIn(anyList()))
//                 .thenReturn(List.of(propertySource1, propertySource2));
//         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
//                 .thenReturn(List.of(entityProperty1, entityProperty2));
//         when(studyPropertyValueRepository.findById(1))
//                 .thenReturn(Optional.empty());
//         when(studyPropertyValueRepository.findById(3))
//                 .thenReturn(Optional.empty());

//         assertThrows(StudyPropertyValuesRetrievalException.class,
//                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId));

//         verify(studyRepository, times(1)).findById(8);
//         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
//         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
//         verify(studyPropertyValueRepository).findById(anyInt());
//     }

//     @Test
//     void testGetStudyProperties_HappyPath() {
//         EntityProperty ep = new EntityProperty();
//         ep.setId(123);
//         ep.setName("Test");
//         StudyPropertyValue spv = new StudyPropertyValue();
//         spv.setId(1);
//         spv.setStudyId(8);
//         spv.setPropertyValue("test");
//         spv.setEntityProperty(ep);

//         when(studyRepository.findById(8))
//                 .thenReturn(Optional.of(new Study()));
//         when(studyPropertyValueRepository.findAllByStudyId(8))
//                 .thenReturn(List.of(spv));

//         StudyRegistrationDTO response = studyRegistrationService.getStudyProperties(8);

//         assertEquals(8, response.studyId());
//         assertEquals("test", response.studyPropertyValues().get(0).value());
//         assertEquals(1, response.studyPropertyValues().get(0).id());
//         assertEquals("Test", response.studyPropertyValues().get(0).entityProperty().name());
//         assertEquals(123, response.studyPropertyValues().get(0).entityProperty().id());

//         verify(studyPropertyValueMapper, times(1))
//                 .entityListToDtoList(List.of(spv));
//     }

//     @Test
//     void testGetUserStudiesByDcc() {
//         Users users = new Users();
//         LkupDCC dcc = new LkupDCC();
//         dcc.setId(1);
//         users.setDcc(dcc);

//         ViewStudy viewStudy = new ViewStudy();
//         viewStudy.setStudyId(1);
//         List<ViewStudy> studies = List.of(viewStudy);

//         LkupStatus pendingDCCStatus = new LkupStatus();
//         int statusId = 1;
//         pendingDCCStatus.setId(statusId);
//         pendingDCCStatus.setName("Pending DCC Input");

//         when(statusRepository.findByName("Pending DCC Input"))
//                 .thenReturn(Optional.of(pendingDCCStatus));
//         when(usersRepository.findById(1))
//                 .thenReturn(Optional.of(users));
//         when(viewStudyRepository.findDCCStudiesByStatus(1, "Pending DCC Input"))
//                 .thenReturn(studies);

//         List<UserStudyRegistrationDTO> resultDTOList = studyRegistrationService.getUserStudiesByDcc(1, "Pending DCC Input");

//         // Verify the results
//         Assertions.assertEquals(1, resultDTOList.get(0).getStudyId());
//     }

//     @Test
//     void testGetUserStudiesByDcc_UserNullDcc() {
//         when(usersRepository.findById(anyInt()))
//                 .thenReturn(Optional.of(new Users()));
//         Assertions.assertThrows(UserAuthorizationException.class, () -> {
//             studyRegistrationService.getUserStudiesByDcc(1, "");
//         });
//     }

//     @Test
//     void testGetUserStudiesByCuratorNoStudies() {
//         LkupStatus approvedStatus = new LkupStatus();
//         int statusId = 1;
//         approvedStatus.setId(statusId);
//         approvedStatus.setName("Approved");
//         when(statusRepository.findByName("Approved"))
//                 .thenReturn(Optional.of(approvedStatus));
//         when(viewStudyRepository.findCuratorStudiesByStatus("Approved"))
//                 .thenReturn(new ArrayList<>(0));

//         List<UserStudyRegistrationDTO> actualDTOs = studyRegistrationService.getUserStudiesByCurator("Approved");

//         assertEquals(new ArrayList<>(0), actualDTOs);
//     }

//     @Test
//     void testDeleteStudiesByCurator_SuccessfulDeletion() {
//         // Arrange
//         Integer studyId = 123;
//         Study study = new Study();
//         study.setId(studyId);
//         study.setFileUrl("s3://bucket/objectKey");

//         when(studyRepository.findById(eq(studyId)))
//                 .thenReturn(Optional.of(study));
//         when(storageService.deleteStudyFromS3(eq(study))).thenReturn(true);

//         studyRegistrationService.deleteStudiesByCurator(studyId);

//         // Assert
//         verify(studyPropertyValueRepository).deleteAllByStudyId(eq(studyId));
//         verify(studyRepository).delete(eq(study));
//     }
//     @Test
//     void deleteStudiesByCurator_ShouldThrowFileDeletionException() {

//         Integer studyId = 1;
//         Study study = new Study();
//         study.setId(studyId);
//         study.setFileUrl("s3://url/test.txt");

//         when(studyRepository.findById(studyId))
//                 .thenReturn(Optional.of(study));
//         when(storageService.deleteStudyFromS3(eq(study)))
//                 .thenReturn(false);

//         assertThrows(FileDeletionException.class, () -> studyRegistrationService.deleteStudiesByCurator(studyId));

//         verify(studyPropertyValueRepository, never()).deleteAllByStudyId(studyId);
//         verify(studyRepository, never()).delete(study);
//     }

// }