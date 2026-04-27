package org.canopyplatform.canopy.submissionservice.models.dtos;

public record StudyPropertyValueDTO(
        Integer id,
        String value,
        //having entity property on this dto is 50% slower since a bunch of joins need to be made
        EntityPropertyDTO entityProperty,
        Integer valueIndex,
        Boolean shouldBeRemoved)
{}
