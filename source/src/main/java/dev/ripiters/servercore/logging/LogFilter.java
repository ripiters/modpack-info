package dev.ripiters.servercore.logging;

import dev.ripiters.servercore.config.ServerCoreConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.Filterable;

import java.util.List;

public class LogFilter extends AbstractFilter {

    public LogFilter() {
        start();
    }

    public static void init() {
        LogFilter filter = new LogFilter();
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        config.getRootLogger().addFilter(filter);
        config.getAppenders().values().forEach(appender -> {
            if (appender instanceof Filterable filterable) {
                filterable.addFilter(filter);
            }
        });

        ctx.updateLoggers();
    }

    @Override
    public Filter.Result filter(LogEvent event) {
        if (event == null || event.getMessage() == null) {
            return Filter.Result.NEUTRAL;
        }

        String msg = event.getMessage().getFormattedMessage();

        try {
            List<? extends String> keywords = ServerCoreConfig.COMMON.suppressedLogs.get();
            if (keywords != null) {
                for (String keyword : keywords) {
                    if (!keyword.isBlank() && msg.contains(keyword)) {
                        return Filter.Result.DENY;
                    }
                }
            }
        } catch (Exception ignored) {}

        return Filter.Result.NEUTRAL;
    }
}