function updateGraph(eventsJson) {
	const data = JSON.parse(eventsJson);
	clear();
    debug(`Loaded ${data.events.length} events`);
    try {
		debug(JSON.stringify(data.events[0]));
		render(data);
	} catch (e) {
		debug(e.message);
		debug(`<pre>${e.stack}</pre>`);
	}
}

const marginLeft = 0;
const marginRight = 0;
const marginTop = 0;
const marginBottom = 0;

const width = window.innerWidth - (marginLeft + marginRight);
const height = window.innerHeight - (marginTop + marginBottom);

var levels = 2;

function updateGraph(eventsJson, packageLevels) {
  const data = JSON.parse(eventsJson);
  clear();
  debug(`Loaded ${data.events.length} events`);
  try {
    levels = packageLevels
    render(data);
  } catch (e) {
    debug(e.message);
    debug(`<pre>${e.stack}</pre>`);
  }
}


function debug(msg) {
  const displayEl = document.getElementById("debug");
  displayEl.innerHTML += "<br />" + msg;
}

function clear() {
  document.getElementById("debug").innerHTML = "";
}

function render(data) {
  // compute package hierarchy
  const svg = d3
    .select("#hierarchical")
    .append("svg")
    .attr("viewBox", [-width / 2, -width / 2, width, width]);
  const graph = buildGraph(data);
  const d3Hierarchy = d3
    .hierarchy(graph.root)
    .sort(
      (a, b) =>
        d3.ascending(a.height, b.height) ||
        d3.ascending(a.data.name, b.data.name)
    );
  const treeClustering = d3.cluster().size([2 * Math.PI, width / 2 - 100])
  const root = treeClustering(bilink(d3Hierarchy));
  const colors = getColors();
  const node = svg
    .append("g")
    .attr("font-family", "sans-serif")
    .attr("font-size", 10)
    .selectAll("g")
    // .data(root.descendants().filter(d => d.data.leaf))
    .data(root.leaves())
    .join("g")
    .attr(
      "transform",
      (d) => `rotate(${(d.x * 180) / Math.PI - 90}) translate(${d.y},0)`
    )
    .append("text")
    .attr("dy", "0.31em")
    .attr("x", (d) => (d.x < Math.PI ? 6 : -6))
    .attr("text-anchor", (d) => (d.x < Math.PI ? "start" : "end"))
    .attr("transform", (d) => (d.x >= Math.PI ? "rotate(180)" : null))
    .text((d) => d.data.name)
    .each(function (d) {
      d.text = this;
    })
    .on("mouseover", overed)
    .on("mouseout", outed)
    .call((text) =>
      text.attr("fill", colors.default).append("title").text(formatTooltip)
    );
  const line = d3
    .lineRadial()
    .curve(d3.curveBundle.beta(0.85))
    .radius((d) => d.y)
    .angle((d) => d.x);
  const link = svg
    .append("g")
    .attr("stroke", colors.link)
    .attr("fill", "none")
    .selectAll("path")
    .data(root.leaves().flatMap((leaf) => leaf.outgoing))
    .join("path")
    .style("mix-blend-mode", "multiply")
    .attr("d", ([i, o]) => line(i.path(o)))
    .each(function (d) {
      d.path = this;
    });

  function overed(event, d) {
    link.style("mix-blend-mode", null);
    d3.select(this)
      .attr("font-weight", "bold")
      .attr("fill", colors.hover)
      .style("cursor", "default");
    d3.selectAll(d.incoming.map((d) => d.path))
      .attr("stroke", colors.incoming)
      .raise();
    d3.selectAll(d.incoming.map(([d]) => d.text))
      .attr("fill", colors.incoming)
      .attr("font-weight", "bold");
    d3.selectAll(d.outgoing.map((d) => d.path))
      .attr("stroke", colors.outgoing)
      .raise();
    d3.selectAll(d.outgoing.map(([, d]) => d.text))
      .attr("fill", colors.outgoing)
      .attr("font-weight", "bold");

    const both = intersection(d.incoming, d.outgoing);
    d3.selectAll(both.map((d) => d.path))
      .attr("stroke", colors.both)
      .raise();
    d3.selectAll(both.map(([d]) => d.text))
      .attr("fill", colors.both)
      .attr("font-weight", "bold");
  }

  function outed(event, d) {
    link.style("mix-blend-mode", "multiply");
    d3.select(this).attr("font-weight", null).attr("fill", colors.default);
    d3.selectAll(d.incoming.map((d) => d.path)).attr("stroke", null);
    d3.selectAll(d.incoming.map(([d]) => d.text))
      .attr("fill", colors.default)
      .attr("font-weight", null);
    d3.selectAll(d.outgoing.map((d) => d.path)).attr("stroke", null);
    d3.selectAll(d.outgoing.map(([, d]) => d.text))
      .attr("fill", colors.default)
      .attr("font-weight", null);
  }

  return svg.node();
}


class Graph {
  constructor() {
    this.root = new Node("root");
    this.nodesByName = { root: this.root };
  }

  createAllNodes(frame) {
    const nodeNames = frame.package.subpackages(levels);
    // .concat([
    //   frame.package.name + "." + frame.clazz
    // ]);
    let node = this.root;
    for (let nodeName of nodeNames) {
      const child = this.ensureNode(nodeName);
      const exists = node.children.find((c) => c.name === child.name);
      if (!exists) {
        node.children.push(child);
      }
      node = child;
    }
    return node;
  }

  ensureNode(name) {
    let node = this.nodesByName[name];
    if (!node) {
      node = new Node(name);
      this.nodesByName[name] = node;
    }
    return node;
  }
}

class Node {
  constructor(name, fullName) {
    this.name = name;
    this.fullName = fullName;
    this.children = [];
    this.outgoing = new Set();
    this.incoming = new Set();
  }

  addChild(node) {
    this.children.push(node);
    return this;
  }

  addOutgoing(node) {
    this.outgoing.add(node);
  }

  addIncoming(node) {
    this.incoming.add(node);
  }
}

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

function intersection(incoming, outgoing) {
  return incoming.filter((i) => outgoing.find((o) => o[1] == i[0]));
}

function subtract(first, second, itemFn) {
  return first.filter((pair) => !second.find((i) => i[0] === itemFn(pair)));
}

function formatTooltip(d) {
  let tooltip = d.data.name;
  const both = intersection(d.incoming, d.outgoing);
  const incoming = subtract(d.incoming, both, (p) => p[0]);
  const outgoing = subtract(d.outgoing, both, (p) => p[1]);
  const getName = (d) => d.data.name;
  const displayLink = (prefix, d) => `${prefix} ${getName(d)}`;
  const displayLinks = (prefix, ds, getItem) =>
    ds.map((d) => displayLink(prefix, getItem(d))).join("\n");
  tooltip += "\n" + displayLinks("←", incoming, (p) => p[0]);
  tooltip += "\n" + displayLinks("↔", both, (p) => p[0]);
  tooltip += "\n" + displayLinks("→", outgoing, (p) => p[1]);
  return tooltip;
}


function bilink(root) {
  // .leaves() returns only top level nodes, we need intermediaries too
  // const leaves = root.descendants().filter(node => node.data.leaf);
  const leaves = root.leaves();
  const map = new Map(leaves.map(d => [d.data.name, d]));
  for (const d of leaves) {
    d.incoming = [];
    d.outgoing = [...d.data.outgoing]
      .map(i => {
        return [d, map.get(i.name)];
      })
      .filter(o => o[1]);
  }

  for (const d of leaves) {
    for (const o of d.outgoing) {
      o[1].incoming.push(o);
    }
  }

  return root;
}

function buildGraph(data) {
  const graph = new Graph();
  data.events
    .map((d) => new Event(d))
    .forEach((event) => {
      const stackTrace = event.stackTrace;
      if (stackTrace) {
        const frames = stackTrace.frames.reverse();
        for (let idx = 0; idx < frames.length; idx++) {
          const currentFrame = frames[idx];
          const parentFrame = frames[idx - 1];
          const current = graph.createAllNodes(currentFrame);
          if (parentFrame) {
            const parent = graph.createAllNodes(parentFrame);
            if (parent != current) {
              parent.addIncoming(current);
              current.addOutgoing(parent);
            }
          }
          if (idx === frames.length - 1) {
            current.leaf = true;
          }
        }
      }
    });
  // _.forOwn(graph.nodesByName, (node, name) => {
  //   if (node.leaf && node.children.length) {
  //     console.log(node.name);
  //   }
  // });
  return graph;
}

function getColors() {
  const scheme = d3.schemeCategory10;
  return {
    outgoing: scheme[2],
    incoming: scheme[1],
    hover: scheme[0],
    both: scheme[3],
    default: "#eee",
    link: "#ccc"
  };
}