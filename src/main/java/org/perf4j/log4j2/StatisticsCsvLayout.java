package org.perf4j.log4j2;

import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.AbstractCsvLayout;
import org.apache.logging.log4j.core.layout.CsvParameterLayout;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.GroupedTimingStatisticsCsvFormatter;
import org.perf4j.helpers.MiscUtils;

/**
 * A layout that outputs {@link org.perf4j.GroupedTimingStatistics} instances as
 * comma-separated values. Thus, this layout is designed to be attached to
 * appenders that are themselves attached to an
 * {@link AsyncCoalescingStatisticsAppender}.
 * <p>
 * By default, each GroupedTimingStatistics object is output as a bunch of
 * lines, with one line for each tagged {@link org.perf4j.TimingStatistics}
 * instance contained within the GroupedTimingStatistics object. The following
 * "columns" are output, separated by commas:
 * <ol>
 * <li>tag - the tag name of the code block that the statistics refer to
 * <li>start - the start time of timing window
 * <li>stop - the stop time of the timing window
 * <li>mean - the mean execution time of stop watch logs that completed in the
 * timing window
 * <li>min - the min execution time of stop watch logs that completed in the
 * timing window
 * <li>max - the max execution time of stop watch logs that completed in the
 * timing window
 * <li>stddev - the standard deviation of the execution times of stop watch logs
 * that completed in the timing window
 * <li>count - the count of stop watch logs that completed during the timing
 * window
 * </ol>
 * <p>
 * You can modify the columns output using the <b>Columns</b> option. For
 * example, you could specify the Columns option as "tag,start,stop,mean,count"
 * to only output those specified values. In addition to the values specified
 * above you can also use "tps" to output transactions per second.
 * <p>
 * In addition to the default output of one line per tag for each
 * GroupedTimingStatistics object, this layout also supports a <b>Pivot</b>
 * option which outputs just a single line for an entire GroupedTimingStatistics
 * object. When pivot is true you should set the Columns to specify the values
 * from the specific tags you want to output. For example, setting Pivot to true
 * and setting Columns to "start,stop,codeBlock1Mean,codeBlock2Mean" would
 * cause, for each GroupedTimingStatistics object, a single line to be output
 * with the start and stop times of the window, the mean execution time for all
 * stop watch logs with a codeBlock1 tag, and the mean execution time for all
 * stop watch logs with a codeBlock2 tag.
 *
 * @author Alex Devine
 */
public class StatisticsCsvLayout extends AbstractCsvLayout {

	public static AbstractCsvLayout createDefaultLayout() {
		return new StatisticsCsvLayout(null, Charset.forName(DEFAULT_CHARSET), CSVFormat.valueOf(DEFAULT_FORMAT), null, null);
	}

	public static AbstractCsvLayout createLayout(final CSVFormat format) {
		return new StatisticsCsvLayout(null, Charset.forName(DEFAULT_CHARSET), format, null, null);
	}

	public StatisticsCsvLayout(final Configuration config, final Charset charset, final CSVFormat csvFormat,
			final String header, final String footer) {
		super(config, charset, csvFormat, header, footer);
	}

	// --- configuration options ---
	/**
	 * Pivot option
	 */
	private boolean pivot = false;
	/**
	 * Columns option, a comma-separated list of column values to output.
	 */
	private String columns = GroupedTimingStatisticsCsvFormatter.DEFAULT_FORMAT_STRING;
	/**
	 * PrintNotStatistics option
	 */
	private boolean printNonStatistics = false;

	// --- contained objects ---
	/**
	 * The csvFormatter is created in the {@link #activateOptions} method. The work
	 * of actually formatting the GroupedTimingStatistics object is delegated to
	 * this object.
	 */
	protected GroupedTimingStatisticsCsvFormatter csvFormatter;

	// --- configuration options ---

	/**
	 * The <b>Pivot</b> option, which is false by default, determines whether or not
	 * a single line will be output for each GroupedTimingStatistics object, or
	 * whether one line for each tag within a GroupedTimingStatistics object will be
	 * output.
	 *
	 * @return the Pivot option.
	 */
	public boolean isPivot() {
		return pivot;
	}

	/**
	 * Sets the value of the <b>Pivot</b> option.
	 *
	 * @param pivot The new Pivot option value.
	 */
	public void setPivot(boolean pivot) {
		this.pivot = pivot;
	}

	/**
	 * The <b>Columns</b> option is a comma-separated list of the values that should
	 * be output for each line that is printed. See the class javadoc for the
	 * allowed value.
	 *
	 * @return the Columns option.
	 */
	public String getColumns() {
		return columns;
	}

	/**
	 * Sets the value of the <b>Columns</b> option.
	 *
	 * @param columns The new Columns option value.
	 */
	public void setColumns(String columns) {
		this.columns = columns;
	}

	/**
	 * Gets the value of the <b>PrintNonStatistics</b> option. In general, this
	 * layout should only be used for appenders that deal with
	 * GroupedTimingStatistics objects (e.g. a FileAppender attached to an
	 * {@link AsyncCoalescingStatisticsAppender}). By default, any logging event
	 * where the message is NOT a GroupedTimingStatistics object is not output.
	 * However, if this option is set to true, then non-GroupedTimingStatistics
	 * messages will be output as their string value.
	 *
	 * @return the PrintNonStatistics option
	 */
	public boolean isPrintNonStatistics() {
		return printNonStatistics;
	}

	/**
	 * Sets the value of the <b>PrintNonStatistics</b> option.
	 *
	 * @param printNonStatistics The new PrintNonStatistics option value.
	 */
	public void setPrintNonStatistics(boolean printNonStatistics) {
		this.printNonStatistics = printNonStatistics;
	}

	public String format(Log4jLogEvent event) {
		try {
			// we assume that the event is a GroupedTimingStatistics object
			return csvFormatter.format((GroupedTimingStatistics) event.getMessage());
		} catch (ClassCastException cce) {
			// then it's not a GroupedTimingStatistics object
			if (isPrintNonStatistics()) {
				return MiscUtils.escapeStringForCsv(event.getMessage().toString(), new StringBuilder())
						.append(MiscUtils.NEWLINE).toString();
			} else {
				return "";
			}
		}
	}

	/**
	 * This layout ignores Throwables set on the LoggingEvent.
	 *
	 * @return true
	 */
	public boolean ignoresThrowable() {
		return true;
	}

	public void activateOptions() {
		csvFormatter = new GroupedTimingStatisticsCsvFormatter(isPivot(), getColumns());
	}

	@Override
	public String toSerializable(LogEvent event) {
		return null;
	}
}
