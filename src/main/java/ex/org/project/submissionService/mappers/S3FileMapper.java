package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.S3File;
import ex.org.project.submissionService.models.dtos.S3FileDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface S3FileMapper {

    S3FileDTO s3FileToDTO(S3File file);
}
