package ex.org.project.submissionService.utils;

import ex.org.project.submissionService.models.DataFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BundlingUtils {

    public static List<DataFile> popParentFilesFromList(List<DataFile> dataFiles) {
        List<DataFile> parentDataFiles = dataFiles.stream().filter(BundlingUtils::isDataFile).toList();
        dataFiles.removeAll(parentDataFiles);
        return parentDataFiles;
    }

    public static List<DataFile> popDocumentsFromList(List<DataFile> dataFiles) {
        List<DataFile> documentFiles = dataFiles.stream().filter(BundlingUtils::isDocumentFile).toList();
        dataFiles.removeAll(documentFiles);
        return documentFiles;
    }

    public static boolean isDataFile(DataFile df) {
        return df.getFileCategory().getCategoryGroup().equals("data");
    }

    public static boolean isDocumentFile(DataFile df) {
        return df.getFileCategory().getCategoryGroup().equals("document");
    }

    public static Map<Integer, List<Integer>> getBundleIdMapping(List<DataFile> parentFiles) {
        return parentFiles.stream()
                .collect(Collectors.toMap(
                        DataFile::getId,
                        BundlingUtils::getChildIds));
    }

    public static List<Integer> getChildIds(DataFile dataFile) {
        List<Integer> childIds = new ArrayList<>();
        if(dataFile.getMetadataFileId() != null) {
            childIds.add(dataFile.getMetadataFileId());
        }
        if(dataFile.getDictionaryFileId() != null) {
            childIds.add(dataFile.getDictionaryFileId());
        }
        return childIds;
    }

    public static List<DataFile> popChildrenFromList(Integer parentId, Map<Integer, List<Integer>> bundleIdMapping,
                                                        List<DataFile> dataFiles) {
        List<Integer> childIds = bundleIdMapping.get(parentId);
        List<DataFile> childFiles = dataFiles.stream().filter(df -> childIds.contains(df.getId())).toList();
        dataFiles.removeAll(childFiles);
        return childFiles;
    }
}
