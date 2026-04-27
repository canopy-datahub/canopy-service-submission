package org.canopyplatform.canopy.submissionservice.mappers;

import java.util.List;

import org.canopyplatform.canopy.submissionservice.models.dtos.BundleFileDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.BundlesDTO;
import org.canopyplatform.canopy.submissionservice.models.dtos.DataFileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.canopyplatform.canopy.submissionservice.models.DataFile;
import org.canopyplatform.canopy.submissionservice.models.DataFileCategory;


@Mapper(componentModel = "spring")
public interface DataFileMapper {

    @Mapping(source = "fileCategory.name", target = "fileCategory")
    List<DataFileDTO> toDTOs(List<DataFile> dataFiles);

    default String map(DataFileCategory dataFileCategory) {
        return dataFileCategory.getName();
    }

    @Mapping(target = "piiPhiFailed", source = "dataFile.piiPhiFailed")
    @Mapping(target = "cdeFailed", source = "dataFile.cdeValidationFailed")
    @Mapping(source = "fileCategory.name", target = "fileCategory")
    BundlesDTO mapToBundleFileDTO(DataFile dataFile);

    List<BundlesDTO> mapToBundleDTOList(List<DataFile> dataFiles);

    List<BundleFileDTO> mapToBundleFileDTOList(List<DataFile> dataFiles);

    @Mapping(source = "fileCategory.name", target = "dataFileCategory")
    BundleFileDTO toBundleFileDTO(DataFile dataFile);
}
