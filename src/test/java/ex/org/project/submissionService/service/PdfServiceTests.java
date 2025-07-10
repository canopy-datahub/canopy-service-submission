package ex.org.project.submissionService.service;

import ex.org.project.submissionService.exceptions.custom.PdfParsingException;
import ex.org.project.submissionService.services.PdfService;
import org.approvaltests.JsonJacksonApprovals;
import org.approvaltests.core.Options;
import org.approvaltests.scrubbers.RegExScrubber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertThrows;

@ExtendWith(MockitoExtension.class)
class PdfServiceTests {

    private static final String PDF_FILE_NAME = "TECH_phs9999_Study Title Goes Here.pdf";

    private final PdfService pdfService = new PdfService();

    @Test
    void testParseStudyRegistrationForm_HappyPath() throws Exception {
        URL examplePdf = getClass().getClassLoader().getResource(PDF_FILE_NAME);
        MockMultipartFile pdfMultiPart = new MockMultipartFile(
                PDF_FILE_NAME, Files.readAllBytes(Paths.get(examplePdf.toURI()))
        );

        Map<String, String> response = pdfService.parseStudyRegistrationForm(pdfMultiPart);

        JsonJacksonApprovals.verifyAsJson(response, new Options(
                new RegExScrubber("PDSignature(.*)", "PDSignature@[memory-location]\"")
        ));
    }

    @Test
    void testParseStudyRegistrationForm_Error() throws Exception {
        MockMultipartFile pdfMultiPart = new MockMultipartFile(PDF_FILE_NAME, "test".getBytes());
        assertThrows(PdfParsingException.class, () -> pdfService.parseStudyRegistrationForm(pdfMultiPart));
    }
}
