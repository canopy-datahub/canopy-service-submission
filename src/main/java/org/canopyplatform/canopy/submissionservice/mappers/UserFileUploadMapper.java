package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.S3File;
import org.canopyplatform.canopy.submissionservice.models.UserFileUpload;
import org.canopyplatform.canopy.submissionservice.models.dtos.UploadPortalCuratorDashboardDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserFileUploadMapper {

    @Mapping(source = "userFileUpload.uploadUser.fullName", target = "uploadBy")
    @Mapping(source = "userFileUpload.viewStudy.studyName", target = "study")
    @Mapping(source = "userFileUpload.s3File", target = "isDeleted", qualifiedByName = "isDeleted")
    UploadPortalCuratorDashboardDTO mapToDto(UserFileUpload userFileUpload);

    List<UploadPortalCuratorDashboardDTO> mapToDtoList(List<UserFileUpload> userFileUploads);

    @Named("isDeleted")
    static Boolean isDeleted(S3File s3File) {
        return s3File == null;
    }
}
