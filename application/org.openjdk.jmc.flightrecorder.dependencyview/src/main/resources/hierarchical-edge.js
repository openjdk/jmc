// Permission to use, copy, modify, and/or distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.

// THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
// WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
// ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
// OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

// Modified version of Mike Bostock's 'Hierarchical Edge Bundling' observable notebook:
// https://observablehq.com/@d3/hierarchical-edge-bundling
function renderHierarchicalEdgeBundling(graph) {
	// compute package hierarchy
	const svg = d3
		.select("#diagram")
		.append("svg")
		.attr("viewBox", [-width / 2, -height / 2, width, height]);
	const d3Hierarchy = d3
		.hierarchy(graph.root)
		.sort(
			(a, b) =>
				d3.ascending(a.height, b.height) ||
				d3.ascending(a.data.name, b.data.name)
		);
	// we use the same formula as for the outerRadius in the chord diagram
	const radius = Math.max(Math.min(width, height) * 0.5 - 90, 100) + 10;
	const treeClustering = d3.cluster().size([2 * Math.PI, radius]);
	const root = treeClustering(bilink(d3Hierarchy));
	const colors = getColors();
	const node = svg
		.append("g")
		.attr("font-family", "sans-serif")
		.attr("font-size", 10)
		.selectAll("g")
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

function bilink(root) {
	const leaves = root.leaves();
	const map = new Map(leaves.map((d) => [d.data.name, d]));
	for (const d of leaves) {
		d.incoming = [];
		d.outgoing = [...d.data.outgoing]
			.map((i) => {
				return [d, map.get(i.name)];
			})
			.filter((o) => o[1]);
	}

	for (const d of leaves) {
		for (const o of d.outgoing) {
			o[1].incoming.push(o);
		}
	}

	return root;
}
