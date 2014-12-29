package com.dennytech.common.util;

import java.util.Locale;

/**
 * 自定义日志，运行时根据debuggable状态来关闭。<br>
 * 在调试状态下开启，发布下关闭。
 * <p>
 * 日志级别定义
 * <p>
 * v/verbose：用以打印非常详细的日志，例如如果你需要打印网络请求及返回的数据。<br>
 * d/debug：用以打印便于调试的日志，例如网络请求返回的关键结果或者操作是否成功。<br>
 * i/information：用以打印为以后调试或者运行中提供运行信息的日志，例如进入或退出了某个函数、进入了函数的某个分支等。<br>
 * w/warning：用以打印不太正常但是还不是错误的日志。<br>
 * e/error：用以打印出现错误的日志，一般用以表示错误导致功能无法继续运行。<br>
 * 
 * @author Jun.Deng
 * 
 */
public class Log {

	/**
	 * 默认tag
	 */
	private static String TAG_DEFAULT = "bdlife";

	/**
	 * 当前的输出等级，当n>=LEVEL时才会输出<br>
	 * 如果当前为调试模式，默认应该设置为i/info<br>
	 * 如果想完全禁用输出，如发布，则应该设置为Integer.MAX_VALUE<br>
	 * 默认为完全禁用
	 */
	public static int LEVEL = android.util.Log.VERBOSE;

	/**
	 * v/verbose：用以打印非常详细的日志，例如如果你需要打印网络请求及返回的数据。
	 */
	public static final int VERBOSE = android.util.Log.VERBOSE;

	/**
	 * d/debug：用以打印便于调试的日志，例如网络请求返回的关键结果或者操作是否成功。
	 */
	public static final int DEBUG = android.util.Log.DEBUG;

	/**
	 * i/information：用以打印为以后调试或者运行中提供运行信息的日志，例如进入或退出了某个函数、进入了函数的某个分支等。
	 */
	public static final int INFO = android.util.Log.INFO;

	/**
	 * w/warning：用以打印不太正常但是还不是错误的日志。
	 */
	public static final int WARN = android.util.Log.WARN;

	/**
	 * e/error：用以打印出现错误的日志，一般用以表示错误导致功能无法继续运行。
	 */
	public static final int ERROR = android.util.Log.ERROR;

	/**
	 * 检查当前是否需要输出对应的level。
	 * <p>
	 * 如果需要打印的字符串需要比较复杂的逻辑生成，则建议先使用该方法判断。<br>
	 * 如果直接输出字符串，则无需检查。如Log.d("test", "this is a error");
	 */
	public static boolean isLoggable(int level) {
		return level >= LEVEL;
	}

	/**
	 * v/verbose：用以打印非常详细的日志，例如如果你需要打印网络请求及返回的数据。
	 */
	public static void v(String msg) {
		v(TAG_DEFAULT, msg);
	}

	/**
	 * v/verbose：用以打印非常详细的日志，例如如果你需要打印网络请求及返回的数据。
	 */
	public static void v(String tag, String msg) {
		v(tag, msg, null);
	}

	/**
	 * v/verbose：用以打印非常详细的日志，例如如果你需要打印网络请求及返回的数据。
	 * <p>
	 * 附带具体的Exception，一般必须带有tag。不允许在默认的tag中输出Exception
	 */
	public static void v(String tag, String msg, Throwable e) {
		if (VERBOSE >= LEVEL) {
			android.util.Log.v(tag, buildMessage(msg), e);
		}
	}

	/**
	 * d/debug：用以打印便于调试的日志，例如网络请求返回的关键结果或者操作是否成功。
	 */
	public static void d(String msg) {
		d(TAG_DEFAULT, msg);
	}

	/**
	 * d/debug：用以打印便于调试的日志，例如网络请求返回的关键结果或者操作是否成功。
	 */
	public static void d(String tag, String msg) {
		d(tag, msg, null);
	}

	/**
	 * d/debug：用以打印便于调试的日志，例如网络请求返回的关键结果或者操作是否成功。
	 * <p>
	 * 附带具体的Exception，一般必须带有tag。不允许在默认的tag中输出Exception
	 */
	public static void d(String tag, String msg, Throwable e) {
		if (DEBUG >= LEVEL) {
			android.util.Log.d(tag, buildMessage(msg), e);
		}
	}

	/**
	 * i/information：用以打印为以后调试或者运行中提供运行信息的日志，例如进入或退出了某个函数、进入了函数的某个分支等。
	 */
	public static void i(String msg) {
		i(TAG_DEFAULT, msg);
	}

	/**
	 * i/information：用以打印为以后调试或者运行中提供运行信息的日志，例如进入或退出了某个函数、进入了函数的某个分支等。
	 */
	public static void i(String tag, String msg) {
		i(tag, msg, null);
	}

	/**
	 * i/information：用以打印为以后调试或者运行中提供运行信息的日志，例如进入或退出了某个函数、进入了函数的某个分支等。
	 * <p>
	 * 附带具体的Exception，一般必须带有tag。不允许在默认的tag中输出Exception
	 */
	public static void i(String tag, String msg, Throwable e) {
		if (INFO >= LEVEL) {
			android.util.Log.i(tag, buildMessage(msg), e);
		}
	}

	/**
	 * w/warning：用以打印不太正常但是还不是错误的日志。
	 */
	public static void w(String msg) {
		w(TAG_DEFAULT, msg);
	}

	/**
	 * w/warning：用以打印不太正常但是还不是错误的日志。
	 */
	public static void w(String tag, String msg) {
		w(tag, msg, null);
	}

	/**
	 * w/warning：用以打印不太正常但是还不是错误的日志。
	 * <p>
	 * 附带具体的Exception，一般必须带有tag。不允许在默认的tag中输出Exception
	 */
	public static void w(String tag, String msg, Throwable e) {
		if (WARN >= LEVEL) {
			android.util.Log.w(tag, buildMessage(msg), e);
		}
	}

	/**
	 * e/error：用以打印出现错误的日志，一般用以表示错误导致功能无法继续运行。
	 */
	public static void e(String msg) {
		e(TAG_DEFAULT, msg);
	}

	/**
	 * e/error：用以打印出现错误的日志，一般用以表示错误导致功能无法继续运行。
	 */
	public static void e(String tag, String msg) {
		e(tag, msg, null);
	}

	/**
	 * e/error：用以打印出现错误的日志，一般用以表示错误导致功能无法继续运行。
	 * <p>
	 * 附带具体的Exception，一般必须带有tag。不允许在默认的tag中输出Exception
	 */
	public static void e(String tag, String msg, Throwable e) {
		if (ERROR >= LEVEL) {
			android.util.Log.e(tag, buildMessage(msg), e);
		}
	}

	/**
	 * 格式化需要打印的消息，并且提供一些格外的追踪信息（比如thread ID、方法名）
	 * 
	 * @param format
	 * @param args
	 * @return
	 */
	private static String buildMessage(String msg) {
//		StackTraceElement[] trace = new Throwable().fillInStackTrace()
//				.getStackTrace();
//
//		String caller = "<unknown>";
//		// 追踪进行Log操作前面的步骤，可以定位到Log发生的位置
//		for (int i = 2; i < trace.length; i++) {
//			String callingClass = trace[i].getClassName();
//			if (!callingClass.equals(Log.class.getName())) {
//				callingClass = callingClass.substring(callingClass
//						.lastIndexOf('.') + 1);
//				callingClass = callingClass.substring(callingClass
//						.lastIndexOf('$') + 1);
//
//				caller = callingClass + "." + trace[i].getMethodName();
//				break;
//			}
//		}
//		return String.format(Locale.CHINA, "[%d] %s: %s", Thread
//				.currentThread().getId(), caller, msg);
		return msg;
	}
}
