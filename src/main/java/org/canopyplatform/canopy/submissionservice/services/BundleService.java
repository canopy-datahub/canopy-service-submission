package org.canopyplatform.canopy.submissionservice.services;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.SubmissionIdInvalidException;
import org.canopyplatform.canopy.submissionservice.mappers.DataFileMapper;
import org.canopyplatform.canopy.submissionservice.mappers.SubmissionStepMapper;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.models.dtos.*;
import org.canopyplatform.canopy.submissionservice.repositories.*;
import org.canopyplatform.canopy.submissionservice.utils.BundlingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.canopyplatform.canopy.submissionservice.mappers.BundleMapper;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.BadDataException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.CategoryNotFoundException;
import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
@Service
public class BundleService {
    private final DataFileRepository dataFileRepository;
	private final DataFileService dataFileService;
	private final DataFileMapper dataFileMapper;
    private final DataFileCategoryRepository dataFileCategoryRepository;
    private final BundleMapper bundleMapper;
	private final DataSubmissionRepository dataSubmissionRepository;
	private final SubmissionStepMapper submissionStepMapper;
	private final LkupSubmissionStepRepository lkupSubmissionStepRepository;
	private final StudyRepository studyRepository;

    /**
     * This function creates bundles in a submission
     * @param submissionId is the submissionId
     * @return map object representing the bundling results
     */
	public boolean createBundles(Integer submissionId) {
		//check if data submission id is valid
		dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
		List<DataFile> dataFiles = dataFileRepository.findDataFilesByFileCategory_CategoryGroupAndSubmissionId(Constants.CATEGORY_DATA, submissionId);
		for (DataFile dataFile : dataFiles) {
			setChildReferences(dataFile, submissionId);
		}
		return true;
	}

    /**
     * This function finds child files and update their references in the datafile table
     * @param dataFile  is the main datafile of the bundle
     * @param submissionId is the submissionId
     */
    public void setChildReferences(DataFile dataFile, Integer submissionId){
        DataFile associatedDictionaryFile = dataFileRepository.findDataFileByFileCategory_CategoryGroupAndNormalizedFileNameAndSubmissionId(Constants.CATEGORY_DICT, dataFile.getNormalizedFileName(),
				submissionId);
        DataFile associatedMetadataFile = dataFileRepository.findDataFileByFileCategory_CategoryGroupAndNormalizedFileNameAndSubmissionId(Constants.CATEGORY_META, dataFile.getNormalizedFileName(),
				submissionId);

		if (associatedDictionaryFile != null) {
			dataFile.setDictionaryFileId(associatedDictionaryFile.getId());
		}
		if (associatedMetadataFile != null) {
			dataFile.setMetadataFileId(associatedMetadataFile.getId());
		}

        dataFileRepository.save(dataFile);
    }

    /**
     * getBundles gets all bundles in a submission
     * @param submissionId is the submissionId
     * @return map object representing the bundling results
     */
	@Transactional(readOnly = true)
    public SubmissionBundlesDTO getBundles(Integer submissionId){
		//check if submission id is valid
		dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        //get all data files in the submission
		List<DataFile> dataFiles = dataFileRepository.findBySubmissionId(submissionId);
		List<DataFile> parentDataFiles = BundlingUtils.popParentFilesFromList(dataFiles);
		List<DataFile> documentFiles = BundlingUtils.popDocumentsFromList(dataFiles);
		checkIfFilesWillBeVersioned(parentDataFiles, submissionId);
		// create a list of bundleDTOs from data files in submission, update reference files if exist, and remove from childFiles Set
		Map<Integer, List<Integer>> bundleIdMapping = BundlingUtils.getBundleIdMapping(parentDataFiles);
		List<BundleDTO> bundlesDTOs = bundleMapper.toBundleDTOs(parentDataFiles);
		for(BundleDTO bundle : bundlesDTOs){
			List<DataFile> childFiles = BundlingUtils.popChildrenFromList(bundle.getId(), bundleIdMapping, dataFiles);
			updateBundleChildren(bundle, childFiles);
		}
		SubmissionBundlesDTO bundles = new SubmissionBundlesDTO();
		bundles.setSubmissionId(submissionId);
		bundles.setBundles(bundlesDTOs);
		bundles.setDocuments(bundleMapper.toBundleDTOs(documentFiles));
		//unassigned are any that were not a parent data file, a document, or a file attached to a data file
		bundles.setUnassigned(bundleMapper.toBundleDTOs(dataFiles));
		return bundles;
    }

	protected void checkIfFilesWillBeVersioned(List<DataFile> parentDataFiles, Integer submissionId) {
		DataSubmission submission = dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
		int studyId = submission.getStudyId();
		for(DataFile dataFile : parentDataFiles) {
			boolean exists = dataFileRepository.existsBySourceFileNameAndStudyId(dataFile.getSourceFileName(), studyId);
			dataFile.setWillBeVersioned(exists);
		}
	}

	public void updateBundleChildren(BundleDTO bundle, List<DataFile> childFiles) {
		List<BundleDTO> bundleChildFiles = bundleMapper.toBundleDTOs(childFiles);
		bundle.setChildFiles(bundleChildFiles);
	}

    /**
     Updates the categories of a post based on the information provided in the DTO.
     @param dto The DTO containing the updated category information.
     */
    @Transactional
	public void updateBundles(SubmissionBundlesDTO dto) {
		List<DataFile> submissionFiles = dataFileRepository.findBySubmissionId(dto.getSubmissionId());
		if (submissionFiles != null && !submissionFiles.isEmpty()) {
			updateCategories(dto.getBundles());
			updateCategories(dto.getDocuments());
			updateCategories(dto.getUnassigned());
			updateBundles(submissionFiles, dto.getBundles());
		}
	}

	public void updateCategories(List<BundleDTO> bundleDTOs) {
		for (BundleDTO bundleDTO : bundleDTOs) {
			// Find the corresponding data file
			Optional<DataFile> optionalDataFile = dataFileRepository.findById(bundleDTO.getId());
			if (optionalDataFile.isPresent()) {
				DataFile mainDataFile = optionalDataFile.get();
				String newCategoryName = bundleDTO.getCategory();
				// Check if the category needs to be updated
				if (!mainDataFile.getFileCategory().getName().equals(newCategoryName)) {
					updateDataFileCategory(mainDataFile, newCategoryName);
				}
			} else {
				throw new BadDataException("Unable to find Data File " + bundleDTO.getId());
			}
			// If there are child files in the bundle DTO, update their categories as well
			if (!bundleDTO.getChildFiles().isEmpty()) {
				updateChildFileCategories(bundleDTO);
			}
		}
	}

    /**
     * Updates the file category for the child files of a bundle.
     * @param bundleDTO       The DTO representation of the bundle
     */
	public void updateChildFileCategories(BundleDTO bundleDTO) {
		// Create a map of ChildFileDTO using their IDs as keys
		for (BundleDTO childFileDTO : bundleDTO.getChildFiles()) {
			if (childFileDTO != null) {
				// Get the data file corresponding to the child file ID
				Optional<DataFile> optionalDataFile = dataFileRepository.findById(childFileDTO.getId());
				if (optionalDataFile.isPresent()) {
					DataFile childDataFile = optionalDataFile.get();
					String newChildCategoryName = childFileDTO.getCategory();
					if (!childDataFile.getFileCategory().getName().equals(newChildCategoryName)) {
						updateDataFileCategory(childDataFile, newChildCategoryName);
					}
				} else {
					throw new BadDataException("Unable to find Child Data File " + childFileDTO.getId() + " from Parent " + bundleDTO.getId());
				}
			}
		}
	}


    /**
     Updates the category of a bundle data file with the provided category name.
     @param dataFile The bundle data file to update.
     @param categoryName The name of the category to set.
     @throws CategoryNotFoundException If the category does not exist for the given bundle data file.
     */
	public void updateDataFileCategory(DataFile dataFile, String categoryName) {
		// Find the file category by name
		DataFileCategory fileCategory = dataFileCategoryRepository.findByName(categoryName);
		// If the file category exists, update the bundle entity with the new category and save it
		if (fileCategory != null) {
			dataFile.setFileCategory(fileCategory);
			dataFileRepository.save(dataFile);
		} else {
			throw new BadDataException("Category not found for data file ID: " + dataFile.getId());
		}
	}

    /**
     Updates the bundles in the database with the provided bundle entities and bundle DTOs.
     @param dataFiles The list of DataFile entities representing the bundles to update.
     @param bundleDTOs The list of BundleDTO objects containing the updated bundle information.
     @return True if the update was successful, false otherwise.
     */
	public boolean updateBundles(List<DataFile> dataFiles, List<BundleDTO> bundleDTOs) {
		// Reset dictionaryFileId and metadataFileId for each DataFile in bundleEntities
		dataFiles.forEach(dataFile -> {
			dataFile.setDictionaryFileId(null);
			dataFile.setMetadataFileId(null);
		});
		// Save all modified DataFile entities to the dataFileRepository
		dataFileRepository.saveAll(dataFiles);
		// Perform additional operations to update parent DataFiles using bundleDTOs
		updateParentDataFiles(dataFiles, bundleDTOs);
		return true;
	}

    /**
     Updates the parent DataFiles in the bundleEntities list using the information provided in the bundleDTOs list.
     @param bundleEntities The list of DataFile entities representing the bundles to update.
     @param bundleDTOs The list of BundleDTO objects containing the updated bundle information.
     @throws BadDataException if the data in the bundleDTOs is incorrect or inconsistent.
     */
	public void updateParentDataFiles(List<DataFile> bundleEntities, List<BundleDTO> bundleDTOs) {
		// Create a map of bundle entities using their ID as the key
		Map<Integer, DataFile> bundleEntityMap = bundleEntities.stream()
				.collect(Collectors.toMap(DataFile::getId, Function.identity()));
		// Iterate over each parent bundle DTO
		for (BundleDTO bundle : bundleDTOs) {
			// Check if the parent bundle has less than or equal to 2 child files
			if (bundle.getChildFiles().size() > 2) {
				throw new BadDataException("The data is incorrect: Parent with ID " + bundle.getId() + " cannot have more than 2 children");
			}

			int metadataCount = 0;
			int dictionaryCount = 0;
			DataFile dataFile = bundleEntityMap.get(bundle.getId());
			// Iterate over each child bundle DTO in the parent bundle
			for (BundleDTO child : bundle.getChildFiles()) {
				DataFileCategory fileCategory = dataFileCategoryRepository.findByName(child.getCategory());
				if (fileCategory != null) {
					String categoryName = fileCategory.getName();
					boolean isMetadata = categoryName.contains("File Metadata");
					boolean isDictionary = categoryName.contains("File Data Dictionary");
					// Check if it is a "metadata" file
					if (isMetadata) {
						metadataCount++;
						if (metadataCount > 1) {
							throw new BadDataException("The bundle can have only one metadata file for parent " + bundle.getId());
						}
						dataFile.setMetadataFileId(child.getId());
					}// Check if it is a "data dictionary" file
					else if (isDictionary) {
						dictionaryCount++;
						if (dictionaryCount > 1) {
							throw new BadDataException("The bundle can have only one data dictionary file for parent " + bundle.getId());
						}
						dataFile.setDictionaryFileId(child.getId());
					} else {
						throw new BadDataException("Category Group Invalid for child data file ID: " + child.getId());
					}
				} else {
					throw new BadDataException("Category not found for data file ID: " + dataFile.getId());
				}
			}

			// Save the data file outside the loop for child files
			dataFileRepository.save(dataFile);
		}
	}

	/**
    Gets all the data file categories and returns them grouped by category group
    @return Map of "Category Group" -> [Data File Categories]
    */
	public Map<String, List<DataFileCategory>> getDataFileCategories() {

		List<DataFileCategory> categories = dataFileCategoryRepository.findAll();

		return categories.stream().collect(Collectors.groupingBy(DataFileCategory::getCategoryGroup));
	}

	/**
	 * Updates the step ID of a data submission based on the provided step description.
	 *
	 * @param submissionId    the ID of the data submission
	 * @param stepDescription the current step description
	 */

	public void updateStepId(Integer submissionId, String stepDescription, Integer userId) {

		// Find the data submission by ID
		DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new NoSuchElementException("Submission not found with ID: " + submissionId));

		// Convert step descriptions to the corresponding updated step description
		switch (stepDescription) {
		case "Upload Files":
			stepDescription = "Bundle Files";
			break;
		case "Bundle Files":
			stepDescription = "Validate Files";
			break;
		case "Validate Files":
			stepDescription = "Review and Submit";
			dataSubmission.setValidated(true);
			break;
		case "Review and Submit":
			stepDescription = "Submitted";
			break;
		case "Submitted":
			break;
		default:
			throw new IllegalArgumentException("Invalid step description: " + stepDescription);
		}

		// Find the ID of the updated step based on the updated step description
		Optional<LkupSubmissionStep> lkupSubmissionStepdesc = lkupSubmissionStepRepository.findByDescription(stepDescription);

		// Update the step ID of the data submission
		dataSubmission.setStepId(lkupSubmissionStepdesc.get());
		dataSubmission.setModifiedBy(userId);
		dataSubmission.setModifiedAt(Timestamp.valueOf(LocalDateTime.now()));
		dataSubmissionRepository.save(dataSubmission);
	}

	/**
	 * Retrieves the submission information for a given submission ID.
	 *
	 * @param submissionId the ID of the data submission
	 * @return the SubmissionInfoDTO object containing the submission information
	 * @throws NoSuchElementException if the data submission is not found
	 */
	public SubmissionStepDTO getSubmissionInfo(Integer submissionId) {
		// Check if submission id is valid then find the data submission by ID
		DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
		Integer updatedStepId = dataSubmission.getStepId().getId();
		Integer studyId = dataSubmission.getStudyId();
		Study study = studyRepository.findStudyById(studyId);
		// Find the lookup submission step using the updated step ID
		Optional<LkupSubmissionStep> lkupSubmissionStep = lkupSubmissionStepRepository.findById(updatedStepId);

		return submissionStepMapper.toStepDto(lkupSubmissionStep.orElse(null), dataSubmission, study);
	}

	/**
	 * Retrieves the source file name and category for each DataFile in a Bundle
	 * @param dataFileId The ID of a member of a Bundle
	 * @return List of source file name and category for each DataFile in a Bundle
	 */
	public GetBundleFilesDTO getBundleFiles(Integer dataFileId){
		Set<Integer> fileIds = dataFileService.getBundleIds(dataFileId);
		List<DataFile> dataFiles = dataFileRepository.findAllById(fileIds);
		boolean isLastBundle = isLastBundle(dataFiles);
		List<BundleFileDTO> files = dataFileMapper.mapToBundleFileDTOList(dataFiles);
		return new GetBundleFilesDTO(isLastBundle, files);
	}

	private boolean isLastBundle(List<DataFile> dataFiles) {
		if(dataFiles.isEmpty()) {
			return false;
		}
		Integer submissionId = dataFiles.get(0).getSubmissionId();
		Integer submissionSize = dataFileRepository.countAllBySubmissionId(submissionId);
		return dataFiles.size() == submissionSize;
	}

	public void goBackAndUploadFiles(Integer submissionId) {
		DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
		Optional<LkupSubmissionStep> lkupSubmissionStepdesc = lkupSubmissionStepRepository.findByDescription("Upload Files");
		if (lkupSubmissionStepdesc.isEmpty()){
			throw new NoSuchElementException("LkupSubmissionStep not found");
		}

		// Update the step ID of the data submission
		dataSubmission.setStepId(lkupSubmissionStepdesc.get());
		dataSubmissionRepository.save(dataSubmission);
	}
}
