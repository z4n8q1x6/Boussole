package tn.esprit.boussole.models;

import java.time.LocalDateTime;

public class AlertReport {
    private Long id;
    private String url;
    private LocalDateTime generatedAt;
    private int alertCount;

    public AlertReport() {}

    public AlertReport(String url, LocalDateTime generatedAt, int alertCount) {
        this.url = url;
        this.generatedAt = generatedAt;
        this.alertCount = alertCount;
    }

    public AlertReport(Long id, String url, LocalDateTime generatedAt, int alertCount) {
        this.id = id;
        this.url = url;
        this.generatedAt = generatedAt;
        this.alertCount = alertCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(int alertCount) {
        this.alertCount = alertCount;
    }

    @Override
    public String toString() {
        return "AlertReport{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", generatedAt=" + generatedAt +
                ", alertCount=" + alertCount +
                '}';
    }
}
