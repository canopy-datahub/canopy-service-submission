package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.dtos.S3FileDTO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-09T16:48:24-0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.50.v20250628-1110, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class S3FileMapperImpl implements S3FileMapper {

    @Override
    public S3FileDTO s3FileToDTO(S3File file) {
        if ( file == null ) {
            return null;
        }

        S3FileDTO s3FileDTO = new S3FileDTO();

        s3FileDTO.setChecksumHash( file.getChecksumHash() );
        s3FileDTO.setFileName( file.getFileName() );
        s3FileDTO.setFileSize( file.getFileSize() );
        s3FileDTO.setUploadErrorDescription( file.getUploadErrorDescription() );
        s3FileDTO.setUploadSuccessful( file.getUploadSuccessful() );

        return s3FileDTO;
    }
}
