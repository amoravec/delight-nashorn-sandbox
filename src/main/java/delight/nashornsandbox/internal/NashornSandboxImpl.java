package delight.nashornsandbox.internal;

import com.google.common.base.Objects;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.internal.BeautifyJs;
import delight.nashornsandbox.internal.InterruptTest;
import delight.nashornsandbox.internal.MonitorThread;
import delight.nashornsandbox.internal.SandboxClassFilter;
import java.util.HashSet;
import java.util.Set;
import javax.script.ScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;

@SuppressWarnings("all")
public class NashornSandboxImpl implements NashornSandbox {
  private final Set<String> allowedClasses;
  
  private ScriptEngine scriptEngine;
  
  private Integer maxCPUTimeInMs = Integer.valueOf(0);
  
  public void assertScriptEngine() {
    try {
      boolean _notEquals = (!Objects.equal(this.scriptEngine, null));
      if (_notEquals) {
        return;
      }
      final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
      SandboxClassFilter _sandboxClassFilter = new SandboxClassFilter(this.allowedClasses);
      ScriptEngine _scriptEngine = factory.getScriptEngine(_sandboxClassFilter);
      this.scriptEngine = _scriptEngine;
      this.scriptEngine.eval("var window = {};");
      this.scriptEngine.eval(BeautifyJs.CODE);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Override
  public Object eval(final String js) {
    try {
      Object _xblockexpression = null;
      {
        this.assertScriptEngine();
        if (((this.maxCPUTimeInMs).intValue() == 0)) {
          return this.scriptEngine.eval(js);
        }
        Thread _currentThread = Thread.currentThread();
        final Runnable _function = new Runnable() {
          @Override
          public void run() {
            Thread _currentThread = Thread.currentThread();
            _currentThread.interrupt();
          }
        };
        final MonitorThread monitorThread = new MonitorThread(((this.maxCPUTimeInMs).intValue() * 1000), _currentThread, _function);
        Object _eval = this.scriptEngine.eval("window.js_beautify;");
        final ScriptObjectMirror jsBeautify = ((ScriptObjectMirror) _eval);
        Object _call = jsBeautify.call("beautify", js);
        final String beautifiedJs = ((String) _call);
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("var InterruptTest = var Java.type(\'");
        String _name = InterruptTest.class.getName();
        _builder.append(_name, "");
        _builder.append("\');");
        _builder.newLineIfNotEmpty();
        final String securedJs = (_builder.toString() + beautifiedJs);
        this.scriptEngine.eval(js);
        final Object res = this.scriptEngine.eval(js);
        monitorThread.stopMonitor();
        _xblockexpression = res;
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Override
  public void setMaxCPUTime(final int limit) {
    this.maxCPUTimeInMs = Integer.valueOf(limit);
  }
  
  @Override
  public void allow(final Class<?> clazz) {
    String _name = clazz.getName();
    this.allowedClasses.add(_name);
    this.scriptEngine = null;
  }
  
  public NashornSandboxImpl() {
    HashSet<String> _hashSet = new HashSet<String>();
    this.allowedClasses = _hashSet;
    this.allow(InterruptTest.class);
  }
}