package ex.org.project.submissionService.models;

public interface DataFileIds {
    Integer getId();
    Integer getDictionaryFileId();
    Integer getMetadataFileId();
    void setId(Integer dataFileId);
    void setDictionaryFileId(Integer dataFileId);
    void setMetadataFileId(Integer dataFileId);
}
