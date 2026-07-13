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

@FacesConverter("yearMonthConverter")
public class YearMonthConverter implements Converter<YearMonth> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public YearMonth getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ConverterException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid year-month",
                    "Use yyyy-MM format."));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, YearMonth value) {
        if (value == null) {
            return "";
        }
        return value.format(FORMATTER);
    }
}
