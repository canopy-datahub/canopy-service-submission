package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataFile;
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
public class UploadMapperImpl implements UploadMapper {

    @Override
    public S3FileDTO toS3FileDto(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }

        S3FileDTO s3FileDTO = new S3FileDTO();

        s3FileDTO.setFileName( dataFileS3FileFileName( dataFile ) );
        s3FileDTO.setChecksumHash( dataFileS3FileChecksumHash( dataFile ) );
        s3FileDTO.setDataFileId( dataFile.getId() );
        s3FileDTO.setFileSize( dataFile.getFileSize() );

        return s3FileDTO;
    }

    private String dataFileS3FileFileName(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }
        S3File s3File = dataFile.getS3File();
        if ( s3File == null ) {
            return null;
        }
        String fileName = s3File.getFileName();
        if ( fileName == null ) {
            return null;
        }
        return fileName;
    }

    private String dataFileS3FileChecksumHash(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }
        S3File s3File = dataFile.getS3File();
        if ( s3File == null ) {
            return null;
        }
        String checksumHash = s3File.getChecksumHash();
        if ( checksumHash == null ) {
            return null;
        }
        return checksumHash;
    }
}
