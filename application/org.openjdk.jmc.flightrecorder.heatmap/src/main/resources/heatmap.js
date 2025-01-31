/*
 Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 Copyright (c) 2021, Datadog, Inc. All rights reserved.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 The contents of this file are subject to the terms of either the Universal Permissive License
 v 1.0 as shown at https://oss.oracle.com/licenses/upl

 or the following license:

 Redistribution and use in source and binary forms, with or without modification, are permitted
 provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 conditions and the following disclaimer in the documentation and/or other materials provided with
 the distribution.

 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 endorse or promote products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
class Logger {
	constructor() {
		this.domElement = document.createElement("pre");
		window.document.body.appendChild(this.domElement);
	}

	log(msg) {
		this.domElement.innerHTML += msg + "\n";
	}

	clear() {
		this.domElement.innerHTML = "";
	}
}
const chart = {
	data: [],
};

window.logger = new Logger();

try {
	function updateHeatmap(jsonStr) {
		const rawData = JSON.parse(jsonStr);
		const eventTimes = getEventTimes(rawData);
		const binnedData = getBinnedData(eventTimes);
		chart.data = binnedData;
		renderHeatmap(chart.data);
	}

	function resizeSVG() {
		renderHeatmap(chart.data);
	}
	d3.select(window).on("resize", resizeSVG);

	function range(from, to) {
		const result = [];
		for (let i = from; i < to; i++) {
			result.push(i);
		}
		return result;
	}

	function getEventTimes(rawData) {
		return rawData.events
			.filter((event) => event.attributes)
			.map(
				(event) =>
					event.attributes.startTime ||
					event.attributes.endTime ||
					event.attributes["(endTime)"]
			)
			.map((time) => new Date(time / 10e5));
	}

	function getBinnedData(eventTimes) {
		const scale = d3.scaleTime().domain(d3.extent(eventTimes)).nice(); // ♥️♥️♥️
		const timeRangeMs = scale.domain()[1] - scale.domain()[0];
		const binFn = d3
			.bin()
			.domain(scale.domain())
			.thresholds(timeRangeMs / 100);
		return binFn(eventTimes);
	}

	function getChartConfig(binnedData) {
		// numCells and maxEvents depend on the data
		const numCells = binnedData.length;

		// margins are fixed
		const marginLeft = 50;
		const marginRight = 20;
		const marginTop = 20;
		const marginBottom = 100;

		// width and height are fixed from the window
		const width = window.innerWidth - (marginLeft + marginRight);
		const height = window.innerHeight - (marginTop + marginBottom);

		// cellSize is derived from width, height and numCells
		// anything below 4px is invisible
		const cellSize = Math.max(
			4,
			Math.floor(Math.sqrt((width * height) / numCells))
		);
		const numCols = Math.floor(width / cellSize);
		const numRows = Math.ceil(numCells / numCols);

		return {
			numCols,
			numRows,
			cellSize,
			marginLeft,
			marginTop,
			width,
			height,
		};
	}

	function getColorScale(binnedData) {
		const maxEvents = d3.max(binnedData, (d) => d.length);
		return d3.scaleSequentialSqrt(d3.interpolateOrRd).domain([0, maxEvents]);
	}

	function renderHeatmap(binnedData) {
		if (!binnedData || binnedData.length < 2) {
			d3.select("#heatmap").append("p").text("No data in current selection.");
			return;
		}
		const chartConfig = getChartConfig(binnedData);
		const {
			numCols,
			numRows,
			cellSize,
			marginLeft,
			marginTop,
			width,
			height,
		} = chartConfig;
		const colorScale = getColorScale(binnedData);
		const xDomain = range(0, numCols).map((val) => (val * 100) / 1000);
		const yDomain = [
			d3.min(binnedData, (d) => d.x0),
			d3.max(binnedData, (d) => d.x1),
		].map((val) => new Date(val));

		const xScale = d3
			.scaleBand()
			.domain(xDomain)
			.range([marginLeft, numCols * cellSize + marginLeft]);
		const yScale = d3
			.scaleLinear()
			.domain(yDomain)
			.range([marginTop, marginTop + numRows * cellSize]);

		const xTickFilter = (x, idx) => {
			if (cellSize > 15) {
				return true;
			} else if (cellSize > 5) {
				return idx % 5 === 0;
			} else {
				return idx % 10 === 0;
			}
		};
		const xAxis = (g) =>
			g
				.call(
					d3
						.axisBottom(xScale)
						.tickValues(xDomain.filter(xTickFilter))
						.tickFormat((d) => `${d3.format(".1f")(d)} s`)
				)
				.selectAll("text")
				.attr("y", 0)
				.attr("x", -9)
				.attr("dy", ".35em")
				.attr("transform", "rotate(270)")
				.style("text-anchor", "end")
				.style("fill", "#777");

		const yAxis = (g) =>
			g
				.call(
					d3
						.axisLeft(yScale)
						.tickSize(3)
						.tickPadding(4)
						.tickFormat(d3.timeFormat("%H:%M:%S"))
				)
				.selectAll("text")
				.style("fill", "#777");

		const makeCells = (g) =>
			g
				.selectAll("rect")
				.data(binnedData)
				.enter()
				.append("rect")
				// we could potentially emphasize 0 by setting the cell color to white:
				// .attr("fill", (d) => d.length ? colorScale(d.length) : "white")
				.attr("fill", (d) => colorScale(d.length))
				.attr("stroke", "white")
				.attr("x", (d, i) => {
					return (i % numCols) * cellSize;
				})
				.attr("y", (d, i) => {
					return Math.floor(i / numCols) * cellSize;
				})
				.attr("height", cellSize)
				.attr("width", cellSize)
				.style("cursor", "pointer")
				.on("mouseover", function () {
					const target = d3.select(this);
					target.attr("fill", "gold");
				})
				.on("mouseout", function () {
					const target = d3.select(this);
					target.attr("fill", (d) => colorScale(d.length));
				})
				.append("title")
				.text(
					(d) => `${d3.timeFormat("%H:%M:%S.%L")(d.x0)} ${d.length} events`
				);

		d3.select("#heatmap").selectAll("*").remove();

		const svg = d3
			.select("#heatmap")
			.append("svg")
			.style("width", window.innerWidth + "px")
			.style("height", window.innerHeight + "px");
		svg
			.append("g")
			.call(makeCells)
			.attr("transform", (d) => `translate(${marginLeft},${marginTop})`);

		svg
			.append("g")
			.call(xAxis)
			.attr("transform", `translate(0, ${numRows * cellSize + marginTop})`);

		const eventCount = d3.sum(binnedData, (d) => d.length);
		const formatDate = (d) => d.toLocaleString("en-GB", { timeZone: "UTC" });
		svg
			.append("g")
			.append("text")
			.text(
				`This is a heatmap of ${eventCount} events recorded
				between ${formatDate(yDomain[0])} and ${formatDate(yDomain[1])}.`
			)
			.attr("transform", `translate(${width / 2},${height + 75})`)
			.attr("fill", "#777")
			.attr("text-anchor", "middle");

		svg.append("g").call(yAxis).attr("transform", `translate(${marginLeft},0)`);
	}
} catch (e) {
	logger.log(e.name + ":" + e.message);
	logger.log(e.stack);
}
