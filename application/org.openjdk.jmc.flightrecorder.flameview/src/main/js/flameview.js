
/*
 Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 Copyright (c) 2020, Datadog, Inc. All rights reserved.

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 The contents of this file are subject to the terms of either the Universal Permissive License
 v 1.0 as shown at http://oss.oracle.com/licenses/upl

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

String.prototype.hashCode = function() {
	var hash = 0;
	if (this.length === 0) return hash;
	for (var i = 0; i < this.length; i++) {
		var char = this.charCodeAt(i);
		hash = ((hash << 5) - hash) + char;
		hash = hash & hash;
	}
	return hash;
};

const htmlTagBr = "\u003Cbr\u002F\u003E";
const rootPackageColor = "darkred";
const invalidPackageColor = "snow";
const packageJavaColorLightGray = "lightgray";
const packageComSunAndJdkColorDarkGray = "darkgray";
const packageSunDarkColorGray = "gray";
const packageRestValueHMax = 360;
const packageRestValueH = 0;
const packageRestSLValues = [42, 53];
const packageConsideredDepth = 3;
const packageMarkerJava = "java";
const packageMarkerSun = "sun";
const packageMarkerComSunAndJdk = "comSunAndJdk";
const packageMarkerRest = "rest";
const packagesIdentifierMap = new Map().set("java.", packageMarkerJava).set("sun.", packageMarkerSun).set("com.sun.", 
		packageMarkerComSunAndJdk).set("jdk.", packageMarkerComSunAndJdk);
const packageColorMap = new Map().set("", rootPackageColor);
const specialCharactersMap = new Map().set('#', '\x23').set('$', '\x24').set('(', '\x28').set(')', '\x29')
		.set(',', '\x2c').set('-', '\x2d').set('.', '\x2e').set('<', '\x3c').set('>', '\x3e').set('[', '\x5b')
		.set(']', '\x5d').set('_', '\x5f').set('{', '\x7b').set('|', '\x7c').set('}', '\x7d').set('~', '\x7e');

const colorByPackage = function(p) {
	if (p === undefined) {
		return invalidPackageColor;
	} else {
		const packageNameStrip = stripPackageName(p);
		const packageSelectedColor = packageColorMap.get(packageNameStrip);
		if (packageSelectedColor === undefined) {
			const packageMarkerSelected = getPackageMarker(packageNameStrip);
			const packageNameStripHash = packageNameStrip.hashCode();
			switch (packageMarkerSelected) {
			case packageMarkerJava:
				packageColorMap.set(packageNameStrip, packageJavaColorLightGray);
				break;
			case packageMarkerComSunAndJdk:
				packageColorMap.set(packageNameStrip, packageComSunAndJdkColorDarkGray);
				break;
			case packageMarkerSun:
				packageColorMap.set(packageNameStrip, packageSunDarkColorGray);
				break;
			case packageMarkerRest:
				const packageRestSelectedColor = createHslColorString(adjustHslPropertyByHash(packageNameStripHash, packageRestValueH, packageRestValueHMax), packageRestSLValues[0], packageRestSLValues[1]);
				packageColorMap.set(packageNameStrip, packageRestSelectedColor);
				break;
			}
			return packageColorMap.get(packageNameStrip);
		} else {
			return packageSelectedColor;
		}
	}
};

const getPackageMarker = function(p) {
	for(let k of packagesIdentifierMap.keys()){
		if(p.startsWith(k)){
			return packagesIdentifierMap.get(k);
		}
	}
	return packageMarkerRest;
};

const stripPackageName = function(p) {
	const splitString = p.split("\u002E");
	const number = Math.min(splitString.length, packageConsideredDepth);
	return splitString.slice(0, number).join("\u002E");
};

const adjustHslPropertyByHash = function (hash, min, max) {
	const proposedValue = hash % (max - min) + min;
	return Math.min(proposedValue, max);
};

const createHslColorString = function(h,s,l) {
	return "hsl\u0028" + h + "\u002c " + s + "\u0025\u002c " + l + "\u0025\u0029";
};

const colorCell = function(d) {
	if (textToSearch !== "" && (evaluateSearchElement(d.data.p) || evaluateSearchElement(d.data.n))) {
		return "magenta";
	} else {
		return colorByPackage(d.data.p);
	}
};

const evaluateSearchElement = function(text) {
	var adjustTextToSearch = removeSpecialCharacters(textToSearch);
	return text !== undefined && removeSpecialCharacters(text).includes(adjustTextToSearch);
};

const removeSpecialCharacters = function(text) {
	return Array.prototype.map.call(text.trim().toLowerCase(), element => {
		if (specialCharactersMap.has(element)) {
			return specialCharactersMap.get(element);
		} else {
			return element;
		}}).join('');
};


const adjustTip = function(d) {
	var tipMessage = "".concat(d.data.n, htmlTagBr);
	
	if (nodeContainsChildren(d.data)) {
		if (d.data.d && d.data.d.includes("|")) {
			tipMessage += createRootTable(d.data.d);
		} else {
			tipMessage += createNodeTipTable(d.data);
		}
	}
	
	return tipMessage;
}

const nodeContainsChildren = function(data) {
	return Array.isArray(data.c) && data.c.length;
}

const createNodeTipTable = function(data) {
	var table = "".concat(tagOpen("table class='d3-flame-graph-tip'"), tagOpen("tbody"))
	if (data.d === undefined) {
		table = table.concat(addTableRow(tootlipPackage, data.p, "tdLabel"), 
				addTableRow(tootlipSamples, data.v, "tdLabel"));
	} else {
		table += addTableRow(tootlipDescription, data.d, "tdCount");
	}
	return table.concat(tagClose("tbody"), tagClose("table"));
}

const createRootTable = function(input) {
	var table = "";
	var tableRows = input.split("|");
	table = table.concat(tagOpen("table class='d3-flame-graph-tip'"), createTableHeader(), tagOpen("tbody"));
	var prevCount = 0;
	for(var i=0; i < tableRows.length - 1; i++) {
		const rowValue = tableRows[i].split(":");
		table += addTableRow(parseInt(rowValue[0]), rowValue[1], "tdCount");
	}
	table = table.concat(tagClose("tbody"), tagClose("table"));
	return table;
}

const tagOpen = function(tag, cssClass) {
	var result = "\u003C" + tag;
	if (cssClass === undefined) {
		result +="\u003E";
	} else {
		var cssExtended = " class='" + cssClass + "' \u003E";
		result += cssExtended;
	}
	return result;
}
const tagClose = function(tag) {
	return "\u003C\u002F "+ tag + "\u003E";
}

const addTableRow = function(eventCount, eventName, cssStartTd) {
	return tableTr(tableTd(eventCount, cssStartTd), tableTd(eventName));
}

const createTableHeader = function() { 
	return tagOpen("thead").concat(tableTr(tableTh(tooltipTableThCount, "tdLabel"), tableTh(tooltipTableThEventType)),tagClose("thead"));
}

const tableTh = function(value, css) {
	return tagOpen("th", css).concat(value, tagClose("th"));
}

const tableTd = function(value, css) {
	return tagOpen("td", css).concat(value, tagClose("td"));
}

const tableTr = function(...elements) {
	return tagOpen("tr").concat(elements.join(""), tagClose("tr"));
}

