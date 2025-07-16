package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.dtos.S3FileDTO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-15T14:01:27-0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.14 (Oracle Corporation)"
)
@Component
public class S3FileMapperImpl implements S3FileMapper {

    @Override
    public S3FileDTO s3FileToDTO(S3File file) {
        if ( file == null ) {
            return null;
        }

        S3FileDTO s3FileDTO = new S3FileDTO();

        s3FileDTO.setFileName( file.getFileName() );
        s3FileDTO.setChecksumHash( file.getChecksumHash() );
        s3FileDTO.setFileSize( file.getFileSize() );
        s3FileDTO.setUploadSuccessful( file.getUploadSuccessful() );
        s3FileDTO.setUploadErrorDescription( file.getUploadErrorDescription() );

        return s3FileDTO;
    }
}
