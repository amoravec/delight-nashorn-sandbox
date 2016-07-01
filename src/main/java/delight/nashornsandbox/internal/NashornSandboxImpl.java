package delight.nashornsandbox.internal;

import com.google.common.base.Objects;
import delight.async.Value;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.exceptions.ScriptCPUAbuseException;
import delight.nashornsandbox.internal.BeautifyJs;
import delight.nashornsandbox.internal.InterruptTest;
import delight.nashornsandbox.internal.MonitorThread;
import delight.nashornsandbox.internal.SandboxClassFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;

@SuppressWarnings("all")
public class NashornSandboxImpl implements NashornSandbox {
  private final Set<String> allowedClasses;
  
  private final Map<String, Object> globalVariables;
  
  private ScriptEngine scriptEngine;
  
  private Long maxCPUTimeInMs = Long.valueOf(0L);
  
  private ExecutorService exectuor;
  
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
      Set<Map.Entry<String, Object>> _entrySet = this.globalVariables.entrySet();
      for (final Map.Entry<String, Object> entry : _entrySet) {
        String _key = entry.getKey();
        Object _value = entry.getValue();
        this.scriptEngine.put(_key, _value);
      }
      this.scriptEngine.eval((((((((((((((((((((((((((("\n" + 
        "quit = function() {};\n") + 
        "exit = function() {};\n") + 
        "\n") + 
        "print = function() {};\n") + 
        "echo = function() {};\n") + 
        "\n") + 
        "readFully = function() {};\n") + 
        "readLine = function() {};\n") + 
        "\n") + 
        "load = function() {};\n") + 
        "loadWithNewGlobal = function() {};\n") + 
        "\n") + 
        "org = null;\n") + 
        "java = null;\n") + 
        "com = null;\n") + 
        "sun = null;\n") + 
        "net = null;\n") + 
        "\n") + 
        "$ARG = null;\n") + 
        "$ENV = null;\n") + 
        "$EXEC = null;\n") + 
        "$OPTIONS = null;\n") + 
        "$OUT = null;\n") + 
        "$ERR = null;\n") + 
        "$EXIT = null;\n") + 
        ""));
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
        if (((this.maxCPUTimeInMs).longValue() == 0)) {
          return this.scriptEngine.eval(js);
        }
        Object _xsynchronizedexpression = null;
        synchronized (this) {
          Object _xblockexpression_1 = null;
          {
            final Value<Object> resVal = new Value<Object>(null);
            final Value<Throwable> exceptionVal = new Value<Throwable>(null);
            final MonitorThread monitorThread = new MonitorThread(((this.maxCPUTimeInMs).longValue() * 1000000));
            boolean _equals = Objects.equal(this.exectuor, null);
            if (_equals) {
              throw new IllegalStateException(
                "When a CPU time limit is set, an executor needs to be provided by calling .setExecutor(...)");
            }
            final Object monitor = new Object();
            final Runnable _function = new Runnable() {
              @Override
              public void run() {
                try {
                  boolean _contains = js.contains("intCheckForInterruption");
                  if (_contains) {
                    throw new IllegalArgumentException(
                      "Script contains the illegal string [intCheckForInterruption]");
                  }
                  Object _eval = NashornSandboxImpl.this.scriptEngine.eval("window.js_beautify;");
                  final ScriptObjectMirror jsBeautify = ((ScriptObjectMirror) _eval);
                  Object _call = jsBeautify.call("beautify", js);
                  final String beautifiedJs = ((String) _call);
                  Random _random = new Random();
                  int _nextInt = _random.nextInt();
                  final int randomToken = Math.abs(_nextInt);
                  StringConcatenation _builder = new StringConcatenation();
                  _builder.append("var InterruptTest = Java.type(\'");
                  String _name = InterruptTest.class.getName();
                  _builder.append(_name, "");
                  _builder.append("\');");
                  _builder.newLineIfNotEmpty();
                  _builder.append("var isInterrupted = InterruptTest.isInterrupted;");
                  _builder.newLine();
                  _builder.append("var intCheckForInterruption");
                  _builder.append(randomToken, "");
                  _builder.append(" = function() {");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("if (isInterrupted()) {");
                  _builder.newLine();
                  _builder.append("\t    ");
                  _builder.append("throw new Error(\'Interrupted");
                  _builder.append(randomToken, "\t    ");
                  _builder.append("\')");
                  _builder.newLineIfNotEmpty();
                  _builder.append("\t");
                  _builder.append("}");
                  _builder.newLine();
                  _builder.append("};");
                  _builder.newLine();
                  String _replaceAll = beautifiedJs.replaceAll(";\\n", ((";intCheckForInterruption" + Integer.valueOf(randomToken)) + "();\n"));
                  String _replace = _replaceAll.replace(") {", ((") {intCheckForInterruption" + Integer.valueOf(randomToken)) + "();\n"));
                  final String securedJs = (_builder.toString() + _replace);
                  final Thread mainThread = Thread.currentThread();
                  Thread _currentThread = Thread.currentThread();
                  monitorThread.setThreadToMonitor(_currentThread);
                  final Runnable _function = new Runnable() {
                    @Override
                    public void run() {
                      mainThread.interrupt();
                    }
                  };
                  monitorThread.setOnInvalidHandler(_function);
                  monitorThread.start();
                  try {
                    final Object res = NashornSandboxImpl.this.scriptEngine.eval(securedJs);
                    resVal.set(res);
                  } catch (final Throwable _t) {
                    if (_t instanceof ScriptException) {
                      final ScriptException e = (ScriptException)_t;
                      String _message = e.getMessage();
                      boolean _contains_1 = _message.contains(("Interrupted" + Integer.valueOf(randomToken)));
                      if (_contains_1) {
                        monitorThread.notifyOperationInterrupted();
                      } else {
                        exceptionVal.set(e);
                        monitorThread.stopMonitor();
                        synchronized (monitor) {
                          monitor.notify();
                        }
                        return;
                      }
                    } else {
                      throw Exceptions.sneakyThrow(_t);
                    }
                  } finally {
                    monitorThread.stopMonitor();
                    synchronized (monitor) {
                      monitor.notify();
                    }
                  }
                } catch (final Throwable _t_1) {
                  if (_t_1 instanceof Throwable) {
                    final Throwable t = (Throwable)_t_1;
                    exceptionVal.set(t);
                    monitorThread.stopMonitor();
                    synchronized (monitor) {
                      monitor.notify();
                    }
                  } else {
                    throw Exceptions.sneakyThrow(_t_1);
                  }
                }
              }
            };
            this.exectuor.execute(_function);
            synchronized (monitor) {
              monitor.wait();
            }
            boolean _isCPULimitExceeded = monitorThread.isCPULimitExceeded();
            if (_isCPULimitExceeded) {
              String notGraceful = "";
              boolean _gracefullyInterrputed = monitorThread.gracefullyInterrputed();
              boolean _not = (!_gracefullyInterrputed);
              if (_not) {
                notGraceful = " The operation could not be gracefully interrupted.";
              }
              Throwable _get = exceptionVal.get();
              throw new ScriptCPUAbuseException(
                ((("Script used more than the allowed [" + this.maxCPUTimeInMs) + " ms] of CPU time. ") + notGraceful), _get);
            }
            Throwable _get_1 = exceptionVal.get();
            boolean _notEquals = (!Objects.equal(_get_1, null));
            if (_notEquals) {
              throw exceptionVal.get();
            }
            _xblockexpression_1 = resVal.get();
          }
          _xsynchronizedexpression = _xblockexpression_1;
        }
        _xblockexpression = _xsynchronizedexpression;
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Override
  public NashornSandbox setMaxCPUTime(final long limit) {
    NashornSandboxImpl _xblockexpression = null;
    {
      this.maxCPUTimeInMs = Long.valueOf(limit);
      _xblockexpression = this;
    }
    return _xblockexpression;
  }
  
  @Override
  public NashornSandbox allow(final Class<?> clazz) {
    NashornSandboxImpl _xblockexpression = null;
    {
      String _name = clazz.getName();
      this.allowedClasses.add(_name);
      boolean _notEquals = (!Objects.equal(this.scriptEngine, null));
      if (_notEquals) {
        throw new IllegalStateException(
          "eval() was already called. Please specify all classes to be allowed/injected before calling eval()");
      }
      _xblockexpression = this;
    }
    return _xblockexpression;
  }
  
  @Override
  public NashornSandbox inject(final String variableName, final Object object) {
    NashornSandboxImpl _xblockexpression = null;
    {
      this.globalVariables.put(variableName, object);
      Class<?> _class = object.getClass();
      String _name = _class.getName();
      boolean _contains = this.allowedClasses.contains(_name);
      boolean _not = (!_contains);
      if (_not) {
        Class<?> _class_1 = object.getClass();
        this.allow(_class_1);
      }
      boolean _notEquals = (!Objects.equal(this.scriptEngine, null));
      if (_notEquals) {
        this.scriptEngine.put(variableName, object);
      }
      _xblockexpression = this;
    }
    return _xblockexpression;
  }
  
  @Override
  public NashornSandbox setExecutor(final ExecutorService executor) {
    NashornSandboxImpl _xblockexpression = null;
    {
      this.exectuor = executor;
      _xblockexpression = this;
    }
    return _xblockexpression;
  }
  
  @Override
  public ExecutorService getExecutor() {
    return this.exectuor;
  }
  
  @Override
  public Object get(final String variableName) {
    Object _xblockexpression = null;
    {
      this.assertScriptEngine();
      _xblockexpression = this.scriptEngine.get(variableName);
    }
    return _xblockexpression;
  }
  
  public NashornSandboxImpl() {
    HashSet<String> _hashSet = new HashSet<String>();
    this.allowedClasses = _hashSet;
    HashMap<String, Object> _hashMap = new HashMap<String, Object>();
    this.globalVariables = _hashMap;
    this.allow(InterruptTest.class);
  }
}
