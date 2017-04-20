package org.perfcake.reporting.destination.builders;

import org.perfcake.reporting.destination.dto.test.TestDto;
import org.perfcake.reporting.destination.dto.test_execution.AttachmentDto;
import org.perfcake.reporting.destination.dto.test_execution.ParameterDto;
import org.perfcake.reporting.destination.dto.test_execution.TestExecutionDto;
import org.perfcake.reporting.destination.dto.test_execution.ValuesGroupDto;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @author Jiri Grunwald (grunwjir@gmail.com)
 */
public class TestExecutionDtoBuilder {

    private TestExecutionDto testExecutionDto;

    public TestExecutionDtoBuilder() {
        testExecutionDto = new TestExecutionDto();
        testExecutionDto.setTags(new LinkedHashSet<>());
        testExecutionDto.setExecutionParameters(new LinkedHashSet<>());
        testExecutionDto.setExecutionValuesGroups(new LinkedHashSet<>());
        testExecutionDto.setExecutionAttachments(new ArrayList<>());
    }

    public TestExecutionDtoBuilder name(String name) {
        testExecutionDto.setName(name);
        return this;
    }

    public TestExecutionDtoBuilder test(TestDto test) {
        testExecutionDto.setTest(test);
        return this;
    }

    public TestExecutionDtoBuilder tag(String tag) {
        testExecutionDto.getTags().add(tag);
        return this;
    }

    public TestExecutionDtoBuilder tags(Set<String> tags) {
        testExecutionDto.getTags().addAll(tags);
        return this;
    }

    public TestExecutionDtoBuilder comment(String comment) {
        testExecutionDto.setComment(comment);
        return this;
    }

    public TestExecutionDtoBuilder started(Date started) {
        testExecutionDto.setStarted(started);
        return this;
    }

    public TestExecutionDtoBuilder executionParameter(String name, String value) {
        ParameterDto parameterDto = new ParameterDto();
        parameterDto.setName(name);
        parameterDto.setValue(value);
        testExecutionDto.getExecutionParameters().add(parameterDto);
        return this;
    }

    public TestExecutionDtoBuilder executionParameters(Set<ParameterDto> parameters) {
        testExecutionDto.getExecutionParameters().addAll(parameters);
        return this;
    }

    public TestExecutionDtoBuilder executionAttachment(AttachmentDto attachment) {
        testExecutionDto.getExecutionAttachments().add(attachment);
        return this;
    }

    public TestExecutionDtoBuilder executionValuesGroup(ValuesGroupDto valuesGroup) {
        testExecutionDto.getExecutionValuesGroups().add(valuesGroup);
        return this;
    }

    public TestExecutionDto build() {
        return testExecutionDto;
    }
}
