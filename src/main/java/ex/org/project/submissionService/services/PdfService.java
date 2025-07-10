package ex.org.project.submissionService.services;

import ex.org.project.submissionService.exceptions.custom.PdfParsingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PdfService {

    public Map<String, String> parseStudyRegistrationForm(MultipartFile file){
        try(PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDAcroForm acroForm = catalog.getAcroForm();

            return acroForm.getFields()
                    .stream()
                    .collect(Collectors.toMap(
                            PDField::getFullyQualifiedName,
                            PDField::getValueAsString)
                    );
        } catch (IOException e){
            String errorMessage = "Error parsing MFA form PDF";
            log.error(errorMessage, e);
            throw new PdfParsingException(errorMessage);
        }
    }
}
