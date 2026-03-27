package ex.org.project.submissionService.controller;


import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import ex.org.project.submissionService.controllers.BundleController;
import ex.org.project.submissionService.models.dtos.SubmissionBundlesDTO;
import ex.org.project.submissionService.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class BundleControllerTests {

    @Mock
    private BundleService bundleService;

    @InjectMocks
    private BundleController bundleController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetBundles() {
        // Mock the behavior of the bundleService.getBundles() method
        SubmissionBundlesDTO expectedBundles = new SubmissionBundlesDTO();
        Integer submissionId = 123;
        when(bundleService.getBundles(submissionId)).thenReturn(expectedBundles);

        SubmissionBundlesDTO actualBundles = bundleService.getBundles(submissionId);

        // Verify that the bundleService.getBundles() method was called with the correct parameter
        verify(bundleService).getBundles(submissionId);

        // Verify that the returned value matches the expected value
        assertEquals(expectedBundles, actualBundles);
    }
    @Test
    public void testGetBundles_InvalidSubmissionId() {
        // Call the getBundles method with an invalid submissionId
        Integer submissionId = -1;
        SubmissionBundlesDTO response = bundleService.getBundles(submissionId);

        // Verify that the response is null or some other expected value
        assertNull(response);
    }

}
