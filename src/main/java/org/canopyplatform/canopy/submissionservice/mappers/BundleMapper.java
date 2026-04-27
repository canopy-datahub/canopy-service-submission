package org.canopyplatform.canopy.submissionservice.mappers;

import org.canopyplatform.canopy.submissionservice.models.dtos.BundleDTO;
import org.canopyplatform.canopy.submissionservice.models.DataFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;



@Mapper(componentModel = "spring")
public interface BundleMapper {

    @Mapping(source = "dataFile.id", target = "id")
    @Mapping(source = "dataFile.sourceFileName", target = "name")
    @Mapping(source = "dataFile.fileCategory.name", target = "category")
    @Mapping(source = "dataFile.fileSize", target = "size")
    @Mapping(source = "dataFile.cdeValidationFailed", target = "cdeValidation")
    @Mapping(source = "dataFile.validationAcknowledged", target = "acknowledged")
    @Mapping(source = "dataFile.piiPhiFailed", target = "piiPhiValidation")
    BundleDTO toBundleDto(DataFile dataFile);

    List<BundleDTO> toBundleDTOs(List<DataFile> dataFiles);
}

