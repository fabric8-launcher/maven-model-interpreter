package io.fabric8.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.assertj.XmlAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
class MavenTest {

    @Test
    void should_read_model() {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom);
        assertThat(model).isNotNull();
        assertThat(model.getPomFile().getAbsolutePath()).isEqualTo(basePom.toAbsolutePath().toString());
        assertThat(model.getParent().getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_read_model_using_reader() {
        Model model = Maven.readModel(
                new StringReader("<project><groupId>org.jboss</groupId><artifactId>maven-model-helper</artifactId></project>"));
        assertThat(model).isNotNull();
        assertThat(model.getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_read_model_string() {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom.toAbsolutePath().toString());
        assertThat(model).isNotNull();
        assertThat(model.getPomFile().getAbsolutePath()).isEqualTo(basePom.toAbsolutePath().toString());
        assertThat(model.getParent().getGroupId()).isEqualTo("org.jboss");
        assertThat(model.getArtifactId()).isEqualTo("maven-model-helper");
    }

    @Test
    void should_write_model(@TempDir Path tempDir) throws IOException {
        File pom = tempDir.resolve("temp-pom.xml").toFile();
        Model model = new Model();
        model.setPomFile(pom);
        model.setGroupId("org.example");
        model.setArtifactId("example");
        model.setVersion("1.0");
        Maven.writeModel(model);
        assertThat(Files.readAllLines(pom.toPath()).stream().map(String::trim))
                .contains("<groupId>org.example</groupId>", "<artifactId>example</artifactId>", "<version>1.0</version>");
    }

    @Test
    void should_write_model_with_sorted_properties(@TempDir Path tempDir) throws IOException {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(basePom.toAbsolutePath().toString());

        Properties properties = model.getProperties();
        assertThat(properties).isInstanceOf(SortedProperties.class);

        properties.put("c", "three");
        properties.put("a", "one");
        properties.put("b", "two");

        // Write pom
        Path pom = tempDir.resolve("temp-pom.xml");
        Maven.writeModel(model, pom);
        assertThat(Files.readAllLines(pom).stream().map(String::trim))
                .containsSequence("<a>one</a>", "<b>two</b>", "<c>three</c>");
    }

    @Test
    void should_write_model_with_sorted_properties_using_reader(@TempDir Path tempDir) throws IOException {
        Path basePom = Paths.get("pom.xml");
        Model model = Maven.readModel(new FileReader(basePom.toFile()));

        Properties properties = model.getProperties();
        assertThat(properties).isInstanceOf(SortedProperties.class);

        properties.put("c", "three");
        properties.put("a", "one");
        properties.put("b", "two");

        // Write pom
        Path pom = tempDir.resolve("temp-pom.xml");
        Maven.writeModel(model, pom);
        assertThat(Files.readAllLines(pom).stream().map(String::trim))
                .containsSequence("<a>one</a>", "<b>two</b>", "<c>three</c>");
    }

    @Test
    void should_preserve_parent_relative_path(@TempDir Path tempDir) throws Exception {
        URL resource = getClass().getResource("parent/parent-pom.xml");
        Path parentPom = Paths.get(resource.toURI());
        Path newPath = tempDir.resolve("new-pom.xml");
        Model model = Maven.readModel(parentPom);
        assertThat(model.getParent().getRelativePath()).isNotNull();

        Maven.writeModel(model, newPath);
        XmlAssert.assertThat(newPath)
                .withNamespaceContext(Collections.singletonMap("maven", "http://maven.apache.org/POM/4.0.0"))
                .valueByXPath("//maven:project/maven:parent/maven:relativePath")
                .isEqualTo("../../pom.xml");
    }

}
