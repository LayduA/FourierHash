/**
* Standard square-and-multiply algorithm to perform modular exponentiation
*/

function powerMod(base, exponent, modulus) {
	if (modulus === 1) return 0;
	var result = 1;
	base = base % modulus;
	while (exponent > 0) {
		if (exponent % 2 === 1)  //odd number
			result = (result * base) % modulus;
		exponent = exponent >> 1; //divide by 2
		base = (base * base) % modulus;
	}
	return result;
}

/**
* Computes a modular reduction on an array containing parts of a big number.
* hashParts must be an array of numbers such that the most signivicant value is at index 0 (big-endian)
*/
function modHash(hash, modulus) {
	let hashParts = hash.match(/.{1,8}/g)
	
	modAcc = 0
	var intValue = 0;
	var modPart = 0;
	for (let i = 0; i < hashParts.length; i++){
		intValue = parseInt(hashParts[i], 16);
		modPart =  (intValue * powerMod(2, 32 * (hashParts.length - 1 - i), modulus))
		modAcc = (modAcc + modPart) % modulus
	}
	return modAcc;
}


function symmetrify(pixels, hash){
	const symMode = modHash(hash, 23)
	const width = pixels.length
	const height = pixels[0].length
	for (let x = 0; x < width; x++){
		for (let y = 0; y < height; y++){
			if(isInReflection(x, y, width, height, symMode)){
				pixels[x][y] = getReflection(pixels, x, y, width, height, symMode)
			}
		}
	}
	function isInReflection(x, y, width, height, symMode){
	switch (symMode) {
		case 0://HOR_LEFT
			return x > width/2
		case 1://HOR_RIGHT
			return x < width/2
		case 2://VER_UP
			return y > height/2
		case 3://VER_DOWN
			return y < height/2
		case 4://DIAG_LEFT
			return x + y > width
		case 5://DIAG_RIGHT
			return x + y < width
		case 6://ANTIDIAG_LEFT
			return x > y
		case 7://ANTIDIAG_RIGHT
			return x < y
		case 8://+CROSS_TOPLEFT
			return x > width / 2 || y > height / 2
		case 9://+CROSS_TOPRIGHT 
			return x < width / 2 || y > height / 2
		case 10://+CROSS_BOTLEFT 
			return x > width / 2 || y < height / 2
		case 11://CROSS_BOTRIGHT
			return x < width / 2 || y < height / 2
		case 12://XCROSS_LEFT 
			return x + y > width || x > y
		case 13://XCROSS_TOP
			return x + y > width || x < y
		case 14://XCROSS_RIGHT 
			return x + y < width || x < y
		case 15://XCROSS_BOT
			return x + y < width || x > y
		case 16://COL_LEFTEST
			return x > width / 4
		case 17://COL_LEFT
			return x < width / 4 || x > width / 2
		case 18://COL_RIGHT
			return x < width / 2 || x > 3 * width / 4
		case 19://COL_RIGHTEST
			return x < 3 * width / 4
		case 20://ROWS_TOPPEST
			return y > height / 4
		case 21://ROW_TOP
			return y < height / 4 || y > height / 2
		case 22://ROW_BOTTOMEST
			return y < height / 2 || y > 3 * height / 4
		default:
			return false
		}
	}
	function getReflection(pixels, x, y, width, height, symMode){
		var coords
		switch (symMode) {
			case 0:
			case 1:
				return pixels[width - 1 - x][y]
			case 2:
			case 3:
				return pixels[x][height - 1 - y]
			case 4:
			case 5:
				return pixels[width - 1 - y][height - 1 - x]
			case 6:
			case 7:
				return pixels[y][x]
			case 8:
				return pixels[(x > width/2 ? width - 1 - x : x)][(y > height/2 ? height - 1 - y : y)]
			case 9:
				return pixels[(x < width/2 ? width - 1 - x : x)][(y > height/2 ? height - 1 - y : y)]
			case 10:
				return pixels[(x > width/2 ? width - 1 - x : x)][(y < height/2 ? height - 1 - y : y)]
			case 11:
				return pixels[(x < width/2 ? width - 1 - x : x)][(y < height/2 ? height - 1 - y : y)]
			case 12:
				return pixels[(x + y > width ? width - 1 - y : y)][(x + y > width ? width - 1 - x : x)]
			case 13:
				if(x < y && x + y <= width) coords = [y, x]
				else coords = [width - 1 - (x < y ? x : y), height - 1 - (x < y ? y : x)]
				return pixels[coords[0]][coords[1]]
			case 14:
				if(x < y && x + y > width) coords = [y, x]
				else coords = [width - 1 - (x < y ? x : y), height - 1 - (x < y ? y : x)]
				return pixels[coords[0]][coords[1]]
			case 15:
				coords = x + y < width ? [width - 1 - Math.max(x,y), height - 1 - Math.min(x,y)] : [y,x]
				return pixels[coords[0]][coords[1]]
			case 16:
			case 17:
				return pixels[x < width / 2 ? width / 2 - x : x - width/2][y]
			case 18:
				var xCoord;
				if(x <= width / 4) xCoord = x + width/2
				else if (x < width/2) xCoord = width/2 - x
				else xCoord = 3 * width /2 - x
				return pixels[xCoord][y]
			case 19:
				var xCoord
				if(x <= width / 4) xCoord = width - 1 - x
				else if (x < width / 2) xCoord = width / 2 - x
				else xCoord = x - width/2
				return pixels[xCoord][y]
			case 20:
			case 21:
				return pixels[x][y < height / 2 ? height/2 - y : y - height/2]
			case 22:
				var yCoord;
				if(y <= height / 4) yCoord = y + width/2
				else if (y < height / 2) yCoord = width/2 - y
				else yCoord = 3 * height /2 - y
				return pixels[x][yCoord]

			default:
				return [0,0,0]
		}
	}
}

function getPalette(hash){
	
	const shift = modHash(hash, 29)
	const sourcePalette32 = [
		"#000000ff",
		"#424242ff",
		"#7f7e7fff",
		"#bebebeff",
		"#ffffffff",
		"#ff9c7cff",
		"#ff0000ff",
		"#9d1636ff",
		"#43142bff",
		"#8c3a21ff",
		"#c96e19ff",
		"#efb300ff",
		"#ffff00ff",
		"#04be00ff",
		"#0c7a42ff",
		"#113939ff",
		"#0000ffff",
		"#3776ffff",
		"#37bbffff",
		"#04ffffff",
		"#ffb5ecff",
		"#ff00ffff",
		"#a018cfff",
		"#4e1f7fff",
		"#131225ff",
		"#38466cff",
		"#32421bff",
		"#5e5e39ff",
		"#0a9e0aff",
		"#f4d1c3ff",
		"#4f3835ff",
		"#362D2Bff"
	]
	
	//rotate palette
	const sourcePaletteShifted = new Array(sourcePalette32.length)
	for (let i = 0; i < sourcePalette32.length; i++){
		sourcePaletteShifted[i] = sourcePalette32[(i + shift) % sourcePalette32.length]
	}
			
	//Begin extraction of parity bits
	
	
	var parityBits = new Array(16)
	const numberFunctions = 3
	const numberBitsPerGroup = 4
	const groupSize = numberFunctions * numberBitsPerGroup
	const binaryHashLength = hash.length * 4
	const lastBitInGroups = binaryHashLength - (binaryHashLength % groupSize)
	
	for(let i = 0; i < groupSize; i++){
		parityBits[i] = 0
		for(let g = i; g < lastBitInGroups; g += groupSize){
			parityBits[i] ^= extractBit(hash, g)
		}
	}

	for(let r = 0; r < (binaryHashLength % groupSize) / 2; r++){
		//Use the bits that are remaining (in case the length is not a multiple of the group size)
		parityBits[groupSize + r] = extractBit(hash, lastBitInGroups + r) ^ extractBit(hash, lastBitInGroups + r + (binaryHashLength % groupSize) / 2)
	}
	
	//End of extraction of parity bits
	androPalette = [-13483493, -10592711, -16081398, -732733, -11585483, -13226709, -16777216, -12434878, -8421761, -4276546, -1, -25476, -65536, -6482378, -12381141, -7587295, -3576295, -1068288, -256, -16466432, -15959486, -15648455, -16776961, -13142273, -13124609, -16449537, -18964, -65281, -6285105, -11657345, -15527387, -13089172]
	
	
	outPalette = new Array(1 << numberFunctions)
	for(let col = 0; col < outPalette.length; col ++){
		outPalette[col] = sourcePaletteShifted[4 * col + (parityBits[2 * col] ? 2 : 0) + (parityBits[2 * col + 1] ? 1 : 0)]
	}
	
	const cols = [-16081398, -11657345, -13142273, -12381141, -16466432, -12434878, -25476, -16449537]
	return permute(outPalette, modHash(hash, 613))
	
}

function extractBit(hash, index){
		const hexChar = hash.substr(Math.floor(index / 4), 1)
		const bitIndexInChar = (3 - index % 4)
		return (parseInt(hexChar, 16) & (1 << bitIndexInChar)) >> bitIndexInChar 
}

/**
* Draws the visual representation of the transaction hash/identifier in the form of a "QR code".
* Each square of the "QR code" represents a bit of the hash
*  * @param {String} hash String representation of a SHA-256 hash"
*/
function getVisualHashPixels(hash)   {

	const start = Date.now()
	let intermediateTime = start    
	pixels = generateSpectrums(hash, 3)
	console.log("Time elapsed for spectrum gen : " + (Date.now() - intermediateTime)/1000)
	intermediateTime = Date.now()
	
	pixels = pixels.map(inverseFFT2D)
	console.log("Time elapsed for inverse FFT: " + (Date.now() - intermediateTime)/1000)
	intermediateTime = Date.now()
	
	const palette = getPalette(hash)
	console.log("Time elapsed for palette sampling: " + (Date.now() - intermediateTime)/1000)
	intermediateTime = Date.now()

	//Draw the result on the canvas
	let colorIndex, mean
	
	const means = pixels.map(getMean)
	
	console.log("Time elapsed for means: " + (Date.now() - intermediateTime)/1000)
	intermediateTime = Date.now()
	let result = new Array(256)
	for(let i = 0; i < 256; i++){
		result[i] = new Array(256)
	}
	
	for (let i = 0; i < pixels[0].length; i++)    {
		for (let j = 0; j < pixels[0][0].length; j++){
			colorIndex = 0
			for (let f = 0; f < pixels.length; f++){
				colorIndex += (0.4 * pixels[f][i][j].norm() / means[f]) > 0.5 ? (1 << (2 - f)): 0
			}
			
			result[i][j] = palette[colorIndex]
		}
	}
	
	console.log("Time elapsed for filtering: " + (Date.now() - intermediateTime)/1000)
	intermediateTime = Date.now()
	
	symmetrify(result, hash)
	console.log("Time elapsed for symmetry: " + (Date.now() - intermediateTime)/1000)
	intermediateTime = Date.now()
	
	return result
}

function getMean(spectrum){
	let acc = 0
	for(let i = 0; i < spectrum.length; i++){
		for(let j = 0; j < spectrum[0].length; j++){
			acc += spectrum[i][j].norm()
		}
	}
	const mean = acc / (spectrum.length * spectrum[0].length)
	return mean
}
function generateSpectrums(hash, nFunctions){
	
	spectrums = new Array(nFunctions)
	for(let i = 0; i < nFunctions; i++){
		spectrums[i] = generateSpectrum(hash, nFunctions, i)
	}
	return spectrums
}
function generateSpectrum(hash, nFunctions, funcIndex){
	const horribleMagicIndices = [
		[0, 1], [1, 0], [0, 2], [1, 1], [2, 0], [256 - 1, 1], [256 - 1, 2], [256 - 2, 1],
		[3, 0], [2, 1], [256 - 1, 3], [256 - 2, 2], [256 - 3, 1], [4, 0],
		[3, 1], [2, 2], [1, 3], [0, 4], [0, 3], [1, 2], [0, 0]]
	
	const nBitsPerElement = 4
	const groupSize = nBitsPerElement * nFunctions;
	const nGroups = Math.floor(128 / groupSize)
	const maxIndex = nGroups * groupSize;
	const remainder = 128 - maxIndex;
	
	//Initializing 256 * 256 empty pixel array
	let pixels = new Array(256)
	for (let i = 0; i < 256; i++){
		pixels[i] = new Array(256)
		for(let j = 0; j < 256; j++){
			pixels[i][j]= new ComplexNumber(0,0)
		}
	}
			
	let coords, curr4Bits, complex

	for(let currentIndex = 0; currentIndex <= maxIndex; currentIndex += groupSize){
		
		if(currentIndex == maxIndex && funcIndex == 2) continue
		
		curr4Bits = 0
		for(let i = 0; i < 4; i++){
			curr4Bits += extractBit(hash, currentIndex + 4 * funcIndex + i) << (3 - i)
		}
		complex = mapToComplex(curr4Bits)
		
		if(currentIndex < maxIndex){
			coords = horribleMagicIndices[Math.round(currentIndex / groupSize)]
			setValuesInSpectrum(pixels, coords[0], coords[1], complex.mult(dist(coords[0],coords[1])))
		}
		coords = horribleMagicIndices[Math.round((currentIndex + maxIndex) / groupSize)]
		setValuesInSpectrum(pixels, coords[0], coords[1], complex.mult(dist(coords[0],coords[1])))
		
	}
	
	return pixels
}

function dist(x,y){
	if(x > 128) return dist(256 - x, y)
	//params.cut()
	sums = Math.abs(x) + Math.abs(y)
	if(sums > 4) return 0
	return x == 0 && y == 0 ? 2 : 2 / sums
}


function setValuesInSpectrum(spectrum, i, j, value){

	spectrum[i][j] = new ComplexNumber(value.re, value.im)
	
	if (j == 0 && i != 0) {
		spectrum[256 - i][j] = new ComplexNumber(value.re, -value.im)
	} else if (i == 0 && j != 0) {
		spectrum[i][256 - j] = new ComplexNumber(value.re, -value.im)
	} else if (i != 0) {
		spectrum[256 - i][256 - j] = new ComplexNumber(value.re, -value.im);
	}
}
