package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.DataFile;
import org.canopyplatform.canopy.submissionservice.models.dtos.S3FileDTO;
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
