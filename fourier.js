function mapToComplex(fourBits){
	reals = [-0.5, -0.5, 1.0, 1.0, 0.5, 0.5, -1.0, -1.0, 1.0, 1.0, -0.5, -0.5, -1.0, -1.0, 0.5, 0.5];
	ims = [0.5, -1.0, -0.5, 1.0, -1.0, 0.5, 1.0, -0.5, 0.5, -1.0, -0.5, 1.0, -1.0, 0.5, 1.0, 0.5];
	return new ComplexNumber(reals[fourBits], ims[fourBits]);
}

function ComplexNumber(real, imaginary){
	this.re = real
	this.im = imaginary
}
ComplexNumber.prototype.mult = function(c) {
	if (typeof c === 'object') { 
		// complex multiplication
		let rePart = this.re * c.re - this.im * c.im;
		let imPart = this.re * c.im + this.im * c.re;
		return new ComplexNumber(rePart, imPart);
	} else { 
		// multiplication with integer
		return new ComplexNumber(c * this.re, c * this.im);
	}
}
ComplexNumber.prototype.cExp = function(){
	let c1 = new ComplexNumber(Math.exp(this.re), 0)
	let c2 = new ComplexNumber(Math.cos(this.im), Math.sin(this.im))
	return c1.mult(c2)
}

ComplexNumber.prototype.add = function(c){
	return new ComplexNumber(this.re + c.re, this.im + c.im)
}

ComplexNumber.prototype.mag = function(){
	return this.re * this.re + this.im * this.im
}

ComplexNumber.prototype.norm = function() {
	return Math.sqrt(this.mag())
}

ComplexNumber.prototype.sub = function(c){
	return this.add(new ComplexNumber(-c.re, -c.im))
}

ComplexNumber.prototype.div = function(c){
	let n = c.mag()
	let real = (this.re * c.re + this.im * c.im) / n
	let imaginary = (this.im * c.re - this.re * c.im) / n
	return new ComplexNumber(real, imaginary)
}

function inverseFFT2D(x){
	let fft = new FFT(256)
	let out = new Array()
	let intermediate = new Array(x.length)
	for(let i = 0; i < x.length; i++){
		intermediate[i] = new Array(x.length)
		out[i] = new Array(x.length)
	}
	for(let i = 0; i < x.length; i ++){
		//newColRec = recursiveInverseFFT(x[i])
		interLeavedCol = mapToInterleavedFormat(x[i])
		newCol = new Array(interLeavedCol.length)
		fft.inverseTransform(newCol, interLeavedCol)
		putCol(intermediate, i, mapToPackedComplex(newCol))
		if(i == 255){
			console.log(mapToPackedComplex(newCol))
			//console.log(newColRec)
		}
	}
	for(let i = 0; i < x.length; i ++){
		//newRowRec = recursiveInverseFFT(getRow(intermediate, i))
		interLeavedRow = mapToInterleavedFormat(getRow(intermediate, i))
		newRow = new Array(interLeavedRow.length)
		fft.inverseTransform(newRow, interLeavedRow)
		putRow(out, i, mapToPackedComplex(newRow))
	}
	return out
	
	
}

function mapToInterleavedFormat(complexArr){
	const newArr = new Array(complexArr.length * 2);
	for(let i = 0; i < complexArr.length; i++){
		newArr[2 * i] = complexArr[i].re;
		newArr[2 * i + 1] = complexArr[i].im
	}
	return newArr
}

function mapToPackedComplex(interleavedArr){
	const newArrPacked = new Array(Math.round(interleavedArr.length / 2));
	for(let i = 0; i < newArrPacked.length; i++){
		newArrPacked[i] = new ComplexNumber(interleavedArr[2 * i], interleavedArr[2 * i + 1])
	}
	return newArrPacked
}
function putRow(x, n, newValues){
	for (let i = 0; i < x.length; i++){
		x[i][n] = new ComplexNumber(newValues[i].re, newValues[i].im)
	}
}
function putCol(x, n, newValues){
	for (let i = 0; i < x.length; i++){
		x[n][i] = new ComplexNumber(newValues[i].re, newValues[i].im)
	}
}

function getRow(x, n){
	let out = new Array()
	for(let i = 0; i < x.length; i++){
		out.push(new ComplexNumber(x[i][n].re, x[i][n].im))
	}
	return out
}


/**
* Computes the inverse Fourier Transform of an array using the Cooley-Tukey algorithm
* @see https://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm
*/
function recursiveInverseFFT (x){
	let z1 = new ComplexNumber(0,0)
	let sum = new ComplexNumber(0,0)
	let diff = new ComplexNumber(0,0)
	let tmp = new ComplexNumber(0,0)
	let cTwo = new ComplexNumber(0,0)
	let n = x.length;
	let m = Math.round(n/2);
	
	let result = new Array(n);
	let sums = new Array(m)
	let diffs = new Array(m)
	let even = new Array(m)
	let odd = new Array(m)
	
	cTwo = new ComplexNumber(2,0);
	if(n == 1){
		result[0] = x[0];
	} else {
		z1 = new ComplexNumber(0.0, 2*(Math.PI)/n);
		tmp = z1.cExp();
		z1 = new ComplexNumber(1.0, 0.0);
		for(let i = 0; i < m; i++){
			sum = x[i].add(x[i+m]);
			sums[i] = sum.div(cTwo);
			
			sum = x[i].sub(x[i+m]);
			diff = sum.mult(z1);
			diffs[i] = diff.div(cTwo);
			z1 = z1.mult(tmp);
		}
		even = recursiveInverseFFT(sums);
		odd = recursiveInverseFFT(diffs);
		for(let i = 0;i < m; i++){
			result[i*2] = new ComplexNumber(even[i].re, even[i].im);
			result[i*2 + 1] = new ComplexNumber(odd[i].re, odd[i].im);
		}
	}
	return result;
}