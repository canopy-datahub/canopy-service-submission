
package ex.org.project.submissionService.repositories;

import ex.org.project.submissionService.models.DataFile;
import ex.org.project.submissionService.models.DataFileIds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DataFileRepository extends JpaRepository<DataFile, Integer> {

	List<DataFile> findDataFilesByFileCategory_CategoryGroupAndSubmissionId(String categoryGroup, Integer submissionId);

	DataFile findDataFileByFileCategory_CategoryGroupAndNormalizedFileNameAndSubmissionId(String categoryGroup,	String normalizedName, Integer submissionId);

	List<DataFile> findBySubmissionId(Integer Id);

	List<DataFile> findDataFilesByFileCategory_NameAndSubmissionId(String category, Integer submissionId);

	List<DataFile> findDataFilesByDictionaryFileId(Integer dictFileId);

	List<DataFile> findDataFilesByMetadataFileId(Integer metaFileId);

	List<DataFile> findDataFilesByOriginalDataFile_Id(Integer originalFileId);

	List<DataFileIds> findDataFileByIdInOrDictionaryFileIdInOrMetadataFileIdIn(Set<Integer> fileIds1, Set<Integer> fileIds2, Set<Integer> fileIds3);

	Optional<DataFileIds> findDistinctById(Integer fileId);

	@Modifying(flushAutomatically = true)
	@Query(
			value = "update data_file df set dictionary_file_id = null, metadata_file_id = null where df.id in ?1",
			nativeQuery = true
	)
	int updateForeignKeysToNull(Set<Integer> fileIds);

	Optional<DataFile> findBySourceFileNameAndSubmissionId(String fileName, Integer submissionId);

	@Query(nativeQuery = true,
			value="select df.* from data_file df where df.submission_id=?1 and (df.cde_validation is True or df.dict_validation is True or df.meta_validation is True or df.pii_phi is True)"
	)
	List<DataFile> getDataFilesThatFailedValidation(Integer submissionId);

	List<DataFile> findDataFilesBySubmissionId(Integer submissionId);

	List<DataFile> findByIdIn(List<Integer> bundleIds);

	@Query(nativeQuery = true,
			value="select df.* from data_file df join data_submission ds on df.submission_id=ds.id " +
					"where ds.study_id=:studyId"
	)
	List<DataFile> findDataFilesByStudyId(Integer studyId);

	@Query(nativeQuery = true,
			value="select df.* from data_file df join data_submission ds on df.submission_id=ds.id " +
					"where df.source_file_name=:fileName and df.is_current_version=true and ds.study_id=:studyId"
	)
	Optional<DataFile> findPreviousVersionByNameAndStudyId(String fileName, Integer studyId);

	@Query(nativeQuery = true,
			value="select exists(select df.* from data_file df join data_submission ds on df.submission_id=ds.id " +
					"where df.source_file_name=:fileName and ds.study_id=:studyId and df.is_current_version=true)"
	)
	Boolean existsBySourceFileNameAndStudyId(String fileName, Integer studyId);

	DataFile findDataFileByS3FileId(Integer s3FileId);

	Integer countAllBySubmissionId(Integer submissionId);

	Boolean existsBySubmissionIdAndPiiPhiFailedIsNullAndFileCategory_CategoryGroup(Integer submissionId, String categoryGroup);

	List<DataFile> findDataFilesByFileCategory_CategoryGroupAndSubmissionIdAndPiiPhiFailedIsNull(String categoryGroup, Integer submissionId);
}
