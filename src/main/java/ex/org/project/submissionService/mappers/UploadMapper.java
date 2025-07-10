package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.dtos.S3FileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UploadMapper {
    UploadMapper INSTANCE = Mappers.getMapper(UploadMapper.class);

    @Mapping(source = "s3File.fileName", target = "fileName")
    @Mapping(source = "s3File.checksumHash", target = "checksumHash")
    @Mapping(source="dataFile.id",target="dataFileId")

    S3FileDTO toS3FileDto(DataFile dataFile);
}
