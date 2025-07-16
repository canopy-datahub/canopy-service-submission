package ex.org.project.submissionService.mappers;

import ex.org.project.submissionService.models.EntityProperty;
import ex.org.project.submissionService.models.StudyPropertyValue;
import ex.org.project.submissionService.models.dtos.EntityPropertyDTO;
import ex.org.project.submissionService.models.dtos.StudyPropertyValueDTO;
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
public class StudyPropertyValueMapperImpl implements StudyPropertyValueMapper {

    @Override
    public StudyPropertyValueDTO entityToDto(StudyPropertyValue spv) {
        if ( spv == null ) {
            return null;
        }

        String value = null;
        Integer id = null;
        EntityPropertyDTO entityProperty = null;
        Integer valueIndex = null;

        value = spv.getPropertyValue();
        id = spv.getId();
        entityProperty = entityPropertyToDto( spv.getEntityProperty() );
        valueIndex = spv.getValueIndex();

        Boolean shouldBeRemoved = false;

        StudyPropertyValueDTO studyPropertyValueDTO = new StudyPropertyValueDTO( id, value, entityProperty, valueIndex, shouldBeRemoved );

        return studyPropertyValueDTO;
    }

    @Override
    public StudyPropertyValue DtoToEntity(StudyPropertyValueDTO spvDto) {
        if ( spvDto == null ) {
            return null;
        }

        StudyPropertyValue studyPropertyValue = new StudyPropertyValue();

        studyPropertyValue.setPropertyValue( spvDto.value() );
        studyPropertyValue.setId( spvDto.id() );
        studyPropertyValue.setEntityProperty( dtoToEntityProperty( spvDto.entityProperty() ) );
        studyPropertyValue.setValueIndex( spvDto.valueIndex() );
        studyPropertyValue.setShouldBeRemoved( spvDto.shouldBeRemoved() );

        return studyPropertyValue;
    }

    @Override
    public EntityPropertyDTO entityPropertyToDto(EntityProperty entityProperty) {
        if ( entityProperty == null ) {
            return null;
        }

        Integer id = null;
        String name = null;

        id = entityProperty.getId();
        name = entityProperty.getName();

        EntityPropertyDTO entityPropertyDTO = new EntityPropertyDTO( id, name );

        return entityPropertyDTO;
    }

    @Override
    public EntityProperty dtoToEntityProperty(EntityPropertyDTO entityPropertyDto) {
        if ( entityPropertyDto == null ) {
            return null;
        }

        EntityProperty entityProperty = new EntityProperty();

        entityProperty.setId( entityPropertyDto.id() );
        entityProperty.setName( entityPropertyDto.name() );

        return entityProperty;
    }

    @Override
    public List<StudyPropertyValueDTO> entityListToDtoList(List<StudyPropertyValue> spvList) {
        if ( spvList == null ) {
            return null;
        }

        List<StudyPropertyValueDTO> list = new ArrayList<StudyPropertyValueDTO>( spvList.size() );
        for ( StudyPropertyValue studyPropertyValue : spvList ) {
            list.add( entityToDto( studyPropertyValue ) );
        }

        return list;
    }

    @Override
    public List<StudyPropertyValue> dtoListToEntityList(List<StudyPropertyValueDTO> dtoList) {
        if ( dtoList == null ) {
            return null;
        }

        List<StudyPropertyValue> list = new ArrayList<StudyPropertyValue>( dtoList.size() );
        for ( StudyPropertyValueDTO studyPropertyValueDTO : dtoList ) {
            list.add( DtoToEntity( studyPropertyValueDTO ) );
        }

        return list;
    }
}
