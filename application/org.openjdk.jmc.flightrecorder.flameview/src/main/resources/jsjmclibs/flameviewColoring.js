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
const packageJavaValueLMax = 80;
const packageJavaValueL = 65;
const packageJavaHslHSValues = [184, 10];
const packageSunValueLMax = 54;
const packageSunValueL = 31;
const packageSunHSValues = [211, 21];
const packageRestValueHMax = 360;
const packageRestValueH = 0;
const packageRestSLValues = [42, 53];
const packageConsideredDepth = 3;
const packageMarkerJava = "java";
const packageMarkerSun = "sun";
const packageMarkerRest = "rest";
const packagesIdentifierMap = new Map().set("java.", packageMarkerJava).set("sun.", packageMarkerSun)
    .set("com.sun.", packageMarkerSun);
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
                    const packageJavaSelectedColor = createHslColorString(packageJavaHslHSValues[0], packageJavaHslHSValues[1], adjustHslPropertyByHash(packageNameStripHash, packageJavaValueL, packageJavaValueLMax));
                    packageColorMap.set(packageNameStrip, packageJavaSelectedColor);
                    break;
                case packageMarkerSun:
                    const packageSunSelectedColor = createHslColorString(packageSunHSValues[0], packageSunHSValues[1], adjustHslPropertyByHash(packageNameStripHash, packageSunValueL, packageSunValueLMax));
                    packageColorMap.set(packageNameStrip, packageSunSelectedColor);
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