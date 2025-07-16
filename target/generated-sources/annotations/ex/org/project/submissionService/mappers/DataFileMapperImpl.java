package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.DataFileCategory;
import ex.org.project.submissionService.models.dtos.BundleFileDTO;
import ex.org.project.submissionService.models.dtos.BundlesDTO;
import ex.org.project.submissionService.models.dtos.DataFileDTO;
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
public class DataFileMapperImpl implements DataFileMapper {

    @Override
    public List<DataFileDTO> toDTOs(List<DataFile> dataFiles) {
        if ( dataFiles == null ) {
            return null;
        }

        List<DataFileDTO> list = new ArrayList<DataFileDTO>( dataFiles.size() );
        for ( DataFile dataFile : dataFiles ) {
            list.add( dataFileToDataFileDTO( dataFile ) );
        }

        return list;
    }

    @Override
    public BundlesDTO mapToBundleFileDTO(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }

        BundlesDTO bundlesDTO = new BundlesDTO();

        bundlesDTO.setPiiPhiFailed( dataFile.getPiiPhiFailed() );
        bundlesDTO.setCdeFailed( dataFile.getCdeValidationFailed() );
        bundlesDTO.setFileCategory( dataFileFileCategoryName( dataFile ) );
        bundlesDTO.setId( dataFile.getId() );
        bundlesDTO.setSourceFileName( dataFile.getSourceFileName() );
        bundlesDTO.setVersionNumber( dataFile.getVersionNumber() );

        return bundlesDTO;
    }

    @Override
    public List<BundlesDTO> mapToBundleDTOList(List<DataFile> dataFiles) {
        if ( dataFiles == null ) {
            return null;
        }

        List<BundlesDTO> list = new ArrayList<BundlesDTO>( dataFiles.size() );
        for ( DataFile dataFile : dataFiles ) {
            list.add( mapToBundleFileDTO( dataFile ) );
        }

        return list;
    }

    @Override
    public List<BundleFileDTO> mapToBundleFileDTOList(List<DataFile> dataFiles) {
        if ( dataFiles == null ) {
            return null;
        }

        List<BundleFileDTO> list = new ArrayList<BundleFileDTO>( dataFiles.size() );
        for ( DataFile dataFile : dataFiles ) {
            list.add( toBundleFileDTO( dataFile ) );
        }

        return list;
    }

    @Override
    public BundleFileDTO toBundleFileDTO(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }

        String dataFileCategory = null;
        String sourceFileName = null;

        dataFileCategory = dataFileFileCategoryName( dataFile );
        sourceFileName = dataFile.getSourceFileName();

        BundleFileDTO bundleFileDTO = new BundleFileDTO( sourceFileName, dataFileCategory );

        return bundleFileDTO;
    }

    protected DataFileDTO dataFileToDataFileDTO(DataFile dataFile) {
        if ( dataFile == null ) {
            return null;
        }

        DataFileDTO dataFileDTO = new DataFileDTO();

        dataFileDTO.setId( dataFile.getId() );
        dataFileDTO.setSubmissionId( dataFile.getSubmissionId() );
        dataFileDTO.setSourceFileName( dataFile.getSourceFileName() );
        dataFileDTO.setNormalizedFileName( dataFile.getNormalizedFileName() );
        dataFileDTO.setVersionNumber( dataFile.getVersionNumber() );
        dataFileDTO.setFileSize( dataFile.getFileSize() );
        dataFileDTO.setValidationAcknowledged( dataFile.getValidationAcknowledged() );
        dataFileDTO.setFileCategory( map( dataFile.getFileCategory() ) );

        return dataFileDTO;
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
