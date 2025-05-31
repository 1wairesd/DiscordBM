package com.wairesd.discordbm.common.utils.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaPluginLogger implements PluginLogger {
    private final Logger logger;

    public JavaPluginLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String s) {
        this.logger.info(s);
    }

    public void info(String s, Throwable t) {
        this.logger.log(Level.INFO, s, t);
    }

    @Override
    public void info(String s, Object... args) {
        this.logger.info(String.format(s, args));
    }

    @Override
    public void warn(String s) {
        this.logger.warning(s);
    }

    @Override
    public void warn(String s, Throwable t) {
        this.logger.log(Level.WARNING, s, t);
    }

    @Override
    public void warn(String s, Object... args) {
        this.logger.log(Level.WARNING, String.format(s, args));
    }

    @Override
    public void error(String s) {
        this.logger.log(Level.SEVERE, s);
    }

    @Override
    public void error(String s, Throwable t) {
        this.logger.log(Level.SEVERE, s, t);
    }

    @Override
    public void error(String s, Object... args) {
        this.logger.log(Level.SEVERE, String.format(s, args));
    }
}