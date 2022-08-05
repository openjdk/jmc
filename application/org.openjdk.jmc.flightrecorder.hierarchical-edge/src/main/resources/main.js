let width = window.innerWidth;
let height = window.innerHeight;

let levels = 2;

function updateGraph(eventsJson, packageLevels, chartType) {
  const data = JSON.parse(eventsJson);
  debug(`${chartType} displaying ${data.events.length} events`);
  try {
    levels = packageLevels;
    width = window.innerWidth;
    height = window.innerHeight;
    d3.select("#hierarchical").selectAll("*").remove();
    if (chartType === "EDGE_BUNDLING") {
      const edgeBundlingData = transformEdgeBundlingData(data);
      renderHierarchicalEdgeBundling(edgeBundlingData);
    } else if (chartType === "CHORD") {
      const chordData = transformChordData(data);
      renderChordDiagram(chordData);
    }
  } catch (e) {
    debug(e.message);
    debug(`<pre>${e.stack}</pre>`);
  }
}
