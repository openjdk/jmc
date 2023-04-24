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

// Modified version of Mike Bostock's 'Chord Dependency Diagram' observable notebook:
// https://observablehq.com/@d3/chord-dependency-diagram 
function renderChordDiagram(data) {
	const svg = d3
		.select("#diagram")
		.append("svg")
		.attr("viewBox", [-width / 2, -height / 2, width, height]);

	const innerRadius = Math.max(Math.min(width, height) * 0.5 - 90, 100);
	const outerRadius = innerRadius + 10;

	const names = Array.from(
		new Set(data.flatMap((d) => [d.source, d.target]))
	).sort(d3.ascending);
	const color = d3.scaleOrdinal(
		names,
		d3.quantize(d3.interpolateRainbow, names.length)
	);

	const chord = d3
		.chordDirected()
		.padAngle(10 / innerRadius)
		.sortSubgroups(d3.descending)
		.sortChords(d3.descending);

	const arc = d3.arc().innerRadius(innerRadius).outerRadius(outerRadius);

	const ribbon = d3
		.ribbonArrow()
		.radius(innerRadius - 1)
		.padAngle(1 / innerRadius);

	const matrix = buildMatrix(data, names);
	const chords = chord(matrix);

	const group = svg
		.append("g")
		.attr("font-size", 10)
		.attr("font-family", "sans-serif")
		.selectAll("g")
		.data(chords.groups)
		.join("g");

	group
		.append("path")
		.attr("fill", (d) => color(names[d.index]))
		.attr("d", arc);

	group
		.append("text")
		.each((d) => (d.angle = (d.startAngle + d.endAngle) / 2))
		.attr("dy", "0.35em")
		.attr(
			"transform",
			(d) => `
				rotate(${(d.angle * 180) / Math.PI - 90})
				translate(${outerRadius + 5})
				${d.angle > Math.PI ? "rotate(180)" : ""}
			`
		)
		.attr("text-anchor", (d) => (d.angle > Math.PI ? "end" : null))
		.text((d) => names[d.index]);

	group.append("title").text(
		(d) => `${names[d.index]}
${d3.sum(
	chords,
	(c) => (c.source.index === d.index) * c.source.value
)} outgoing →
${d3.sum(
	chords,
	(c) => (c.target.index === d.index) * c.source.value
)} incoming ←`
	);

	svg
		.append("g")
		.attr("fill-opacity", 0.75)
		.selectAll("path")
		.data(chords)
		.join("path")
		.style("mix-blend-mode", "multiply")
		.attr("fill", (d) => color(names[d.target.index]))
		.attr("d", ribbon)
		.append("title")
		.text(
			(d) =>
				`${names[d.source.index]} → ${names[d.target.index]} ${d.source.value}`
		);

	return svg.node();
}

function buildMatrix(data, names) {
	const index = new Map(names.map((name, i) => [name, i]));
	const matrix = Array.from(index, () => new Array(names.length).fill(0));
	for (const { source, target, value } of data)
		matrix[index.get(source)][index.get(target)] += value;
	return matrix;
}
