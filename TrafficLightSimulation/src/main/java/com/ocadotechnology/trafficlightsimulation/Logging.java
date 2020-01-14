package com.ocadotechnology.trafficlightsimulation;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import com.ocadotechnology.event.EventUtil;
import com.ocadotechnology.time.TimeProvider;

public class Logging {
    private static TimeProvider timeProvider;

    public static void configure(TimeProvider timeProvider) {
        Logging.timeProvider = timeProvider;

        Logger rootLogger = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = rootLogger.getLoggerContext();
        loggerContext.reset();

        PatternLayout patternLayout = new PatternLayout();
        patternLayout.getDefaultConverterMap().put("st", SimTimeConverter.class.getName());

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{ISO8601} %-5p %st | %m [%C{0}]%n");
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.start();

        rootLogger.addAppender(appender);
        rootLogger.setLevel(Level.INFO);
    }

    public static class SimTimeConverter extends ClassicConverter {
        @Override
        public String convert(ILoggingEvent event) {
            return EventUtil.eventTimeToString(timeProvider.getTime());
        }
    }
}
