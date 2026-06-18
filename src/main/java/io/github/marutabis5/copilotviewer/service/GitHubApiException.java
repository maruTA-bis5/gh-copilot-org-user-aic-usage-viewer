package io.github.marutabis5.copilotviewer.service;

/**
 * Thrown when the GitHub Billing API returns an error after all retry attempts.
 * Carries the HTTP status code and a safe, sanitised summary of the error
 * (no tokens, no internal stack traces exposed to the UI).
 */
public class GitHubApiException extends RuntimeException {

    private final int httpStatus;
    private final String summary;

    public GitHubApiException(int httpStatus, String summary) {
        super("GitHub API error HTTP %d: %s".formatted(httpStatus, summary));
        this.httpStatus = httpStatus;
        this.summary = summary;
    }

    public GitHubApiException(int httpStatus, String summary, Throwable cause) {
        super("GitHub API error HTTP %d: %s".formatted(httpStatus, summary), cause);
        this.httpStatus = httpStatus;
        this.summary = summary;
    }

    /** The HTTP status code returned by GitHub (e.g. 403, 404, 429, 500). */
    public int getHttpStatus() { return httpStatus; }

    /**
     * A brief, user-safe description of the error.
     * Never contains PAT tokens, response bodies, or internal stack traces.
     */
    public String getSummary() { return summary; }
}
