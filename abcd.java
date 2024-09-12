package com.verizon.ucs.s3helper.service;

import com.verizon.ucs.s3helper.model.Trends;
import com.verizon.ucs.s3helper.model.UcspProject;
import com.verizon.ucs.s3helper.model.UcspUcgSource;
import com.verizon.ucs.s3helper.repository.UCSPProjectsRepository;
import com.verizon.ucs.s3helper.repository.UCSPTrendsRepository;
import com.verizon.ucs.s3helper.repository.UCSPUcgSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class S3HelperTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private UCSPProjectsRepository projectsRepository;

    @Mock
    private UCSPUcgSourceRepository ucgSourceRepository;

    @Mock
    private UCSPTrendsRepository trendsRepository;

    @InjectMocks
    private S3Helper s3Helper;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);  // Initialize mocks before each test
    }

    @Test
    public void testFetchAndStoreAuditData() throws Exception {
        // Mock repositories
        UcspProject project = new UcspProject();
        project.setId(1); // Set an ID to avoid null pointer issues
        project.setS3PrimaryPath("test-project-path/");

        UcspUcgSource ucgSource = new UcspUcgSource();
        ucgSource.setId(1);
        ucgSource.setS3PrimaryPath("test-source-path/");
        ucgSource.setProjectId(1);

        // Mock findAll() to return a list with one project and one source
        when(projectsRepository.findAll()).thenReturn(Collections.singletonList(project));
        when(ucgSourceRepository.findAll()).thenReturn(Collections.singletonList(ucgSource));

        // Mock S3Client response
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Collections.singletonList(S3Object.builder().key("test-project-path/test-source-path/2023-01-01/file.txt").size(1024L).build()))
                .build();
        when(s3Client.listObjectsV2(any())).thenReturn(response);

        // Call the method
        s3Helper.fetchAndStoreAuditData("bucket-name");

        // Verify that the save method is called on the trends repository
        verify(trendsRepository, times(1)).save(any(Trends.class));
    }

    @Test
    public void testDeleteOldTrendsData() {
        // Call the method
        s3Helper.deleteOldTrendsData();

        // Verify delete method is invoked with any Date parameter
        verify(trendsRepository).deleteByCollectionDateBefore(any(Date.class));
    }
}
