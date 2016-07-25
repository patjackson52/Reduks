package com.beyondeye.reduks.logger.logformatter;

import com.beyondeye.reduks.logger.LogLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

final class LoggerPrinter implements Printer {

    private static final String DEFAULT_TAG = "PRETTYLOGGER";

//    private static final int DEBUG = 3;
//    private static final int ERROR = 6;
//    private static final int ASSERT = 7;
//    private static final int INFO = 4;
//    private static final int VERBOSE = 2;
//    private static final int WARN = 5;

    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */
    private static final int CHUNK_SIZE = 4000;

    /**
     * It is used for json pretty print
     */
    private static final int JSON_INDENT = 2;

    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 3;

    /**
     * Drawing toolbox
     */
    private static final char TOP_LEFT_CORNER = '╔';
    private static final char BOTTOM_LEFT_CORNER = '╚';
    private static final char MIDDLE_CORNER = '╟';
    private static final char HORIZONTAL_DOUBLE_LINE = '║';
    private static final String HORIZONTAL_DOUBLE_LINE_STR = HORIZONTAL_DOUBLE_LINE+" ";
    private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
    private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;

    /**
     * single level group blanks
     */
    private static final String SINGLEGROUPBLANKS = "  ";

    /**
     * tag is used for the Log, the name is a little different
     * in order to differentiate the logs easily with the filter
     */
    private String tag;

    /**
     * Localize single tag and method count and groupBlanks for each thread
     */
    private final ThreadLocal<String> localTag = new ThreadLocal<String>();
    private final ThreadLocal<Integer> localMethodCount = new ThreadLocal<Integer>();
    private final ThreadLocal<String> groupBlanks = new ThreadLocal<String>(); //TODO it's wrong to make it thread local since reducer/subscribers can operate from multiple threads?

    /**
     * It is used to determine log settings such as method count, thread info visibility
     */
    private final Settings settings = new Settings();

    public LoggerPrinter() {
        init(DEFAULT_TAG);
    }

    /**
     * It is used to change the tag
     *
     * @param tag is the given string which will be used in Logger
     */
    @Override
    public Settings init(String tag) {
        if (tag == null) {
            throw new NullPointerException("tag may not be null");
        }
        if (tag.trim().length() == 0) {
            throw new IllegalStateException("tag may not be empty");
        }
        this.tag = tag;
        return settings;
    }

    @Override
    public Settings getSettings() {
        return settings;
    }

    public void groupStart() {
        String curGroupBlanks = groupBlanks.get();
        groupBlanks.set(curGroupBlanks + SINGLEGROUPBLANKS);
    }

    public void groupEnd() {
        String curGroupBlanks = groupBlanks.get();
        int newlength = curGroupBlanks.length() - SINGLEGROUPBLANKS.length();
        if (newlength >= 0) {
            String newGroupBlanks = curGroupBlanks.substring(0, newlength);
            groupBlanks.set(newGroupBlanks);
        }
    }

    @Override
    public Printer t(String tag, int methodCount) {
        if (tag != null) {
            localTag.set(tag);
        }
        localMethodCount.set(methodCount);
        return this;
    }

//    @Override
//    public void d(String message, Object... args) {
//        log(DEBUG, null, message, args);
//    }


//    @Override
//    public void d(Object object) {
//        String message;
//        if (object.getClass().isArray()) {
//            message = Arrays.deepToString((Object[]) object);
//        } else {
//            message = object.toString();
//        }
//        log(DEBUG, null, message);
//    }

//    @Override
//    public void e(String message, Object... args) {
//        e(null, message, args);
//    }

//    @Override
//    public void e(Throwable throwable, String message, Object... args) {
//        log(ERROR, throwable, message, args);
//    }

//    @Override
//    public void w(String message, Object... args) {
//        log(WARN, null, message, args);
//    }

//    @Override
//    public void i(String message, Object... args) {
//        log(INFO, null, message, args);
//    }

//    @Override
//    public void v(String message, Object... args) {
//        log(VERBOSE, null, message, args);
//    }

//    @Override
//    public void wtf(String message, Object... args) {
//        log(ASSERT, null, message, args);
//    }
    private void d(String message) {
        log(LogLevel.DEBUG, null, message, null);
    }

    private void e(String message) {
        log(LogLevel.ERROR, null, message, null);
    }

    /**
     * Formats the json content and print it
     *
     * @param json the json content
     */
    @Override
    public void json(String json) {
        if (Helper.isEmpty(json)) {
            d("Empty/Null json content");
            return;
        }
        try {
            json = json.trim();
            if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                String message = jsonObject.toString(JSON_INDENT);
                d(message);
                return;
            }
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                String message = jsonArray.toString(JSON_INDENT);
                d(message);
                return;
            }
            e("Invalid Json");
        } catch (JSONException e) {
            e("Invalid Json");
        }
    }

    //TODO remove synchronized from here and put on reduks_logger printBuffer
    @Override
    public synchronized void log(int loglevel, String tag, String message, Throwable throwable) {
        if (!settings.getLogEnabled()) {
            return;
        }
        if (throwable != null && message != null) {
            message += " : " + Helper.getStackTraceString(throwable);
        }
        if (throwable != null && message == null) {
            message = Helper.getStackTraceString(throwable);
        }
        if (message == null) {
            message = "No message/exception is set";
        }
        int methodCount = getMethodCount();
        if (Helper.isEmpty(message)) {
            message = "Empty/NULL log message";
        }

        logTopBorder(loglevel, tag);
        logHeaderContent(loglevel, tag, methodCount);

        //get bytes of message with system's default charset (which is UTF-8 for Android)
        byte[] bytes = message.getBytes();
        int length = bytes.length;
        if (length <= CHUNK_SIZE) {
            if (methodCount > 0) {
                logDivider(loglevel, tag);
            }
            logContent(loglevel, tag, message);
            logBottomBorder(loglevel, tag);
            return;
        }
        if (methodCount > 0) {
            logDivider(loglevel, tag);
        }
        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int count = Math.min(length - i, CHUNK_SIZE);
            //create a new String with system's default charset (which is UTF-8 for Android)
            logContent(loglevel, tag, new String(bytes, i, count));
        }
        logBottomBorder(loglevel, tag);
    }

    @Override
    public void resetSettings() {
        settings.reset();
    }

//    /**
//     * This method is synchronized in order to avoid messy of logs' order.
//     */
//    private synchronized void log(int priority, Throwable throwable, String msg, Object... args) {
//        if (!settings.getLogEnabled()) {
//            return;
//        }
//        String tag = getTag();
//        String message = createMessage(msg, args);
//        log(priority, tag, message, throwable);
//    }

    private void logTopBorder(int logType, String tag) {
        if (!settings.isBorderEnabled()) return;
        logChunk(logType, tag, TOP_BORDER);
    }
    private String HorizontalDoubleLine() {
        return settings.isBorderEnabled() ? HORIZONTAL_DOUBLE_LINE_STR : "";
    }
    @SuppressWarnings("StringBufferReplaceableByString")
    private void logHeaderContent(int logType, String tag, int methodCount) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (settings.isShowThreadInfo()) {
            logChunk(logType, tag, HorizontalDoubleLine() + "Thread: " + Thread.currentThread().getName());
            logDivider(logType, tag);
        }
        String level = "";

        int stackOffset = getStackOffset(trace) + settings.getMethodOffset();

        //corresponding method count with the current stack may exceeds the stack trace. Trims the count
        if (methodCount + stackOffset > trace.length) {
            methodCount = trace.length - stackOffset - 1;
        }

        for (int i = methodCount; i > 0; i--) {
            int stackIndex = i + stackOffset;
            if (stackIndex >= trace.length) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("║ ")
                    .append(level)
                    .append(getSimpleClassName(trace[stackIndex].getClassName()))
                    .append(".")
                    .append(trace[stackIndex].getMethodName())
                    .append(" ")
                    .append(" (")
                    .append(trace[stackIndex].getFileName())
                    .append(":")
                    .append(trace[stackIndex].getLineNumber())
                    .append(")");
            level += "   ";
            logChunk(logType, tag, builder.toString());
        }
    }

    private void logBottomBorder(int logType, String tag) {
        if (!settings.isBorderEnabled()) return;
        logChunk(logType, tag, BOTTOM_BORDER);
    }

    private void logDivider(int logType, String tag) {
        if (!settings.isBorderEnabled()) return;
        logChunk(logType, tag, MIDDLE_BORDER);
    }

    private void logContent(int logType, String tag, String chunk) {
        String[] lines = chunk.split(System.getProperty("line.separator"));
        for (String line : lines) {
            logChunk(logType, tag, HorizontalDoubleLine() + line);
        }
    }

    private void logChunk(int logType, String tag, String chunk) {
        String finalTag = formatTag(tag);
        switch (logType) {
            case LogLevel.ERROR:
                settings.getLogAdapter().e(finalTag, chunk);
                break;
            case LogLevel.INFO:
                settings.getLogAdapter().i(finalTag, chunk);
                break;
            case LogLevel.VERBOSE:
                settings.getLogAdapter().v(finalTag, chunk);
                break;
            case LogLevel.WARN:
                settings.getLogAdapter().w(finalTag, chunk);
                break;
            case LogLevel.ASSERT:
                settings.getLogAdapter().wtf(finalTag, chunk);
                break;
            case LogLevel.DEBUG:
                // Fall through, log debug by default
            default:
                settings.getLogAdapter().d(finalTag, chunk);
                break;
        }
    }

    private String getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    private String formatTag(String tag) {
        if (!Helper.isEmpty(tag) && !Helper.equals(this.tag, tag)) {
            return this.tag + "-" + tag + groupBlanks;
        }
        return this.tag;
    }

    /**
     * @return the appropriate tag based on local or global
     */
    private String getTag() {
        String tag = localTag.get();
        if (tag != null) {
            localTag.remove();
            return tag;
        }
        return this.tag;
    }

//    private String createMessage(String message, Object... args) {
//        return args == null || args.length == 0 ? message : String.format(message, args);
//    }

    private int getMethodCount() {
        Integer count = localMethodCount.get();
        int result = settings.getMethodCount();
        if (count != null) {
            localMethodCount.remove();
            result = count;
        }
        if (result < 0) {
            throw new IllegalStateException("methodCount cannot be negative");
        }
        return result;
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            String name = e.getClassName();
            if (!name.equals(LoggerPrinter.class.getName()) && !name.equals(Logger.class.getName())) {
                return --i;
            }
        }
        return -1;
    }

}