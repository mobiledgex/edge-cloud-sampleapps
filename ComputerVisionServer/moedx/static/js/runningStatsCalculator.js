class RunningStatsCalculator {
  constructor(name) {
    this._name = name;
    this.reset();
  }

  reset() {
    this._count = 0
    this._mean = 0
    this._dSquared = 0
    this._min = Number.MAX_VALUE
    this._max = 0
  }

  update(newValue) {
    if (newValue < this._min) {
      this._min = newValue;
    }

    if (newValue > this._max) {
      this._max = newValue;
    }

    this._count++

    const meanDifferential = (newValue - this._mean) / this._count

    const newMean = this._mean + meanDifferential

    const dSquaredIncrement =	(newValue - newMean) * (newValue - this._mean)

    const newDSquared = this._dSquared + dSquaredIncrement

    this._mean = newMean

    this._dSquared = newDSquared
  }

  get statsText() {
    return "min/avg/max/std="+this.min.toFixed(0)+"/"+this.mean.toFixed(0)+"/"+this.max.toFixed(0)+"/"+this.sampleStdev.toFixed(0);
  }

  get mean() {
    this.validate()
    return this._mean
  }

  get dSquared() {
    this.validate()
    return this._dSquared
  }

  get populationVariance() {
    return this.dSquared / this._count
  }

  get populationStdev() {
    return Math.sqrt(this.populationVariance)
  }

  get sampleVariance() {
    return this._count > 1 ? this.dSquared / (this._count - 1) : 0
  }

  get sampleStdev() {
    return Math.sqrt(this.sampleVariance)
  }

  get min() {
    return this._min;
  }

  get max() {
    return this._max;
  }

  validate() {
    if (this._count == 0) {
      throw new StatsError('Mean is undefined')
    }
  }
}

class StatsError extends Error {
  constructor(...params) {
    super(...params)

    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, StatsError)
    }
  }
}
