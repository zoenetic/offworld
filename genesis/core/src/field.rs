use crate::Vec3;
pub trait Field {
    fn sample(&self, p: Vec3) -> f64;
}

pub struct Constant(pub f64);

impl Field for Constant {
    fn sample(&self, _p: Vec3) -> f64 {
        self.0
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn constant_samples_everywhere() {
        let f = Constant(42.0);
        assert_eq!(f.sample(Vec3::new(0.0, 0.0, 0.0)), 42.0);
        assert_eq!(f.sample(Vec3::new(1e6, -40.0, 7.0)), 42.0);
    }
}