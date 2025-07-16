package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.UserFileUpload;
import ex.org.project.submissionService.models.Users;
import ex.org.project.submissionService.models.ViewStudy;
import ex.org.project.submissionService.models.dtos.UploadPortalCuratorDashboardDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-15T14:01:27-0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.14 (Oracle Corporation)"
)
@Component
public class UserFileUploadMapperImpl implements UserFileUploadMapper {

    @Override
    public UploadPortalCuratorDashboardDTO mapToDto(UserFileUpload userFileUpload) {
        if ( userFileUpload == null ) {
            return null;
        }

        UploadPortalCuratorDashboardDTO uploadPortalCuratorDashboardDTO = new UploadPortalCuratorDashboardDTO();

        uploadPortalCuratorDashboardDTO.setUploadBy( userFileUploadUploadUserFullName( userFileUpload ) );
        uploadPortalCuratorDashboardDTO.setStudy( userFileUploadViewStudyPhsTitle( userFileUpload ) );
        uploadPortalCuratorDashboardDTO.setIsDeleted( UserFileUploadMapper.isDeleted( userFileUpload.getS3File() ) );
        uploadPortalCuratorDashboardDTO.setId( userFileUpload.getId() );
        uploadPortalCuratorDashboardDTO.setUploadAt( userFileUpload.getUploadAt() );
        uploadPortalCuratorDashboardDTO.setDownloadAt( userFileUpload.getDownloadAt() );
        uploadPortalCuratorDashboardDTO.setDeleteAt( userFileUpload.getDeleteAt() );

        return uploadPortalCuratorDashboardDTO;
    }

    @Override
    public List<UploadPortalCuratorDashboardDTO> mapToDtoList(List<UserFileUpload> userFileUploads) {
        if ( userFileUploads == null ) {
            return null;
        }

        List<UploadPortalCuratorDashboardDTO> list = new ArrayList<UploadPortalCuratorDashboardDTO>( userFileUploads.size() );
        for ( UserFileUpload userFileUpload : userFileUploads ) {
            list.add( mapToDto( userFileUpload ) );
        }

        return list;
    }

    private String userFileUploadUploadUserFullName(UserFileUpload userFileUpload) {
        if ( userFileUpload == null ) {
            return null;
        }
        Users uploadUser = userFileUpload.getUploadUser();
        if ( uploadUser == null ) {
            return null;
        }
        String fullName = uploadUser.getFullName();
        if ( fullName == null ) {
            return null;
        }
        return fullName;
    }

    private String userFileUploadViewStudyPhsTitle(UserFileUpload userFileUpload) {
        if ( userFileUpload == null ) {
            return null;
        }
        ViewStudy viewStudy = userFileUpload.getViewStudy();
        if ( viewStudy == null ) {
            return null;
        }
        String phsTitle = viewStudy.getPhsTitle();
        if ( phsTitle == null ) {
            return null;
        }
        return phsTitle;
    }
}
