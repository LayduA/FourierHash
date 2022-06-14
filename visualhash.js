//Primes
const SYMMETRY_PRIME = 23;
const PALETTE_SHIFT_PRIME = 29;
const PERMUTATION_PRIME = 613;
//Complex encodings (bits to frequency mapping)
const COMPLEX_ENCODINGS_REAL = [-0.5, -0.5,  1.0,  1.0,  0.5,  0.5, -1.0, -1.0,  1.0,  1.0, -0.5, -0.5, -1.0, -1.0,  0.5,  0.5];
const COMPLEX_ENCODINGS_IMAG = [ 0.5, -1.0, -0.5,  1.0, -1.0,  0.5,  1.0, -0.5,  0.5, -1.0, -0.5,  1.0, -1.0,  0.5,  1.0,  0.5];
//Constant parameters
const CORRECTION_FACTOR = 0.4;
const NUMBER_PARITY_BITS = 16;
const COLORS_32 = ["#000000ff","#424242ff","#7f7e7fff","#bebebeff","#ffffffff","#ff9c7cff","#ff0000ff","#9d1636ff","#43142bff",
	"#8c3a21ff","#c96e19ff","#efb300ff","#ffff00ff","#04be00ff","#0c7a42ff","#113939ff","#0000ffff","#3776ffff","#37bbffff",
	"#04ffffff","#ffb5ecff","#ff00ffff","#a018cfff","#4e1f7fff","#131225ff","#38466cff","#32421bff","#5e5e39ff","#0a9e0aff",
	"#f4d1c3ff","#4f3835ff","#362D2Bff"
]
//Useful constants
const BITS_IN_ONE_HEX_CHAR = 4;

//Main method
function getVisualHashPixels(hash, sideLength, nFunctions)   {

	let spectrums = generateSpectrums(hash, sideLength, nFunctions);
	spectrums = spectrums.map(inverseFFT2D);
	spectrums = spectrums.map(normalizeMeanCorrected)

	const palette = getPalette(hash, nFunctions);

	//Initialize empty fingerprint
	let fingerprint = new Array(sideLength);
	for(let i = 0; i < sideLength; i++){
		fingerprint[i] = new Array(sideLength);
	}

	//Getting the rounded value of every function at every pixel
	let colorIndex
	const symmetryMode = modHash(hash, SYMMETRY_PRIME)
	for (let i = 0; i < spectrums[0].length; i++)    {
		for (let j = 0; j < spectrums[0][0].length; j++){
			const xy = getPreImageCoordinates(i, j, spectrums[0].length, symmetryMode) 
			const x = xy[0]
			const y = xy[1]

			colorIndex = 0;
			for (let f = 0; f < spectrums.length; f++){
				if(spectrums[f][x][y].module() > 0.5){
					colorIndex += 1 << f;
				}
			}
			
			fingerprint[i][j] = palette[colorIndex];
		}
	}

    smoothen(fingerprint);
	return fingerprint;
}

class ComplexNumber {
	constructor(real, imaginary) {
		this.re = real;
		this.im = imaginary;
	}
	mult(c) {
		if (typeof c === 'object') {
			// complex multiplication
			let rePart = this.re * c.re - this.im * c.im;
			let imPart = this.re * c.im + this.im * c.re;
			return new ComplexNumber(rePart, imPart);
		} else {
			// multiplication with real number
			return new ComplexNumber(c * this.re, c * this.im);
		}
	}
	module() {
		return Math.sqrt(this.re * this.re + this.im * this.im);
	}
}

function modHash(hash, modulus) {
	//Computes a modular reduction on a hash (hex string)

	const chunkSize = 8
	//Split string into chunks of 8 hex chars (= 32 bits) each
	let hashChunks = hash.match(new RegExp(".{1,L}".replace("L", chunkSize), "g"))

	let acc = 0;
	var chunkValue = 0;
	var chunkMod = 0;
	//Compute for each chunk, compute its modulo and add it to the accumulator
	for (let i = 0; i < hashChunks.length; i++){
		chunkValue = parseInt(hashChunks[i], 16);
		offset = chunkSize * BITS_IN_ONE_HEX_CHAR * (hashChunks.length - 1 - i);
		chunkMod =  chunkValue * powerMod(2, offset, modulus);
		acc = (acc + chunkMod) % modulus;
	}
	return acc;
}

function powerMod(base, exponent, modulus) {
	//Standard square-and-multiply algorithm
	if (modulus === 1) return 0;
	var result = 1;
	base = base % modulus;
	while (exponent > 0) {
		if (exponent % 2 === 1) {
			result = (result * base) % modulus;
		}
		exponent = exponent >> 1; 
		base = (base * base) % modulus;
	}
	return result;
}

//-------------------------SPECTRUM GENERATION------------------------------//

function generateSpectrums(hash, sideLength, nFunctions){

	spectrums = new Array(nFunctions);
	for(let i = 0; i < nFunctions; i++){
		spectrums[i] = generateSpectrum(hash, sideLength, nFunctions, i);
	}
	return spectrums;
}
function generateSpectrum(hash, sideLength, numberFunctions, functionIndex){
	
	// Those are the locations at which the frequencies are going to be placed, centered around (0,0)
    // The "sideLength - X" values correspond to negative values, i.e "sideLength - 2" corresponds to -2
	const spectrumLocations = [
		[[0,1],[sideLength - 1,3]], [[1, 0], [sideLength - 2,2]], [[0,2], [sideLength - 3,1]],
		 [[1, 1], [4, 0]], [[2,0], [3,1]], [[sideLength - 1,1], [2,2]] , [[sideLength - 1,2],
		 [1,3]], [[sideLength - 2,1], [0,4]], [[3,0], [0,3]], [[2,1], [1,2]], [[0,0]]
	];

	//Initialize all-zero complex spectrum
	const spectrum = new Array(sideLength);
	for (let i = 0; i < sideLength; i++){
		spectrum[i] = new Array(sideLength);
		for(let j = 0; j < sideLength; j++){
			spectrum[i][j]= new ComplexNumber(0,0);
		}
	}
	
	//Read and encode each character into a frequency and place it in the spectrum
	for(let charIndex = 0; charIndex + functionIndex < hash.length; charIndex += numberFunctions){

		const currentHexChar = parseInt(hash.charAt(charIndex + functionIndex), 16);
		const currentFrequency = mapToComplex(currentHexChar);
		const locationsInSpectrum = spectrumLocations[(charIndex / numberFunctions) >> 0]; //"Integer division" in javascript shenanigans

		placeInSpectrum(spectrum, locationsInSpectrum, currentFrequency, sideLength);

	}

	return spectrum;
}

function mapToComplex(fourBits){	
	return new ComplexNumber(COMPLEX_ENCODINGS_REAL[fourBits], COMPLEX_ENCODINGS_IMAG[fourBits]);
}

function getDistanceFactor(x, y, sideLength){
	const absX = (x > sideLength / 2 ? sideLength - x : x);
	const sum = absX + y;
	return sum == 0 ? 1 : 1 / sum;
}

function placeInSpectrum(spectrum, locationsInSpectrum, complex, sideLength){
	locationsInSpectrum.forEach(location => setValueInSpectrum(spectrum, location, complex, sideLength));
}

function setValueInSpectrum(spectrum, locationInSpectrum, complex, sideLength){

	const x = locationInSpectrum[0];
	const y = locationInSpectrum[1];

	const value = complex.mult(getDistanceFactor(x, y, sideLength));

	spectrum[x][y] = new ComplexNumber(value.re, value.im);

	//We ensure the spectrum is (conjugate-) symmetric. Since the symmetry axis is ON the 0 coordinate
	// (rather than between 0 and -1), the symmetric of the 0 coordinate is the 0 coordinate.
	const symmetricX = (x == 0 ? 0 : sideLength - x);
	const symmetricY = (y == 0 ? 0 : sideLength - y);
	spectrum[symmetricX][symmetricY] = new ComplexNumber(value.re, -value.im);
}

function normalizeMeanCorrected(spectrum) {
	//Scales the contents of a 2D complex array such that the output has an average module
	// equal to CORRECTION_FACTOR.

	const averageModule = getMeanModule(spectrum)
	if (averageModule == 0)
		throw new IllegalArgumentException("Cannot normalize an all-zero array");

	const normalized = new Array(spectrum.length)
	for(let i = 0; i < spectrum.length; i++){
		normalized[i] = new Array(spectrum[0].length);
		for (let j = 0; j < spectrum[0].length; j++) {
			normalized[i][j] = spectrum[i][j].mult(CORRECTION_FACTOR / averageModule);
		}
	}

	return normalized;
}

function getMeanModule(spectrum){
	let acc = 0;
	for(let i = 0; i < spectrum.length; i++){
		for(let j = 0; j < spectrum[0].length; j++){
			acc += spectrum[i][j].module();
		}
	}
	const mean = acc / (spectrum.length * spectrum[0].length);
	return mean;
}

//------------------------------PALETTE--------------------------//
function getPalette(hash, numberFunctions){
	//Returns a 2^n color palette, with n the number of functions

	const palette = new Array(1 << numberFunctions)
	const paletteShift = modHash(hash, PALETTE_SHIFT_PRIME)

	var parityBits = getParityBits(hash, numberFunctions)

	for(let col = 0; col < palette.length; col ++){
		//For now, we support nFunctions = 3 or 4
		if(numberFunctions == 3){
			palette[col] = COLORS_32[(4 * col + 2 * parityBits[2 * col] + parityBits[2 * col + 1] + paletteShift) % COLORS_32.length]
		}else if (numberFunctions == 4){
			palette[col] = COLORS_32[(2 * col + parityBits[col] + paletteShift) % COLORS_32.length]
		}
	}

	return permute(palette, modHash(hash, PERMUTATION_PRIME))
}

function getParityBits(hash, numberFunctions){
	const parityBits = new Array(NUMBER_PARITY_BITS);
	
	const numberBitsPerFrequency = Math.round(Math.log2(COMPLEX_ENCODINGS_REAL.length)) //Typically 4
	const groupSize = numberFunctions * numberBitsPerFrequency //Typically 12
	const binaryHashLength = hash.length * BITS_IN_ONE_HEX_CHAR //Typically 128
	const bitsInRemainder = binaryHashLength % groupSize		//Typically 8

	let parityBitsTemp = 0
	for (let g = 0; g < binaryHashLength - bitsInRemainder; g += groupSize){
		//XOR the bit of every group
		let bits = extractBits(hash, g, groupSize)
		parityBitsTemp ^= bits
	}

	if(bitsInRemainder != 0){
		//XOR the remaining bits in pairs : left half is XORed with right half
		const halfBitsInRemainder = bitsInRemainder / 2
		parityBitsTemp <<= halfBitsInRemainder
		parityBitsTemp ^= extractBits(hash, binaryHashLength - bitsInRemainder, halfBitsInRemainder) 
		parityBitsTemp ^= extractBits(hash, binaryHashLength - halfBitsInRemainder, halfBitsInRemainder)
	}

	for (let i = 0; i < parityBits.length; i++){
		//Convert into array, preserving the order
		parityBits[i] = (parityBitsTemp >> parityBits.length - 1 - i) & 1
	}

	return parityBits
}

function extractBits(hash, index, length){
	//Extracts a bitstring of given length from the hash (given as hex string), starting from the given index 
	const charIndex = (index / BITS_IN_ONE_HEX_CHAR) >> 0 
	const charLength = (length / BITS_IN_ONE_HEX_CHAR) >> 0

	return parseInt(hash.substring(charIndex, charIndex + charLength), 16)
}

//--------------------------------------------------SYMMETRY-------------------------------------------//

function getPreImageCoordinates(x, y, sideLength, symmetryMode) {
	//Get the coordinate of the pre-image of a pixel located at coordinates (x,y) under the given symmetry mode.
	// That is, if (x', y') is the reflection of (x,y) under the given symmetry mode, then calling
	// this method on (x', y') returns (x,y). Calling it on (x,y) will also return (x,y), because
	// under these assumptions the pixel at (x,y) is not in the "reflection" part of the image.

	if ((symmetryMode >= 23 || symmetryMode < 0) && symmetryMode != 99) {
		console.log(symmetryMode)
		throw new Error("Invalid symmetry mode : should be between 0 and 22 (included)");
	}

	const halfSide = sideLength / 2;
	const xGreaterThanHalf = x > halfSide;
	const yGreaterThanHalf = y > halfSide;
	const sumGreaterThanSide = x + y > sideLength;
	const xGreaterThanY = x > y;
	const oppositeX = sideLength - 1 - x;
	const oppositeY = sideLength - 1 - y;

	let isInReflection;
	let reflectionX;
	let reflectionY;

	//It may be possible to express those cases in 1 function, but this was already painful enough
	//to debug
	switch (symmetryMode) {
		case 0://HORIZONTAL_LEFT
			isInReflection = xGreaterThanHalf;
			reflectionX = oppositeX;
			reflectionY = y;
			break;
		case 1://HORIZONTAL_RIGHT
			isInReflection = !xGreaterThanHalf;
			reflectionX = oppositeX;
			reflectionY = y;
			break;
		case 2://VERTICAL_UP
			isInReflection = yGreaterThanHalf;
			reflectionX = x;
			reflectionY = oppositeY;
			break;
		case 3://VERTICAL_DOWN
			isInReflection = !yGreaterThanHalf;
			reflectionX = x;
			reflectionY = oppositeY;
			break;
		case 4://DIAGONAL_LEFT (/)
			isInReflection = sumGreaterThanSide;
			reflectionX = oppositeY;
			reflectionY = oppositeX;
			break;
		case 5://DIAGONAL_RIGHT (/)
			isInReflection = !sumGreaterThanSide;
			reflectionX = oppositeY;
			reflectionY = oppositeX;
			break;
		case 6://ANTIDIAG_LEFT (\)
			isInReflection = xGreaterThanY;
			reflectionX = y;
			reflectionY = x;
			break;
		case 7://ANTIDIAG_RIGHT (\)
			isInReflection = !xGreaterThanY;
			reflectionX = y;
			reflectionY = x;
			break;
		case 8://+_CROSS_TOPLEFT
			isInReflection = xGreaterThanHalf || yGreaterThanHalf;
			reflectionX = (xGreaterThanHalf ? oppositeX : x);
			reflectionY = (yGreaterThanHalf ? oppositeY : y);
			break;
		case 9://+_CROSS_TOPRIGHT
			isInReflection = !xGreaterThanHalf || yGreaterThanHalf;
			reflectionX = (!xGreaterThanHalf ? oppositeX : x);
			reflectionY = (yGreaterThanHalf ? oppositeY : y);
			break;
		case 10://+_CROSS_BOTLEFT
			isInReflection = xGreaterThanHalf || !yGreaterThanHalf;
			reflectionX = (xGreaterThanHalf ? oppositeX : x);
			reflectionY = (!yGreaterThanHalf ? oppositeY : y);
			break;
		case 11://+_CROSS_BOTRIGHT
			isInReflection = !xGreaterThanHalf || !yGreaterThanHalf;
			reflectionX = (!xGreaterThanHalf ? oppositeX : x);
			reflectionY = (!yGreaterThanHalf ? oppositeY : y);
			break;
		case 12://X_CROSS_LEFT
			isInReflection = sumGreaterThanSide || xGreaterThanY;
			if(sumGreaterThanSide && xGreaterThanY){
				reflectionX = oppositeX;
				reflectionY = oppositeY;
			}else{
				reflectionX = (sumGreaterThanSide ? oppositeY : y);
				reflectionY = (sumGreaterThanSide ? oppositeX : x);
			}
			break;
		case 13://X_CROSS_TOP
			isInReflection = sumGreaterThanSide || !xGreaterThanY;
			if (!xGreaterThanY && x + y <= sideLength) {
				reflectionX = y;
				reflectionY = x;
			} else {
				reflectionX = sideLength - 1 - (Math.min(x, y));
				reflectionY = sideLength - 1 - (Math.max(x, y));
			}
			break;
		case 14://X_CROSS_RIGHT
			isInReflection = !sumGreaterThanSide || !xGreaterThanY;
			if (!xGreaterThanY && sumGreaterThanSide) {
				reflectionX = y;
				reflectionY = x;
			} else {
				reflectionX = sideLength - 1 - (Math.min(x, y));
				reflectionY = sideLength - 1 - (Math.max(x, y));
			}
			break;
		case 15://X_CROSS_BOT
			isInReflection = !sumGreaterThanSide || xGreaterThanY;
			reflectionX = sumGreaterThanSide ? y : sideLength - 1 - Math.max(x, y);
			reflectionY = sumGreaterThanSide ? x : sideLength - 1 - Math.min(x, y);
			break;
		case 16://COLUMNS_LEFTEST
			isInReflection = x > sideLength / 4;
			if(x > 3 * sideLength / 4){
				reflectionX = oppositeX
			}else{
				reflectionX = !xGreaterThanHalf ? halfSide - x : x - halfSide;
			}
			reflectionY = y;
			break;
		case 17://COLUMNS_LEFT
			isInReflection = x < sideLength / 4 || xGreaterThanHalf;
			if(!xGreaterThanHalf){
				reflectionX = halfSide - x;
			}else{
				reflectionX = x <= 3 * sideLength / 4 ? oppositeX : x - halfSide;
			}
			reflectionY = y;
			break;
		case 18://COLUMNS_RIGHT
			isInReflection = !xGreaterThanHalf || x > 3 * sideLength / 4;
			if (x <= sideLength / 4) reflectionX = x + halfSide;
			else if (!xGreaterThanHalf) reflectionX = oppositeX;
			else reflectionX = 3 * halfSide - x;
			reflectionY = y;
			break;
		case 19://COLUMNS_RIGHTEST
			isInReflection = x < 3 * sideLength / 4;
			if (x <= sideLength / 4) reflectionX = oppositeX;
			else if (!xGreaterThanHalf) reflectionX = halfSide - 1 + x;
			else reflectionX = 3 * sideLength / 2 - x;
			reflectionY = y;
			break;
		case 20://ROWS_TOPPEST
			isInReflection = y < sideLength / 4 || yGreaterThanHalf;
			reflectionX = x;
			if(!yGreaterThanHalf){
				reflectionY = halfSide - y;
			}else{
				reflectionY = y <= 3 * sideLength / 4 ? oppositeY : y - halfSide;
			}
			break;
		case 21://ROW_TOP
			isInReflection = y < sideLength / 4 || yGreaterThanHalf;
			reflectionX = x;
			if(!yGreaterThanHalf){
				reflectionY = halfSide - y;
			}else{
				reflectionY = y <= 3 * sideLength / 4 ? oppositeY : y - halfSide;
			}
			break;
		case 22://ROW_BOTTOM
			isInReflection = !yGreaterThanHalf || y > 3 * sideLength / 4;
			reflectionX = x;
			if (y <= sideLength / 4) reflectionY = y + halfSide;
			else if (!yGreaterThanHalf) reflectionY = oppositeY;
			else reflectionY = 3 * halfSide - y;
			break;
		case 99://NO SYMMETRY (for debugging)
			isInReflection = false
			break;
		default:
			//Will never happen
			return [0,0];
	}
	if (!isInReflection) return [x, y];
	else return [reflectionX, reflectionY];
}


//----------------------------------POST-PROCESSING----------------------------------//
function smoothen(pixels){
	//Smoothen an image by making pixels that are at the border between 2 color zones their average
	let current, right, bottom, newRed, newGreen, newBlue;
	for(let i = 0; i < pixels.length - 1; i++){
		for(let j = 0; j < pixels[0].length - 1; j++){
			if(pixels[i][j] === pixels[i+1][j] && pixels[i][j] === pixels[i][j+1]) continue
			//Parse colors
			current = parseInt(pixels[i][j].substring(1, 7), 16);
			right = parseInt(pixels[i+1][j].substring(1, 7), 16);
			bottom = parseInt(pixels[i][j+1].substring(1, 7), 16);
			//Average colors
			newRed = Math.round((current >>> 16 & 0xff) / 3 + (right >>> 16 & 0xff) / 3 + (bottom >>> 16 & 0xff) / 3).toString(16)
			newGreen = Math.round((current >>> 8 & 0xff) / 3 + (right >>> 8 & 0xff) / 3 + (bottom >>> 8 & 0xff) / 3).toString(16)
			newBlue = Math.round((current & 0xff) / 3 + (right & 0xff) / 3 + (bottom & 0xff) / 3).toString(16)
			pixels[i][j] = "#" + newRed.padStart(2, '0') + newGreen.padStart(2, '0') + newBlue.padStart(2, '0') + "ff"
		}
	}
}

