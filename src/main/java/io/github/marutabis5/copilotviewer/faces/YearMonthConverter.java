package io.github.marutabis5.copilotviewer.faces;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * JSF converter between the {@code YYYY-MM} string representation and
 * {@link YearMonth}.  Registered automatically for all {@code YearMonth}
 * properties via {@code forClass}.
 */
@FacesConverter(forClass = YearMonth.class)
public class YearMonthConverter implements Converter<YearMonth> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public YearMonth getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value.trim(), FMT);
        } catch (DateTimeParseException e) {
            throw new ConverterException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid year-month format",
                    "Please enter a year-month in YYYY-MM format (e.g. 2025-06)."));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, YearMonth value) {
        if (value == null) {
            return "";
        }
        return value.format(FMT);
    }
}
