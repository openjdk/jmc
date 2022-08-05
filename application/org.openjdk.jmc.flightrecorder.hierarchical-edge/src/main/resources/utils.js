class StackTrace {
	constructor(stackTraceData) {
		const frames = (stackTraceData && stackTraceData.frames) || [];
		this.frames = frames.map((frame) => new StackFrame(frame));
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
		this.time = attributes.startTime ||
					attributes.endTime ||
					attributes["(endTime)"]
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

// data manipulation for chord diagram
const maxPackages = 500;

function truncatePackage(name, level) {
	return name.split(".").slice(0, level).join(".");
}

function transformChordData(json) {
	const events = json.events.map((e) => new Event(e));
	let calls = {};
	events.forEach((e) => {
		const frames = e.stackTrace.frames;
		const len = frames.length;
		for (let i = len - 1; i > 0; i--) {
			const source = truncatePackage(frames[i].package.name, levels);
			const target = truncatePackage(frames[i - 1].package.name, levels);
			const id = `${source} ${target}`;
			calls[id] = (calls[id] || 0) + 1;
		}
	});
	calls = Object.entries(calls)
		.sort(([, a], [, b]) => b - a)
		.slice(0, maxPackages)
		.reduce((r, [k, v]) => ({ ...r, [k]: v }), {});
	return Object.keys(calls).map((id) => {
		const [source, target] = id.split(" ");
		return { source, target, value: calls[id] };
	});
}

// data manipulation for hierarchical edge bundling diagram
function transformEdgeBundlingData(data) {
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
		link: "#ccc",
	};
}

class Graph {
	constructor() {
		this.root = new Node("root");
		this.nodesByName = { root: this.root };
	}

	createAllNodes(frame) {
		const nodeNames = frame.package.subpackages(levels);
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
