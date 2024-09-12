package com.verizon.ucs.s3helper.repository;

import com.verizon.ucs.s3helper.model.UcspProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UCSPProjectsRepositoryTest {

    @Autowired
    private UCSPProjectsRepository projectsRepository;

    @BeforeEach
    public void setup() {
        // Create and save mock project data
        UcspProject project1 = new UcspProject();
        project1.setId(1);  // Assume setId is present
        project1.setName("Project 1");
        projectsRepository.save(project1);

        UcspProject project2 = new UcspProject();
        project2.setId(2);  
        project2.setName("Project 2");
        projectsRepository.save(project2);
    }

    @Test
    public void testFindUniqueProjects() {
        List<UcspProject> uniqueProjects = projectsRepository.findUniqueProjects();
        assertThat(uniqueProjects).isNotEmpty();
        assertThat(uniqueProjects.size()).isEqualTo(2); // There should be 2 unique projects
    }
}



package com.verizon.ucs.s3helper.repository;

import com.verizon.ucs.s3helper.model.UcspUcgSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UCSPUcgSourceRepositoryTest {

    @Autowired
    private UCSPUcgSourceRepository ucgSourceRepository;

    @BeforeEach
    public void setup() {
        // Insert some mock UcspUcgSource data
        UcspUcgSource source1 = new UcspUcgSource();
        source1.setId(1);
        source1.setProjectId(1);
        ucgSourceRepository.save(source1);
        
        UcspUcgSource source2 = new UcspUcgSource();
        source2.setId(2);
        source2.setProjectId(2);
        ucgSourceRepository.save(source2);
    }

    @Test
    public void testFindAllUcgSources() {
        List<UcspUcgSource> sources = ucgSourceRepository.findAll();
        assertThat(sources).isNotEmpty();
        assertThat(sources.size()).isEqualTo(2); // There should be 2 ucg sources
    }
}



package com.verizon.ucs.s3helper.repository;

import com.verizon.ucs.s3helper.model.Trends;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UCSPTrendsRepositoryTest {

    @Autowired
    private UCSPTrendsRepository trendsRepository;

    private int ucgSourceId = 1;
    private Date collectionDate;

    @BeforeEach
    public void setup() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        collectionDate = dateFormat.parse("2023-01-01");

        // Insert some mock data into the repository
        Trends trend = new Trends();
        trend.setUcgSourceID(ucgSourceId);
        trend.setCollectionDate(collectionDate);
        trend.setNumberOfFiles(5);
        trend.setSizeOfFilesKB(1024);
        trendsRepository.save(trend);
    }

    @Test
    public void testFindDailyTrends() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date fromDate = dateFormat.parse("2023-01-01");
        Date toDate = dateFormat.parse("2023-01-02");

        List<Trends> trends = trendsRepository.findDailyTrends(ucgSourceId, fromDate, toDate);
        assertThat(trends).isNotEmpty();
        assertThat(trends.size()).isEqualTo(1);  // Expect 1 trend to be found
    }

    @Test
    public void testExistsByUcgSourceIDAndCollectionDate() {
        boolean exists = trendsRepository.existsByUcgSourceIDAndCollectionDate(ucgSourceId, collectionDate);
        assertThat(exists).isTrue();  // The trend with ucgSourceId and collectionDate exists
    }

    @Test
    public void testDeleteByCollectionDateBefore() throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date thresholdDate = dateFormat.parse("2022-12-31");

        trendsRepository.deleteByCollectionDateBefore(thresholdDate);

        // Ensure the trend is still there because it's after the threshold date
        List<Trends> remainingTrends = trendsRepository.findAll();
        assertThat(remainingTrends.size()).isEqualTo(1);
    }
}
