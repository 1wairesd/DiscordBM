package com.wairesd.discordbm.common.utils.logging;

public interface PluginLogger {

    void info(String s);

    void info(String s, Throwable t);

    void info(String s, Object... args);

    void warn(String s);

    void warn(String s, Throwable t);

    void warn(String s, Object... args);

    void error(String s);

    void error(String s, Throwable t);

    void error(String s, Object... args);

}