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
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

public class S3HelperTest {

    @InjectMocks
    private S3Helper s3Helper;

    @Mock
    private S3Client s3Client;

    @Mock
    private UCSPProjectsRepository projectsRepository;

    @Mock
    private UCSPUcgSourceRepository ucgSourceRepository;

    @Mock
    private UCSPTrendsRepository trendsRepository;

    @Value("${data.threshold-days}")
    private int thresholdDays = 30;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFetchAndStoreAuditData() throws Exception {
        // Mock the S3 responses
        List<S3Object> s3Objects = Arrays.asList(
            S3Object.builder().key("project/ucgSource/2024-09-10/file1").size(1024L).build()
        );
        ListObjectsV2Response response = ListObjectsV2Response.builder().contents(s3Objects).build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // Mock repositories
        UcspProject project = new UcspProject();
        project.setId(1);
        project.setS3PrimaryPath("project/");
        UcspUcgSource ucgSource = new UcspUcgSource();
        ucgSource.setId(1);
        ucgSource.setProjectId(1);
        ucgSource.setS3PrimaryPath("ucgSource/");
        when(projectsRepository.findAll()).thenReturn(Collections.singletonList(project));
        when(ucgSourceRepository.findAll()).thenReturn(Collections.singletonList(ucgSource));
        when(trendsRepository.existsByUcgSourceIDAndCollectionDate(anyInt(), any(Date.class))).thenReturn(false);

        // Execute the method
        s3Helper.fetchAndStoreAuditData("test-bucket");

        // Verify interactions
        verify(trendsRepository).save(any(Trends.class));
    }

    @Test
    void testDeleteOldTrendsData() {
        s3Helper.deleteOldTrendsData();
        // Verify that the delete method was called with the correct parameters
        verify(trendsRepository).deleteByCollectionDateBefore(any(Date.class));
    }
}



package com.verizon.ucs.s3helper.repository;

import com.verizon.ucs.s3helper.model.Trends;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UCSPTrendsRepositoryTest {

    @Autowired
    private UCSPTrendsRepository trendsRepository;

    private Trends trends;

    @BeforeEach
    void setUp() {
        trends = new Trends();
        trends.setUcgSourceID(1);
        trends.setCollectionDate(new Date());
        trends.setSizeOfFilesKB(1024);
        trends.setNumberOfFiles(10);
        trendsRepository.save(trends);
    }

    @Test
    void testFindDailyTrends() {
        Date fromDate = new Date(System.currentTimeMillis() - 86400000L);
        Date toDate = new Date();
        assertThat(trendsRepository.findDailyTrends(1, fromDate, toDate)).hasSize(1);
    }

    @Test
    void testExistsByUcgSourceIDAndCollectionDate() {
        assertThat(trendsRepository.existsByUcgSourceIDAndCollectionDate(1, trends.getCollectionDate())).isTrue();
    }

    @Test
    void testDeleteByCollectionDateBefore() {
        Date thresholdDate = new Date(System.currentTimeMillis() - 86400000L);
        trendsRepository.deleteByCollectionDateBefore(thresholdDate);
        assertThat(trendsRepository.existsByUcgSourceIDAndCollectionDate(1, trends.getCollectionDate())).isFalse();
    }
}
