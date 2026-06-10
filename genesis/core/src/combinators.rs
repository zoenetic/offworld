use crate::{Field, Vec3};

pub struct Frequency<F> {
    pub input: F,
    pub factor: f64,
}

impl<F: Field> Field for Frequency<F> {
    fn sample(&self, p: Vec3) -> f64 {
        self.input.sample(Vec3::new(
            p.x * self.factor,
            p.y * self.factor,
            p.z * self.factor,
        ))
    }
}

pub struct Scale<F> {
    pub input: F,
    pub factor: f64,
}

impl<F: Field> Field for Scale<F> {
    fn sample(&self, p: Vec3) -> f64 {
        self.input.sample(p) * self.factor
    }
}

pub struct Add<A, B> {
    pub a: A,
    pub b: B,
}

impl<A: Field, B: Field> Field for Add<A, B> {
    fn sample(&self, p: Vec3) -> f64 {
        self.a.sample(p) + self.b.sample(p)
    }
}

pub struct Clamp<F> {
    pub input: F,
    pub lo: f64,
    pub hi: f64,
}

impl<F: Field> Field for Clamp<F> {
    fn sample(&self, p: Vec3) -> f64 {
        self.input.sample(p).clamp(self.lo, self.hi)
    }
}

pub struct Translate<F> {
    pub input: F,
    pub offset: Vec3,
}

impl<F: Field> Field for Translate<F> {
    fn sample(&self, p: Vec3) -> f64 {
        self.input.sample(p + self.offset)
    }
}

pub trait FieldExt: Field + Sized {
    fn frequency(self, factor: f64) -> Frequency<Self> {
        Frequency { input: self, factor }
    }
    fn scale(self, factor: f64) -> Scale<Self> {
        Scale { input: self, factor }
    }
    fn add<B: Field>(self, other: B) -> Add<Self, B> {
        Add { a: self, b: other }
    }
    fn clamp(self, lo: f64, hi: f64) -> Clamp<Self> {
        Clamp { input: self, lo, hi }
    }
    fn translate(self, offset: Vec3) -> Translate<Self> { Translate { input: self, offset }}
}

impl<F: Field> FieldExt for F {}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::{Constant, ValueNoise};

    const ORIGIN: Vec3 = Vec3::new(0.0, 0.0, 0.0);

    #[test]
    fn scale_multiplies_output() {
        assert_eq!(Constant(2.0).scale(3.0).sample(ORIGIN), 6.0);
    }

    #[test]
    fn add_sums() {
        assert_eq!(Constant(2.0).add(Constant(5.0)).sample(ORIGIN), 7.0);
    }

    #[test]
    fn clamp_bounds() {
        assert_eq!(Constant(9.0).clamp(0.0, 1.0).sample(ORIGIN), 1.0);
    }

    #[test]
    fn frequency_scales_the_domain() {
        let scaled = ValueNoise::new(1).frequency(0.5);
        let raw = ValueNoise::new(1);
        assert_eq!(
            scaled.sample(Vec3::new(10.0, 0.0, 0.0)),
            raw.sample(Vec3::new(5.0, 0.0, 0.0)),
        );
    }

    #[test]
    fn translate_shifts_the_domain() {
        let raw = ValueNoise::new(1);
        let shifted = ValueNoise::new(1).translate(Vec3::new(5.0, 0.0, 0.0));
        assert_eq!(
            shifted.sample(Vec3::new(10.0, 0.0, 0.0)),
            raw.sample(Vec3::new(15.0, 0.0, 0.0)),
        )
    }
}