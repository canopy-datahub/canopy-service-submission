package ex.org.project.submissionService.mappers;

import java.util.List;

import ex.org.project.submissionService.models.dtos.BundleFileDTO;
import ex.org.project.submissionService.models.dtos.BundlesDTO;
import ex.org.project.submissionService.models.dtos.DataFileDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.DataFileCategory;


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
