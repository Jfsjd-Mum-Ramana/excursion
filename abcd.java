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
