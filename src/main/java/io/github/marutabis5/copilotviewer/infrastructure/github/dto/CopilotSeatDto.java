package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import jakarta.json.bind.annotation.JsonbProperty;

public class CopilotSeatDto {

    @JsonbProperty("created_at")
    private String createdAt;

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
