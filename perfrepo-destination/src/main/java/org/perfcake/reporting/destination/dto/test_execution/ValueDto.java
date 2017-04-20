package org.perfcake.reporting.destination.dto.test_execution;

import java.util.Set;

/**
 * Represents one measured value of a test execution {@link TestExecutionDto}.
 *
 * @author Jiri Grunwald (grunwjir@gmail.com)
 */
public class ValueDto {

    private double value;

    private Set<ValueParameterDto> parameters;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Set<ValueParameterDto> getParameters() {
        return parameters;
    }

    public void setParameters(Set<ValueParameterDto> parameters) {
        this.parameters = parameters;
    }
}