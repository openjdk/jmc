
class StackTrace {
  constructor(stackTraceData) {
    const frames = (stackTraceData && stackTraceData.frames) || [];
    this.frames = frames.map(frame => new StackFrame(frame));
  }
}

class StackFrame {
  constructor({ name }) {
    this.id = name;
    const [fqcn, method] = name.split("#");
    const lastDelimiterIdx = fqcn.lastIndexOf(".");
    this.package = new Package(fqcn.slice(0, lastDelimiterIdx));
    this.clazz = fqcn.slice(lastDelimiterIdx + 1);
    this.method = method.split("(")[0];
  }
}

class Package {
  constructor(name) {
    this.name = name;
  }

  subpackages(maxDepth) {
    const parts = this.name.split(".");
    const subpackages = parts.reduce((acc, current, idx) => {
      if (maxDepth - 1 >= 0 && maxDepth - 1 < idx) {
        return acc;
      }
      const parent = acc[acc.length - 1];
      acc.push(parent ? `${parent}.${current}` : current);
      return acc;
    }, []);
    // subpackages.push("/" + parts.join("."));
    return subpackages;
  }
}

class Event {
  constructor({ type, attributes }) {
    this.type = type;
    if (!attributes) {
      return;
    }
    // TODO: support more time attributes
    this.time = attributes["(endTime)"];
    this.eventThread = attributes.eventThread;
    this.stackTrace = new StackTrace(attributes.stackTrace);
    this.state = attributes.state;
  }
}

function debug(msg) {
  const displayEl = document.getElementById("debug");
  displayEl.innerHTML += "<br />" + msg;
}

function clear() {
  document.getElementById("debug").innerHTML = "";
}
