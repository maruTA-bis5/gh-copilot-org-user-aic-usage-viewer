package io.github.marutabis5.copilotviewer.infrastructure.github;

import io.github.marutabis5.copilotviewer.infrastructure.github.dto.AiCreditUsageResponse;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * MicroProfile REST Client for the GitHub Billing AI Credit Usage API.
 *
 * <p>Base URL is configured via {@code github-billing/mp-rest/url} in
 * {@code microprofile-config.properties} (default: {@code https://api.github.com}).</p>
 *
 * <p>Required PAT is read at call time from {@code github.pat} MicroProfile Config property.</p>
 */
@RegisterRestClient(configKey = "github-billing")
@ClientHeaderParam(name = "Accept",           value = "application/vnd.github+json")
@ClientHeaderParam(name = "X-GitHub-Api-Version", value = "2026-03-10")
@ClientHeaderParam(name = "Authorization",    value = "{lookupAuthHeader}")
public interface GitHubBillingClient extends AutoCloseable {

    /**
     * Fetches AI credit usage for a single calendar day.
     *
     * @param org   organisation name
     * @param year  four-digit year
     * @param month month (1–12)
     * @param day   day of month (1–31)
     * @param user  GitHub login to filter on (case-insensitive)
     * @return the parsed response; never {@code null}
     */
    @GET
    @Path("/organizations/{org}/settings/billing/ai_credit/usage")
    @Produces(MediaType.APPLICATION_JSON)
    AiCreditUsageResponse getAiCreditUsage(
            @PathParam("org")   String org,
            @QueryParam("year") int    year,
            @QueryParam("month") int   month,
            @QueryParam("day")  int    day,
            @QueryParam("user") String user
    );

    /**
     * Resolved at call-time via MicroProfile Config so the token is never
     * hard-coded and never appears in logs or error messages.
     */
    default String lookupAuthHeader() {
        String pat = ConfigProvider.getConfig().getValue("github.pat", String.class);
        return "Bearer " + pat;
    }
}
