package com.verizon.ucs.s3helper.repository;

import com.verizon.ucs.s3helper.model.UcspProject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UCSPProjectsRepositoryTest {

    @Autowired
    private UCSPProjectsRepository repository;

    @Test
    public void testFindUniqueProjects() {
        List<UcspProject> projects = repository.findUniqueProjects();
        // Add assertions based on your test data
        assertThat(projects).isNotEmpty();
    }
}



package com.verizon.ucs.s3helper.repository;

import com.verizon.ucs.s3helper.model.Trends;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UCSPTrendsRepositoryTest {

    @Autowired
    private UCSPTrendsRepository repository;

    @Test
    public void testFindDailyTrends() {
        // Prepare test data and call the method
        List<Trends> trends = repository.findDailyTrends(1, new Date(), new Date());
        // Add assertions based on your test data
        assertThat(trends).isNotEmpty();
    }

    @Test
    public void testExistsByUcgSourceIDAndCollectionDate() {
        boolean exists = repository.existsByUcgSourceIDAndCollectionDate(1, new Date());
        // Add assertions based on your test data
        assertThat(exists).isTrue();
    }

    @Test
    public void testDeleteByCollectionDateBefore() {
        repository.deleteByCollectionDateBefore(new Date());
        // Verify that data is deleted; this might need additional setup
    }
}



package com.verizon.ucs.s3helper.scheduler;

import com.verizon.ucs.s3helper.service.S3Helper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.mockito.Mockito.verify;

public class SchedulerTest {

    @Mock
    private S3Helper s3Helper;

    @InjectMocks
    private Scheduler scheduler;

    public SchedulerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testScheduleAuditDataFetch() {
        scheduler.scheduleAuditDataFetch();
        verify(s3Helper).fetchAndStoreAuditData("your-bucket-name"); // Adjust if necessary
    }

    @Test
    public void testScheduleAuditDataCleanup() {
        scheduler.scheduleAuditDataCleanup();
        verify(s3Helper).deleteOldTrendsData();
    }
}



package com.verizon.ucs.s3helper.service;

import com.verizon.ucs.s3helper.model.Trends;
import com.verizon.ucs.s3helper.model.UcspProject;
import com.verizon.ucs.s3helper.model.UcspUcgSource;
import com.verizon.ucs.s3helper.repository.UCSPProjectsRepository;
import com.verizon.ucs.s3helper.repository.UCSPTrendsRepository;
import com.verizon.ucs.s3helper.repository.UCSPUcgSourceRepository;
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
import static org.mockito.ArgumentMatchers.eq;
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

    public S3HelperTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFetchAndStoreAuditData() throws Exception {
        // Mock repositories
        List<UcspProject> projects = Collections.singletonList(new UcspProject());
        List<UcspUcgSource> ucgSources = Collections.singletonList(new UcspUcgSource());
        when(projectsRepository.findAll()).thenReturn(projects);
        when(ucgSourceRepository.findAll()).thenReturn(ucgSources);

        // Mock S3Client
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Collections.singletonList(S3Object.builder().key("key").size(1024L).build()))
                .build();
        when(s3Client.listObjectsV2(any())).thenReturn(response);

        // Call the method
        s3Helper.fetchAndStoreAuditData("bucket-name");

        // Verify interactions
        verify(trendsRepository, times(1)).save(any(Trends.class));
    }

    @Test
    public void testDeleteOldTrendsData() {
        s3Helper.deleteOldTrendsData();
        verify(trendsRepository).deleteByCollectionDateBefore(any(Date.class));
    }
}
