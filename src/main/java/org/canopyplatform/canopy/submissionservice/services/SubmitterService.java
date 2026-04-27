package org.canopyplatform.canopy.submissionservice.services;

import java.util.List;

import org.canopyplatform.canopy.submissionservice.exceptions.custom.DataFileNotFoundException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.FileDeletionException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.StatusNotFoundException;
import org.canopyplatform.canopy.submissionservice.exceptions.custom.SubmissionIdInvalidException;
import org.canopyplatform.canopy.submissionservice.models.*;
import org.canopyplatform.canopy.submissionservice.models.dtos.SubmissionInfoDTO;
import org.canopyplatform.canopy.submissionservice.repositories.DataFileRepository;
import org.canopyplatform.canopy.submissionservice.repositories.LkupStatusRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import org.canopyplatform.canopy.submissionservice.mappers.SubmitterInfoMapper;
import org.canopyplatform.canopy.submissionservice.repositories.DataSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SubmitterService {

    private final SubmitterInfoMapper submitterInfoMapper;
    private final DataSubmissionRepository dataSubmissionRepository;
    private final DataFileRepository dataFileRepository;
    private final DataFileService dataFileService;
    private final LkupStatusRepository lkupStatusRepository;

    /**
     * Retrieves the submissions for a user via user ID.
     *
     * @param userId The ID of the submitter
     * @return List of SubmitterInfoDTO objects containing the submitter information
     */
    public List<SubmissionInfoDTO> getSubmissions(Integer userId, String statusValue) {
        LkupStatus status = lkupStatusRepository.findByUsageAndName(Constants.USAGE_DATA_SUBMISSION, statusValue)
                .orElseThrow(()  -> new StatusNotFoundException(String.format("Could not find submission status entity. Invalid Support Request Status: %s", statusValue)));
        return dataSubmissionRepository
                .findBySubmitterUserIdAndStatusOrderByModifiedAtDesc(userId, status)
                .stream()
                .map(submitterInfoMapper::toDTO)
                .toList();
    }

    @Transactional
    public void deleteSubmission(Integer submissionId) {
        DataSubmission dataSubmission = dataSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionIdInvalidException("Could not find submission ID: " + submissionId));
        List<DataFile> datafileList = dataFileRepository.findBySubmissionId(dataSubmission.getId());
        if (!datafileList.isEmpty()) {
            datafileList.stream().filter(df -> df.getS3File() == null).findAny().ifPresent(df -> {
                throw new DataFileNotFoundException("No S3 file found for ID " + df.getId());
            });
            for (DataFile dataFile : datafileList) {
                S3File s3File = dataFile.getS3File();
                s3File.setS3FileKeyAndBucketFromPath();
                //check if any data files have file to be deleted as a foreign key reference
                //if so, get them and set reference to null
                List<DataFile> foreignKeyDictFiles = dataFileRepository.findDataFilesByDictionaryFileId(dataFile.getId());
                if (!foreignKeyDictFiles.isEmpty()) {
                    foreignKeyDictFiles.forEach(df -> df.setDictionaryFileId(null));
                    dataFileRepository.saveAll(foreignKeyDictFiles);
                }

                List<DataFile> foreignKeyMetaFiles = dataFileRepository.findDataFilesByMetadataFileId(dataFile.getId());
                if (!foreignKeyMetaFiles.isEmpty()) {
                    foreignKeyMetaFiles.forEach(df -> df.setMetadataFileId(null));
                    dataFileRepository.saveAll(foreignKeyMetaFiles);
                }

                try {
                    //delete object from data_file and s3_file tables, and the s3 bucket
                    dataFileRepository.deleteById(dataFile.getId());
                    dataFileService.deleteS3FileAndEntity(s3File);
                } catch (DataAccessException e) {
                    throw new FileDeletionException("Error deleting file from database");
                }
            }
        } else {
          throw new DataFileNotFoundException("No data file descriptor found for submission ID: " + submissionId);
        }
        dataSubmissionRepository.deleteById(submissionId);
    }

}
