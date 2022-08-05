const marginLeft = 0;
const marginRight = 0;
const marginTop = 0;
const marginBottom = 0;

const width = window.innerWidth - (marginLeft + marginRight);
const height = window.innerHeight - (marginTop + marginBottom);

let levels = 2;
let chartType = "EDGE_BUNDLING";

function setChartType(value) {
  chartType = value;
}

function updateGraph(eventsJson, packageLevels) {
  const data = JSON.parse(eventsJson);
  debug(`Loaded ${data.events.length} events`);
  try {
    levels = packageLevels;
    if (chartType === "EDGE_BUNDLING") {
      debug(`Rendering edge bundling diagram: ${chartType}`);
      const edgeBundlingData = transformEdgeBundlingData(data);
      renderHierarchicalEdgeBundling(edgeBundlingData);
    } else if (chartType === "CHORD") {
      debug(`Rendering chord diagram: ${chartType}`);
      const chordData = transformChordData(data);
      renderChordDiagram(chordData);
    }
  } catch (e) {
    debug(e.message);
    debug(`<pre>${e.stack}</pre>`);
  }
}
