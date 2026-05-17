package com.keplerops.groundcontrol.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keplerops.groundcontrol.domain.exception.DomainValidationException;
import com.keplerops.groundcontrol.domain.projects.model.Project;
import com.keplerops.groundcontrol.domain.testcases.model.TestCaseFolder;
import org.junit.jupiter.api.Test;

class TestCaseFolderTest {

    private static Project project() {
        return new Project("ground-control", "Ground Control");
    }

    @Test
    void constructorInitialisesRequiredFields() {
        var folder = new TestCaseFolder(project(), null, "Smoke tests", "regression smoke set", 0);
        assertThat(folder.getProject().getIdentifier()).isEqualTo("ground-control");
        assertThat(folder.getParent()).isNull();
        assertThat(folder.getTitle()).isEqualTo("Smoke tests");
        assertThat(folder.getDescription()).isEqualTo("regression smoke set");
        assertThat(folder.getSortOrder()).isZero();
    }

    @Test
    void constructorAllowsNestingViaParent() {
        var root = new TestCaseFolder(project(), null, "Suite", null, 0);
        var child = new TestCaseFolder(project(), root, "Login flow", null, 0);
        assertThat(child.getParent()).isSameAs(root);
    }

    @Test
    void constructorRejectsNullProject() {
        assertThatThrownBy(() -> new TestCaseFolder(null, null, "Folder", null, 0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Project");
    }

    @Test
    void constructorRejectsBlankTitle() {
        var project = project();
        assertThatThrownBy(() -> new TestCaseFolder(project, null, "", null, 0))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Title");
        assertThatThrownBy(() -> new TestCaseFolder(project, null, "   ", null, 0))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> new TestCaseFolder(project, null, null, null, 0))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void constructorRejectsNegativeSortOrder() {
        var project = project();
        assertThatThrownBy(() -> new TestCaseFolder(project, null, "Folder", null, -1))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Sort order");
    }

    @Test
    void setTitleRejectsBlank() {
        var folder = new TestCaseFolder(project(), null, "Folder", null, 0);
        assertThatThrownBy(() -> folder.setTitle(""))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Title");
    }

    @Test
    void setSortOrderRejectsNegative() {
        var folder = new TestCaseFolder(project(), null, "Folder", null, 0);
        assertThatThrownBy(() -> folder.setSortOrder(-3))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Sort order");
    }

    @Test
    void parentMayBeReassignedToRoot() {
        var parent = new TestCaseFolder(project(), null, "Parent", null, 0);
        var folder = new TestCaseFolder(project(), parent, "Child", null, 0);
        folder.setParent(null);
        assertThat(folder.getParent()).isNull();
    }
}
