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

String.prototype.hashCode = function () {
	var hash = 0;
	if (this.length === 0) return hash;
	for (var i = 0; i < this.length; i++) {
		var char = this.charCodeAt(i);
		hash = ((hash << 5) - hash) + char;
		hash = hash & hash;
	}
	return hash;
};

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

const colorByPackage = function (p) {
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

const getPackageMarker = function(p){
	for(let k of packagesIdentifierMap.keys()){
		if(p.startsWith(k)){
			return packagesIdentifierMap.get(k);
		}
	}
	return packageMarkerRest;
};

const stripPackageName = function (p) {
	const splitString = p.split("\u002E");
	const number = Math.min(splitString.length, packageConsideredDepth);
	return splitString.slice(0, number).join("\u002E");
};

const adjustHslPropertyByHash = function (hash, min, max) {
	const proposedValue = hash % (max - min) + min;
	return Math.min(proposedValue, max);
};

const createHslColorString = function(h,s,l){
	return "hsl\u0028" + h + "\u002c " + s + "\u0025\u002c " + l + "\u0025\u0029";
};

const colorCell = function (d) {
	return colorByPackage(d.data.p);
};

const adjustTip = function (d) {
	return d.data.n + "\u003Cbr\u002F\u003Epackage: " + d.data.p + "\u003Cbr\u002F\u003Esamples: " + d.data.v;
};