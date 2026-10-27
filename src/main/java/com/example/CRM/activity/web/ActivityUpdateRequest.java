package com.example.CRM.activity.web;

import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class ActivityUpdateRequest {
    @NotNull(message = "Selecciona un tipo de actividad")
    private ActivityType activityType;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 180, message = "El asunto no puede superar 180 caracteres")
    private String subject;

    private String description;

    @NotNull(message = "Selecciona una prioridad")
    private ActivityPriority priority;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dueAt;

    private Long ownerId;

    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType value) { activityType = value; }
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public ActivityPriority getPriority() { return priority; }
    public void setPriority(ActivityPriority value) { priority = value; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime value) { scheduledAt = value; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime value) { dueAt = value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { ownerId = value; }
}
