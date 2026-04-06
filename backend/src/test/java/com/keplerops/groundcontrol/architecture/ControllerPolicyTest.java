package com.keplerops.groundcontrol.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ControllerPolicyTest {

    @Test
    void everyControllerHasMatchingWebMvcTest() throws IOException {
        var controllerRoot = Path.of("src/main/java/com/keplerops/groundcontrol/api");
        var testRoot = Path.of("src/test/java/com/keplerops/groundcontrol/unit/api");

        List<Path> controllers;
        try (Stream<Path> stream = Files.walk(controllerRoot)) {
            controllers = stream.filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .toList();
        }

        for (Path controller : controllers) {
            var controllerName = controller.getFileName().toString().replace(".java", "");
            var expectedTest = testRoot.resolve(controllerName + "Test.java");

            assertThat(Files.exists(expectedTest))
                    .as("Expected %s to have a matching WebMvc test at %s", controller, expectedTest)
                    .isTrue();

            var testContent = Files.readString(expectedTest);
            assertThat(testContent)
                    .as("Expected %s to use @WebMvcTest", expectedTest)
                    .contains("@WebMvcTest(")
                    .contains(controllerName + ".class");
        }
    }
}
