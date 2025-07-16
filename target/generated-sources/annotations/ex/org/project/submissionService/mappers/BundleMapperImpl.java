package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.DataFileCategory;
import ex.org.project.submissionService.models.dtos.BundleDTO;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-07-15T14:01:27-0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.14 (Oracle Corporation)"
)
@Component
public class BundleMapperImpl implements BundleMapper {

    @Override
    public BundleDTO toBundleDto(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }

        BundleDTO bundleDTO = new BundleDTO();

        bundleDTO.setId( dataFile.getId() );
        bundleDTO.setName( dataFile.getSourceFileName() );
        bundleDTO.setCategory( dataFileFileCategoryName( dataFile ) );
        bundleDTO.setSize( dataFile.getFileSize() );
        bundleDTO.setCdeValidation( dataFile.getCdeValidationFailed() );
        bundleDTO.setAcknowledged( dataFile.getValidationAcknowledged() );
        bundleDTO.setPiiPhiValidation( dataFile.getPiiPhiFailed() );
        bundleDTO.setWillBeVersioned( dataFile.getWillBeVersioned() );

        return bundleDTO;
    }

    @Override
    public List<BundleDTO> toBundleDTOs(List<DataFile> dataFiles) {
        if ( dataFiles == null ) {
            return null;
        }

        List<BundleDTO> list = new ArrayList<BundleDTO>( dataFiles.size() );
        for ( DataFile dataFile : dataFiles ) {
            list.add( toBundleDto( dataFile ) );
        }

        return list;
    }

    private String dataFileFileCategoryName(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }
        DataFileCategory fileCategory = dataFile.getFileCategory();
        if ( fileCategory == null ) {
            return null;
        }
        String name = fileCategory.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
