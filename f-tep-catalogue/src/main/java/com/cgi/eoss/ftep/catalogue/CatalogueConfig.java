package com.cgi.eoss.ftep.catalogue;

import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,
        PersistenceConfig.class
})
@ComponentScan(basePackageClasses = CatalogueConfig.class)
public class CatalogueConfig {

    @Bean
    public Path referenceDataBasedir(@Value("${ftep.catalogue.refData.baseDir:/data/refData}") String baseDir) {
        return Paths.get(baseDir);
    }

}
