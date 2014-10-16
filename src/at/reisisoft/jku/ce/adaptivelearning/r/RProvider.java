package at.reisisoft.jku.ce.adaptivelearning.r;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Optional;

import javax.script.ScriptException;

import rcaller.Globals;
import rcaller.RCaller;
import rcaller.RCaller.FailurePolicy;
import rcaller.RCode;
import rcaller.ROutputParser;
import at.reisisoft.jku.ce.adaptivelearning.core.LogHelper;

public class RProvider {

	private String rScript_exe;
	private final ByteArrayOutputStream byteArrayOutputStream;

	public RProvider() throws ScriptException {
		byteArrayOutputStream = new ByteArrayOutputStream();
		// Guess R location
		Globals.detect_current_rscript();
		StringBuilder rPath = new StringBuilder(Globals.R_current);
		// Guessing is not good on Windows, DIY
		if (System.getProperty("os.name").startsWith("Win")) {
			rPath.setLength(0);
			rPath.append("C:\\Program Files\\R");
			File rFolder = new File(rPath.toString());

			File[] subfolders = rFolder
					.listFiles((FileFilter) pathname -> pathname.isDirectory());

			Optional<String> max = Arrays.asList(subfolders).stream()
					.map(e -> e.getName()).max((e1, e2) -> e1.compareTo(e2));

			if (!max.isPresent()) {
				throw new ScriptException("R is not present @ " + rPath);

			}
			rPath.append('\\').append(max.get()).append('\\').append("bin\\");

			if (!"x86".equals(System.getProperty("os.arch"))) {
				// 64 bit VM -> 64 bit R required
				rPath.append("x64\\");
			}
			rScript_exe = rPath.append("Rscript.exe").toString();
		}
	}

	public RCaller getRCaller() throws ScriptException {
		RCaller caller = new RCaller();
		caller.setRscriptExecutable(rScript_exe);
		caller.setFailurePolicy(FailurePolicy.RETRY_1);
		return caller;
	}

	public RCode getRCode() {
		return new RCode();
	}

	public void run(RCaller caller, RCode code) throws ScriptException {
		caller.setRCode(code);
		synchronized (byteArrayOutputStream) {
			try {
				byteArrayOutputStream.reset();
				caller.redirectROutputToStream(byteArrayOutputStream);
				caller.runOnly();
			} catch (Exception e) {
				LogHelper.logRError(byteArrayOutputStream.toString());
				throw new ScriptException(e);
			} finally {
				caller.StopRCallerOnline();
			}
		}

	}

	public ROutputParser execute(RCaller caller, RCode code, String toReturn)
			throws ScriptException {
		caller.setRCode(code);
		synchronized (byteArrayOutputStream) {
			try {
				byteArrayOutputStream.reset();
				caller.redirectROutputToStream(byteArrayOutputStream);
				caller.runAndReturnResult(toReturn);
			} catch (Exception e) {
				LogHelper.logRError(byteArrayOutputStream.toString());
				throw new ScriptException(e);
			} finally {
				caller.StopRCallerOnline();
			}
		}

		return caller.getParser();
	}
}