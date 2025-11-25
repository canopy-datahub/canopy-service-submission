 package ex.org.project.submissionService.service;

 import ex.org.project.datahub.auth.exception.UserAuthorizationException;
 import ex.org.project.submissionService.emails.EmailRequestService;
 import ex.org.project.submissionService.exceptions.custom.*;
 import ex.org.project.submissionService.mappers.*;
 import ex.org.project.submissionService.models.*;
 import ex.org.project.submissionService.models.dtos.EntityPropertyDTO;
 import ex.org.project.submissionService.models.dtos.StudyPropertyValueDTO;
 import ex.org.project.submissionService.models.dtos.StudyRegistrationDTO;
 import ex.org.project.submissionService.models.dtos.UserStudyRegistrationDTO;
 import ex.org.project.submissionService.repositories.*;
 import ex.org.project.submissionService.services.*;
 import org.junit.jupiter.api.Assertions;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.mockito.*;
 import org.springframework.mock.web.MockMultipartFile;
 import org.mockito.InjectMocks;
 import org.mockito.Mock;

 import org.mockito.MockitoAnnotations;

 import java.util.ArrayList;
 import java.util.List;
 import java.util.Optional;


 import static org.junit.jupiter.api.Assertions.*;
 import static org.mockito.ArgumentMatchers.*;
 import static org.mockito.Mockito.*;

 import java.util.*;


 public class StudyRegistrationServiceTests {

     @Mock UsersRepository usersRepository;
     @Mock LkupCenterRepository centerRepository;
     @Mock StudyRepository studyRepository;
     @Mock ViewStudyRepository viewStudyRepository;
     @Mock StudyPropertyValueRepository studyPropertyValueRepository;
     @Mock LkupPropertySourceRepository propertySourceRepository;
     @Mock EntityPropertyRepository entityPropertyRepository;
     @Mock LkupPropertyCodelistValueRepository codelistValueRepository;
     @Mock LkupStatusRepository statusRepository;
     @Mock EntityPropertyMtaMappingRepository mtaMappingRepository;
     @Mock AwsStorageService storageService;
     @Mock EmailRequestService emailRequestService;

     @Spy StudyPropertyValueMapper studyPropertyValueMapper = new StudyPropertyValueMapperImpl();
     @Spy ViewStudyCenterMapper viewStudyDccMapper = new ViewStudyCenterMapperImpl();

     @InjectMocks StudyRegistrationService studyRegistrationService;
     @Mock UserFileUploadRepository UserFileUploadRepository;

     @Captor ArgumentCaptor<List<StudyPropertyValue>> spvCaptor;


     @BeforeEach
     void setUp() {
         MockitoAnnotations.openMocks(this);
     }


     private List<StudyPropertyValueDTO> getSpvDtoList(){
         EntityPropertyDTO epDto1 = new EntityPropertyDTO(1, "Test");
         EntityPropertyDTO epDto2 = new EntityPropertyDTO(2, "Validate");
         StudyPropertyValueDTO spvDto1 = new StudyPropertyValueDTO(1, "test1", epDto1, null, false);
         StudyPropertyValueDTO spvDto2 = new StudyPropertyValueDTO(null, "test3", epDto1, null, false);
         StudyPropertyValueDTO spvDto3 = new StudyPropertyValueDTO(3, "validate3", epDto2, null, false);
         return List.of(spvDto1, spvDto2, spvDto3);
     }

     private List<StudyPropertyValueDTO> getSpvDtoListToDelete(){
         EntityPropertyDTO epDto1 = new EntityPropertyDTO(1, "Test");
         StudyPropertyValueDTO spvDto1 = new StudyPropertyValueDTO(1, "test1", epDto1, null, true);
         //ep not required to delete
         StudyPropertyValueDTO spvDto3 = new StudyPropertyValueDTO(3, "validate3", null, null, true);
         return List.of(spvDto1, spvDto3);
     }

     private List<LkupPropertyCodelistValue> getCodelistValues() {
         LkupPropertyCodelistValue codelistValue1 = new LkupPropertyCodelistValue();
         codelistValue1.setId(1);
         codelistValue1.setPropertyCodelistId(33);
         codelistValue1.setValue("test1");
         LkupPropertyCodelistValue codelistValue2 = new LkupPropertyCodelistValue();
         codelistValue2.setId(2);
         codelistValue2.setPropertyCodelistId(33);
         codelistValue2.setValue("test2");
         LkupPropertyCodelistValue codelistValue3 = new LkupPropertyCodelistValue();
         codelistValue3.setId(3);
         codelistValue3.setPropertyCodelistId(33);
         codelistValue3.setValue("test3");
         LkupPropertyCodelistValue codelistValue4 = new LkupPropertyCodelistValue();
         codelistValue4.setId(4);
         codelistValue4.setPropertyCodelistId(44);
         codelistValue4.setValue("validate1");
         LkupPropertyCodelistValue codelistValue5 = new LkupPropertyCodelistValue();
         codelistValue5.setId(5);
         codelistValue5.setPropertyCodelistId(44);
         codelistValue5.setValue("validate2");
         LkupPropertyCodelistValue codelistValue6 = new LkupPropertyCodelistValue();
         codelistValue6.setId(6);
         codelistValue6.setPropertyCodelistId(44);
         codelistValue6.setValue("validate3");
         return List.of(codelistValue1, codelistValue2, codelistValue3, codelistValue4, codelistValue5, codelistValue6);
     }

     @Test
     void testEditStudyPropertyValues_CuratorEditHappyPath(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));

         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId);

         assertEquals("Successfully updated property values", response);
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
     }

     @Test
     void testEditStudyPropertyValues_CuratorEditWithNullEntityProperty(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         //dto set up
         StudyPropertyValueDTO spvDto = new StudyPropertyValueDTO(1, "test", null, null, false);
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, List.of(spvDto));

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));

         assertThrows(StudyRegRequestException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId)
         );

     }

     @Test
     void testEditStudyPropertyValues_CuratorEditAndSubmitHappyPath(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         EntityProperty entityProperty3 = new EntityProperty();
         entityProperty3.setId(3);
         entityProperty3.setName("release_date");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         LkupStatus status = new LkupStatus();
         status.setId(1);
         status.setName("Approved");
         status.setUsage("study");

         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(entityPropertyRepository.findByName("release_date"))
                 .thenReturn(Optional.of(entityProperty3));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));
         when(statusRepository.findByUsageAndName("study", "Approved"))
                 .thenReturn(Optional.of(status));
         when(viewStudyRepository.findByStudyId(8))
                 .thenReturn(Optional.of(new ViewStudy()));

         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", true, userId);

         assertEquals("Successfully updated property values", response);
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(4)).save(any(StudyPropertyValue.class));
         assertEquals(status, study.getStatus());
     }

     @Test
     void testEditStudyPropertyValues_DccEditHappyPath(){
         //set up db mocks
         Integer userId = 2;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));

         String response = studyRegistrationService.editStudyPropertyValues(dto, "DCC", false, userId);

         assertEquals("Successfully updated property values", response);
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
     }

     @Test
     void testEditStudyPropertyValues_DccEditAndSubmitHappyPath(){
         //set up db mocks
         Integer userId = 2;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         LkupStatus status = new LkupStatus();
         status.setId(2);
         status.setName("In Review");
         status.setUsage("study");
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));
         when(statusRepository.findByUsageAndName("study", "In Review"))
                 .thenReturn(Optional.of(status));
         when(viewStudyRepository.findByStudyId(8))
                 .thenReturn(Optional.of(new ViewStudy()));

         String response = studyRegistrationService.editStudyPropertyValues(dto, "DCC", true, userId);

         assertEquals("Successfully updated property values", response);
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
         assertEquals(status, study.getStatus());
     }

     @Test
     void testEditStudyPropertyValues_CodelistedHappyPath(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setCodeListId(33);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setCodeListId(44);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("test2");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("validate2");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));
         when(codelistValueRepository.findAll())
                 .thenReturn(getCodelistValues());

         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId);

         assertEquals("Successfully updated property values", response);
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
         verify(codelistValueRepository, times(1)).findAll();
     }

     @Test
     void testEditStudyPropertyValues_CodelistedInvalidValue(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setCodeListId(33);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setCodeListId(44);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("test2");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("validate2");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);

         List<LkupPropertyCodelistValue> codelistValues = getCodelistValues();
         codelistValues.get(0).setValue("wrong");
         codelistValues.get(1).setValue("fail");
         codelistValues.get(2).setValue("bad");

         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));
         when(codelistValueRepository.findAll())
                 .thenReturn(codelistValues);

         assertThrows(BadDataException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId));
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(codelistValueRepository, times(1)).findAll();
     }

     @Test
     void testEditStudyPropertyValues_DeleteHappyPath(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoListToDelete());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));


         String response = studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId);

         assertEquals("Successfully updated property values", response);
         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(1)).deleteAllById(List.of(1, 3));
     }

     @Test
     void testEditStudyPropertyValues_StudyNotFound(){
         Integer userId = 2;
         when(studyRepository.findById(8))
                 .thenReturn(Optional.empty());
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         assertThrows(StudyNotFoundException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "DCC", false, userId));
     }

     @Test
     void testEditStudyPropertyValues_InvalidRole(){
         Integer userId = 3;
         Study study = new Study();
         study.setId(8);
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));

         assertThrows(BadDataException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Fail", false, userId));
     }

     @Test
     void testEditStudyPropertyValues_DccTryingToEditCuratorFields(){
         //set up db mocks
         Integer userId = 2;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty2));

         assertThrows(UserAuthorizationException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "DCC", false, userId));

         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
     }

     @Test
     void testEditStudyPropertyValues_CuratorStudyStatusNotFound(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         LkupStatus status = new LkupStatus();
         status.setId(1);
         status.setName("Approved");
         status.setUsage("study");

         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));
         when(statusRepository.findByUsageAndName("study", "approved_study"))
                 .thenThrow(new StatusNotFoundException("Invalid Data File Status: approved_study"));

         assertThrows(StatusNotFoundException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", true, userId));

         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
     }

     @Test
     void testEditStudyPropertyValues_DccStudyStatusNotFound(){
         //set up db mocks
         Integer userId = 2;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         LkupStatus status = new LkupStatus();
         status.setId(2);
         status.setName("In Review");
         status.setUsage("study");
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.of(spv1));
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.of(spv3));
         when(statusRepository.findByUsageAndName("study", "In Review Status"))
                 .thenThrow(new StatusNotFoundException("status invalid"));

         assertThrows(StatusNotFoundException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "DCC", true, userId));

         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository, times(2)).findById(anyInt());
         verify(studyPropertyValueRepository, times(3)).save(any(StudyPropertyValue.class));
     }

     @Test
     void testEditStudyPropertyValues_InvalidStudyPropertyValueId(){
         //set up db mocks
         Integer userId = 1;
         Integer studyId = 8;
         Study study = new Study();
         study.setId(studyId);
         LkupPropertySource propertySource1 = new LkupPropertySource();
         propertySource1.setId(1);
         propertySource1.setName("dbGaP/MTA");
         LkupPropertySource propertySource2 = new LkupPropertySource();
         propertySource2.setId(2);
         propertySource2.setName("Hub Online Submission");
         EntityProperty entityProperty1 = new EntityProperty();
         entityProperty1.setId(1);
         entityProperty1.setName("Test");
         EntityProperty entityProperty2 = new EntityProperty();
         entityProperty2.setId(2);
         entityProperty2.setName("Validate");

         StudyPropertyValue spv1 = new StudyPropertyValue();
         spv1.setId(1);
         spv1.setPropertyValue("temp1");
         spv1.setStudyId(8);
         spv1.setEntityProperty(entityProperty1);
         StudyPropertyValue spv3 = new StudyPropertyValue();
         spv3.setId(3);
         spv3.setPropertyValue("temp3");
         spv3.setStudyId(8);
         spv3.setEntityProperty(entityProperty2);
         //dto set up
         StudyRegistrationDTO dto = new StudyRegistrationDTO(8, getSpvDtoList());

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(study));
         when(propertySourceRepository.findAllByNameIn(anyList()))
                 .thenReturn(List.of(propertySource1, propertySource2));
         when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                 .thenReturn(List.of(entityProperty1, entityProperty2));
         when(studyPropertyValueRepository.findById(1))
                 .thenReturn(Optional.empty());
         when(studyPropertyValueRepository.findById(3))
                 .thenReturn(Optional.empty());

         assertThrows(StudyPropertyValuesRetrievalException.class,
                      () -> studyRegistrationService.editStudyPropertyValues(dto, "Curator", false, userId));

         verify(studyRepository, times(1)).findById(8);
         verify(propertySourceRepository, times(1)).findAllByNameIn(anyList());
         verify(entityPropertyRepository, times(1)).findAllByPropertySourceIdIn(anyList());
         verify(studyPropertyValueRepository).findById(anyInt());
     }

     @Test
     void testGetStudyProperties_HappyPath() {
         EntityProperty ep = new EntityProperty();
         ep.setId(123);
         ep.setName("Test");
         StudyPropertyValue spv = new StudyPropertyValue();
         spv.setId(1);
         spv.setStudyId(8);
         spv.setPropertyValue("test");
         spv.setEntityProperty(ep);

         when(studyRepository.findById(8))
                 .thenReturn(Optional.of(new Study()));
         when(studyPropertyValueRepository.findAllByStudyId(8))
                 .thenReturn(List.of(spv));

         StudyRegistrationDTO response = studyRegistrationService.getStudyProperties(8);

         assertEquals(8, response.studyId());
         assertEquals("test", response.studyPropertyValues().get(0).value());
         assertEquals(1, response.studyPropertyValues().get(0).id());
         assertEquals("Test", response.studyPropertyValues().get(0).entityProperty().name());
         assertEquals(123, response.studyPropertyValues().get(0).entityProperty().id());

         verify(studyPropertyValueMapper, times(1))
                 .entityListToDtoList(List.of(spv));
     }

    @Test
    void testGetUserStudiesByDcc() {
        Users users = new Users();
        LkupCenter dcc = new LkupCenter();
        dcc.setId(1);
        users.setCenter(dcc);

         ViewStudy viewStudy = new ViewStudy();
         viewStudy.setStudyId(1);
         List<ViewStudy> studies = List.of(viewStudy);

         LkupStatus pendingDCCStatus = new LkupStatus();
         int statusId = 1;
         pendingDCCStatus.setId(statusId);
         pendingDCCStatus.setName("Pending DCC Input");

         when(statusRepository.findByName("Pending DCC Input"))
                 .thenReturn(Optional.of(pendingDCCStatus));
         when(usersRepository.findById(1))
                 .thenReturn(Optional.of(users));
         when(viewStudyRepository.findCenterStudiesByStatus(1, "Pending DCC Input"))
                 .thenReturn(studies);

         List<UserStudyRegistrationDTO> resultDTOList = studyRegistrationService.getUserStudiesByCenter(1, "Pending DCC Input");

         // Verify the results
         Assertions.assertEquals(1, resultDTOList.get(0).getStudyId());
     }

     @Test
     void testGetUserStudiesByDcc_UserNullDcc() {
         when(usersRepository.findById(anyInt()))
                 .thenReturn(Optional.of(new Users()));
         Assertions.assertThrows(UserAuthorizationException.class, () -> {
             studyRegistrationService.getUserStudiesByCenter(1, "");
         });
     }

     @Test
     void testGetUserStudiesByCuratorNoStudies() {
         LkupStatus approvedStatus = new LkupStatus();
         int statusId = 1;
         approvedStatus.setId(statusId);
         approvedStatus.setName("Approved");
         when(statusRepository.findByName("Approved"))
                 .thenReturn(Optional.of(approvedStatus));
         when(viewStudyRepository.findCuratorStudiesByStatus("Approved"))
                 .thenReturn(new ArrayList<>(0));

         List<UserStudyRegistrationDTO> actualDTOs = studyRegistrationService.getUserStudiesByCurator("Approved");

         assertEquals(new ArrayList<>(0), actualDTOs);
     }

     @Test
     void testDeleteStudiesByCurator_SuccessfulDeletion() {
         // Arrange
         Integer studyId = 123;
         Study study = new Study();
         study.setId(studyId);
         study.setFileUrl("s3://bucket/objectKey");

         when(studyRepository.findById(eq(studyId)))
                 .thenReturn(Optional.of(study));
         when(storageService.deleteStudyFromS3(eq(study))).thenReturn(true);

         studyRegistrationService.deleteStudiesByCurator(studyId);

         // Assert
         verify(studyPropertyValueRepository).deleteAllByStudyId(eq(studyId));
         verify(studyRepository).delete(eq(study));
     }
    @Test
    void deleteStudiesByCurator_ShouldThrowFileDeletionException() {

        Integer studyId = 1;
        Study study = new Study();
        study.setId(studyId);
        study.setFileUrl("s3://url/test.txt");

        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(storageService.deleteStudyFromS3(eq(study)))
                .thenReturn(false);

        assertThrows(FileDeletionException.class, () -> studyRegistrationService.deleteStudiesByCurator(studyId));

        verify(studyPropertyValueRepository, never()).deleteAllByStudyId(studyId);
        verify(studyRepository, never()).delete(study);
    }

    // ========== registerNewStudy Tests ==========

    @Test
    void testRegisterNewStudy_CuratorNoSubmitHappyPath() {
        // Arrange
        Integer userId = 1;
        Integer studyId = 100;
        String role = "Curator";
        Boolean shouldSubmit = false;

        Study study = new Study();
        study.setId(studyId);
        study.setUuid(UUID.randomUUID().toString());

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");
        draftStatus.setUsage("study");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        LkupPropertySource propertySource1 = new LkupPropertySource();
        propertySource1.setId(1);
        propertySource1.setName("Hub Online Submission");

        EntityProperty entityProperty1 = new EntityProperty();
        entityProperty1.setId(1);
        entityProperty1.setName("Test");

        EntityProperty hasDataFilesProperty = new EntityProperty();
        hasDataFilesProperty.setId(10);
        hasDataFilesProperty.setName("has_data_files");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(List.of(propertySource1));
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(List.of(entityProperty1));
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.of(hasDataFilesProperty));
        when(codelistValueRepository.findAll())
                .thenReturn(getCodelistValues());

        // Act
        Map<String, Integer> result = studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit);

        // Assert
        assertNotNull(result);
        assertEquals(studyId, result.get("studyId"));
        verify(statusRepository).findByUsageAndName("study", "Draft");
        verify(studyRepository).saveAndFlush(any(Study.class));
        verify(emailRequestService).sendStudyRegEmail(eq(studyId), any());
        verify(studyPropertyValueRepository, atLeastOnce()).save(any(StudyPropertyValue.class));
    }

    @Test
    void testRegisterNewStudy_CuratorWithSubmitHappyPath() {
        // Arrange
        Integer userId = 1;
        Integer studyId = 100;
        String role = "Curator";
        Boolean shouldSubmit = true;

        Study study = new Study();
        study.setId(studyId);
        study.setUuid(UUID.randomUUID().toString());

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");
        draftStatus.setUsage("study");

        LkupStatus approvedStatus = new LkupStatus();
        approvedStatus.setId(2);
        approvedStatus.setName("Approved");
        approvedStatus.setUsage("study");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        LkupPropertySource propertySource1 = new LkupPropertySource();
        propertySource1.setId(1);
        propertySource1.setName("Hub Online Submission");

        EntityProperty entityProperty1 = new EntityProperty();
        entityProperty1.setId(1);
        entityProperty1.setName("Test");

        EntityProperty hasDataFilesProperty = new EntityProperty();
        hasDataFilesProperty.setId(10);
        hasDataFilesProperty.setName("has_data_files");

        EntityProperty releaseDateProperty = new EntityProperty();
        releaseDateProperty.setId(11);
        releaseDateProperty.setName("release_date");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(statusRepository.findByUsageAndName("study", "Approved"))
                .thenReturn(Optional.of(approvedStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(List.of(propertySource1));
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(List.of(entityProperty1));
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.of(hasDataFilesProperty));
        when(entityPropertyRepository.findByName("release_date"))
                .thenReturn(Optional.of(releaseDateProperty));
        when(codelistValueRepository.findAll())
                .thenReturn(getCodelistValues());
        when(viewStudyRepository.findByStudyId(studyId))
                .thenReturn(Optional.of(new ViewStudy()));

        // Act
        Map<String, Integer> result = studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit);

        // Assert
        assertNotNull(result);
        assertEquals(studyId, result.get("studyId"));
        verify(statusRepository).findByUsageAndName("study", "Draft");
        verify(statusRepository).findByUsageAndName("study", "Approved");
        verify(studyRepository, times(2)).save(any(Study.class));
        verify(emailRequestService).sendStudyRegEmail(eq(studyId), any());
        assertEquals(approvedStatus, study.getStatus());
    }

    @Test
    void testRegisterNewStudy_DccWithSubmitHappyPath() {
        // Arrange
        Integer userId = 2;
        Integer studyId = 100;
        String role = "DCC";
        Boolean shouldSubmit = true;

        Study study = new Study();
        study.setId(studyId);
        study.setUuid(UUID.randomUUID().toString());

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");
        draftStatus.setUsage("study");

        LkupStatus inReviewStatus = new LkupStatus();
        inReviewStatus.setId(3);
        inReviewStatus.setName("In Review");
        inReviewStatus.setUsage("study");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        LkupPropertySource propertySource1 = new LkupPropertySource();
        propertySource1.setId(1);
        propertySource1.setName("Hub Online Submission");

        EntityProperty entityProperty1 = new EntityProperty();
        entityProperty1.setId(1);
        entityProperty1.setName("Test");

        EntityProperty hasDataFilesProperty = new EntityProperty();
        hasDataFilesProperty.setId(10);
        hasDataFilesProperty.setName("has_data_files");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(statusRepository.findByUsageAndName("study", "In Review"))
                .thenReturn(Optional.of(inReviewStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(List.of(propertySource1));
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(List.of(entityProperty1));
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.of(hasDataFilesProperty));
        when(codelistValueRepository.findAll())
                .thenReturn(getCodelistValues());
        when(viewStudyRepository.findByStudyId(studyId))
                .thenReturn(Optional.of(new ViewStudy()));

        // Act
        Map<String, Integer> result = studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit);

        // Assert
        assertNotNull(result);
        assertEquals(studyId, result.get("studyId"));
        verify(statusRepository).findByUsageAndName("study", "In Review");
        assertEquals(inReviewStatus, study.getStatus());
    }

    @Test
    void testRegisterNewStudy_EmptyPropertyValues() {
        // Arrange
        Integer userId = 1;
        Integer studyId = 100;
        String role = "Curator";
        Boolean shouldSubmit = false;

        Study study = new Study();
        study.setId(studyId);

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");
        draftStatus.setUsage("study");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        EntityProperty hasDataFilesProperty = new EntityProperty();
        hasDataFilesProperty.setId(10);
        hasDataFilesProperty.setName("has_data_files");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, Collections.emptyList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.of(hasDataFilesProperty));

        // Act
        Map<String, Integer> result = studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit);

        // Assert
        assertNotNull(result);
        assertEquals(studyId, result.get("studyId"));
        verify(studyRepository).saveAndFlush(any(Study.class));
    }

    @Test
    void testRegisterNewStudy_DraftStatusNotFound() {
        // Arrange
        Integer userId = 1;
        String role = "Curator";
        Boolean shouldSubmit = false;

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(StatusNotFoundException.class,
                () -> studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit));

        verify(studyRepository, never()).saveAndFlush(any(Study.class));
        verify(emailRequestService, never()).sendStudyRegEmail(anyInt(), any());
    }

    @Test
    void testRegisterNewStudy_CenterNotFound() {
        // Arrange
        Integer userId = 1;
        String role = "Curator";
        Boolean shouldSubmit = false;

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class,
                () -> studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit));
    }

    @Test
    void testRegisterNewStudy_ApprovedStatusNotFoundWhenSubmitting() {
        // Arrange
        Integer userId = 1;
        Integer studyId = 100;
        String role = "Curator";
        Boolean shouldSubmit = true;

        Study study = new Study();
        study.setId(studyId);

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        EntityProperty hasDataFilesProperty = new EntityProperty();
        hasDataFilesProperty.setId(10);
        hasDataFilesProperty.setName("has_data_files");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(statusRepository.findByUsageAndName("study", "Approved"))
                .thenReturn(Optional.empty());
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.of(hasDataFilesProperty));

        // Act & Assert
        assertThrows(StatusNotFoundException.class,
                () -> studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit));

        verify(studyRepository).saveAndFlush(any(Study.class));
    }

    @Test
    void testRegisterNewStudy_HasDataFilesPropertyNotFound() {
        // Arrange
        Integer userId = 1;
        Integer studyId = 100;
        String role = "Curator";
        Boolean shouldSubmit = false;

        Study study = new Study();
        study.setId(studyId);

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CategoryNotFoundException.class,
                () -> studyRegistrationService.registerNewStudy(dto, role, userId, shouldSubmit));
    }

    @Test
    void testRegisterNewStudy_NullUserId() {
        // Arrange
        Integer studyId = 100;
        String role = "Curator";
        Boolean shouldSubmit = false;

        Study study = new Study();
        study.setId(studyId);
        study.setCreatedBy(null);

        LkupStatus draftStatus = new LkupStatus();
        draftStatus.setId(1);
        draftStatus.setName("Draft");

        LkupCenter center = new LkupCenter();
        center.setId(1);
        center.setName("RADx-UP");

        EntityProperty hasDataFilesProperty = new EntityProperty();
        hasDataFilesProperty.setId(10);
        hasDataFilesProperty.setName("has_data_files");

        StudyRegistrationDTO dto = new StudyRegistrationDTO(null, getSpvDtoList());

        when(statusRepository.findByUsageAndName("study", "Draft"))
                .thenReturn(Optional.of(draftStatus));
        when(centerRepository.findByNameContainingIgnoreCase("RADx-UP"))
                .thenReturn(Optional.of(center));
        when(studyRepository.saveAndFlush(any(Study.class)))
                .thenReturn(study);
        when(studyRepository.findById(studyId))
                .thenReturn(Optional.of(study));
        when(propertySourceRepository.findAllByNameIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findAllByPropertySourceIdIn(anyList()))
                .thenReturn(new ArrayList<>());
        when(entityPropertyRepository.findByName("has_data_files"))
                .thenReturn(Optional.of(hasDataFilesProperty));

        // Act
        Map<String, Integer> result = studyRegistrationService.registerNewStudy(dto, role, null, shouldSubmit);

        // Assert
        assertNotNull(result);
        assertEquals(studyId, result.get("studyId"));
    }

}
