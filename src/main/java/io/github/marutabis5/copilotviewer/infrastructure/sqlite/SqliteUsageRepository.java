package io.github.marutabis5.copilotviewer.infrastructure.sqlite;

import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.repository.UsageRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.YearMonth;

/**
 * Future {@link UsageRepository} implementation backed by SQLite.
 *
 * <p><strong>Not used in the MVP.</strong> This stub is provided so that the
 * data-source selection logic in {@link io.github.marutabis5.copilotviewer.service.CopilotUsageService}
 * can be wired to SQLite for past months without structural changes.</p>
 *
 * <p>Implementation notes (for future sprint):</p>
 * <ul>
 *   <li>Use {@code org.xerial:sqlite-jdbc} (already on the classpath) for raw JDBC access.</li>
 *   <li>Store {@code (org, login, date, product, sku, model, unitType,
 *       grossQuantity, discountQuantity, netQuantity, netAmount)} rows.</li>
 *   <li>On a cache miss, fall back to
 *       {@link io.github.marutabis5.copilotviewer.infrastructure.github.GitHubApiUsageRepository},
 *       persist the result, then return it.</li>
 * </ul>
 */
@ApplicationScoped
public class SqliteUsageRepository implements UsageRepository {

    @Override
    public MonthlyUsageReport findByOrgAndUserAndMonth(String org,
                                                       String login,
                                                       YearMonth yearMonth) {
        throw new UnsupportedOperationException(
                "SQLite repository is not implemented yet. "
                + "Wire GitHubApiUsageRepository for all months in the MVP.");
    }
}
