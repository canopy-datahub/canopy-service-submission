package org.canopyplatform.canopy.submissionservice.models;

public interface DataFileIds {
    Integer getId();
    Integer getDictionaryFileId();
    Integer getMetadataFileId();
    void setId(Integer dataFileId);
    void setDictionaryFileId(Integer dataFileId);
    void setMetadataFileId(Integer dataFileId);
}
