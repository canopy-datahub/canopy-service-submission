package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.dtos.BundleDTO;
import ex.org.project.submissionService.models.DataFile;
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

