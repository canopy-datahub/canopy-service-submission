package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.S3File;
import org.canopyplatform.canopy.submissionservice.models.dtos.S3FileDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface S3FileMapper {

    S3FileDTO s3FileToDTO(S3File file);
}
