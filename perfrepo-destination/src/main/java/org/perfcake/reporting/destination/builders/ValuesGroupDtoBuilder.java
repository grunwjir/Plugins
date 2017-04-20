package org.perfcake.reporting.destination.builders;

import org.perfcake.reporting.destination.dto.metric.MetricDto;
import org.perfcake.reporting.destination.dto.test_execution.ValueDto;
import org.perfcake.reporting.destination.dto.test_execution.ValuesGroupDto;
import org.perfcake.reporting.destination.enums.MeasuredValueType;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jiri Grunwald (grunwjir@gmail.com)
 */
public class ValuesGroupDtoBuilder {

    private ValuesGroupDto valuesGroupDto;

    public ValuesGroupDtoBuilder() {
        valuesGroupDto = new ValuesGroupDto();
        valuesGroupDto.setValues(new ArrayList<>());
    }

    public ValuesGroupDtoBuilder metric(String metricName) {
        valuesGroupDto.setMetricName(metricName);
        return this;
    }

    public ValuesGroupDtoBuilder valueType(MeasuredValueType valueType) {
        valuesGroupDto.setValueType(valueType);
        return this;
    }

    public ValuesGroupDtoBuilder value(ValueDto value) {
        valuesGroupDto.getValues().add(value);
        return this;
    }

    public ValuesGroupDtoBuilder parameterNames(String... parameterNames) {
        valuesGroupDto.setParameterNames(Stream.of(parameterNames).collect(Collectors.toSet()));
        return this;
    }

    public ValuesGroupDto build() {
        return valuesGroupDto;
    }
}